package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Collections

class FirestoreSyncManager(
    private val repository: AppRepository,
    private val pendingDao: PendingActionDao? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private val activeListeners = Collections.synchronizedList(mutableListOf<ListenerRegistration>())

    companion object {
        private val _isQuotaExhausted = MutableStateFlow(false)
        val isQuotaExhausted: StateFlow<Boolean> = _isQuotaExhausted.asStateFlow()

        fun triggerQuotaExhausted() {
            _isQuotaExhausted.value = true
        }

        fun resetQuotaExhausted() {
            _isQuotaExhausted.value = false
        }
    }

    private fun handleCriticalFirestoreError(e: FirebaseFirestoreException?) {
        if (e == null) return
        if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
            triggerQuotaExhausted()
        }
        if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ||
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) {
            Log.w("FirestoreSyncManager", "Quota Firestore épuisé (${e.code}). Arrêt de tous les listeners et passage en mode dégradé.")
            stopSync()
            try {
                com.example.di.Graph.adminOrderRepository.notifyQuotaOrNetworkExhausted()
            } catch (_: Throwable) {}
        }
    }

    // Dual-direction synchronization loop prevention tracker
    private val incomingSyncTracker = ConcurrentHashMap.newKeySet<String>()

    fun stopSync() {
        synchronized(activeListeners) {
            for (listener in activeListeners) {
                try {
                    listener.remove()
                } catch (_: Throwable) {}
            }
            activeListeners.clear()
        }
    }

    fun retrySync() {
        stopSync()
        startSync()
    }

    fun startSync() {
        stopSync()
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e("FirestoreSyncManager", "Firebase not initialized", e)
            return
        }
        val db = firestore ?: return

        // --- OPTIMISATION DU QUOTA FIRESTORE (ÉTAPE 2/6) ---
        // Éradication totale des addSnapshotListener permanents sur les collections massives (clients, catalogue, historique, paramètres, etc.).
        // L'Admin ne dispose de temps réel QUE pour les commandes nécessitant une action (AdminOrderRepository.syncActiveOrdersRealtime).
        // Tout le reste passe en chargement ponctuel et sur Pull-to-Refresh via get().await().
        scope.launch(Dispatchers.IO) {
            try {
                repository.fetchSettings()
                repository.fetchClients()
                repository.fetchCatalog()
            } catch (e: Throwable) {
                Log.e("FirestoreSyncManager", "Erreur lors du chargement initial one-shot get().await(): ${e.message}", e)
            }
        }

        val enableLegacyRealtimeListeners = false
        if (enableLegacyRealtimeListeners) {
        // --- 1. REAL-TIME LISTENERS FROM FIRESTORE ---

        // USERS & UTILISATEURS DUAL LISTENER
        val usersCollections = listOf("users", "utilisateurs")
        for (colName in usersCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteUser(doc.id)
                            } else {
                                val docNom = doc.getString("nom")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("nomAdmin")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("nomUser")?.takeIf { it.isNotEmpty() }
                                    ?: ""
                                val docPrenom = doc.getString("prenom")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("prenomAdmin")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("prenomUser")?.takeIf { it.isNotEmpty() }
                                    ?: ""
                                val docEmail = doc.getString("email")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("mailAdmin")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("mailUser")?.takeIf { it.isNotEmpty() }
                                    ?: ""
                                val docMotDePasse = doc.getString("motDePasse")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("motDePasseAdmin")?.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("motDePasseUser")?.takeIf { it.isNotEmpty() }
                                    ?: ""

                                val user = UserEntity(
                                    id = doc.id,
                                    nom = docNom,
                                    prenom = docPrenom,
                                    email = docEmail,
                                    motDePasse = docMotDePasse,
                                    role = doc.getString("role") ?: "CLIENT",
                                    telephone = doc.getString("telephone") ?: doc.getString("numeroUser") ?: "",
                                    coordonnees = doc.getString("coordonnees") ?: doc.getString("adresseUser") ?: "",
                                    token = doc.getString("token") ?: "",
                                    fidelityPointsKg = (doc.get("fidelityPointsKg") as? Number)?.toFloat()
                                        ?: (doc.get("cumulKiloFideliterUser") as? Number)?.toFloat()
                                        ?: 0f,
                                    myReferralCode = doc.getString("myReferralCode") ?: "",
                                    referredBy = doc.getString("referredBy") ?: "",
                                    isSuspended = doc.getBoolean("isSuspended") ?: doc.getBoolean("estSuspendu") ?: false,
                                    estSuspendu = doc.getBoolean("isSuspended") ?: doc.getBoolean("estSuspendu") ?: false,
                                    suspensionReason = doc.getString("suspensionReason") ?: doc.getString("raisonSuspension") ?: "",
                                    raisonSuspension = doc.getString("suspensionReason") ?: doc.getString("raisonSuspension") ?: "",
                                    suspensionUntil = doc.getLong("suspensionUntil"),

                                    isBanned = doc.getBoolean("isBanned") ?: doc.getBoolean("estBanni") ?: doc.getBoolean("banni") ?: false,
                                    estBanni = doc.getBoolean("isBanned") ?: doc.getBoolean("estBanni") ?: doc.getBoolean("banni") ?: false,
                                    banReason = doc.getString("banReason") ?: doc.getString("raisonBannissement") ?: doc.getString("motifBannissement") ?: "",
                                    raisonBannissement = doc.getString("banReason") ?: doc.getString("raisonBannissement") ?: doc.getString("motifBannissement") ?: "",
                                    isBanPermanent = doc.getBoolean("isBanPermanent") ?: doc.getBoolean("bannissementPermanent") ?: false,
                                    bannissementPermanent = doc.getBoolean("isBanPermanent") ?: doc.getBoolean("bannissementPermanent") ?: false,
                                    banUntil = doc.getLong("banUntil") ?: doc.getLong("dateFinBannissement"),
                                    dateFinBannissement = doc.getLong("banUntil") ?: doc.getLong("dateFinBannissement"),

                                    canUsePromo = doc.getBoolean("canUsePromo") ?: true,
                                    canUsePromoUntil = doc.getLong("canUsePromoUntil"),
                                    canOrder = doc.getBoolean("canOrder") ?: true,
                                    isArchived = doc.getBoolean("isArchived") ?: false,
                                    permissions = doc.getString("permissions") ?: "ALL",
                                    isLoyaltyEnabled = doc.getBoolean("isLoyaltyEnabled") ?: true,
                                    loyaltyValidUntil = doc.getLong("loyaltyValidUntil"),
                                    accumulatedFreeKgs = (doc.get("accumulatedFreeKgs") as? Number)?.toDouble() ?: 0.0,
                                    currentKgTowardsReward = (doc.get("currentKgTowardsReward") as? Number)?.toDouble() ?: 0.0,
                                    lifetimeKgWashed = (doc.get("lifetimeKgWashed") as? Number)?.toDouble() ?: 0.0,
                                    customAutoBanEnabled = doc.getBoolean("customAutoBanEnabled") ?: false,
                                    customAutoBanThreshold = doc.getLong("customAutoBanThreshold")?.toInt() ?: 0,
                                    customAutoBanDurationHours = doc.getLong("customAutoBanDurationHours")?.toInt() ?: 0,
                                    canSelectPieces = doc.getBoolean("canSelectPieces") ?: true,
                                    canSelectCategories = doc.getBoolean("canSelectCategories") ?: true,
                                    canEstimateKg = doc.getBoolean("canEstimateKg") ?: true,
                                    canUseExpress = doc.getBoolean("canUseExpress") ?: true,
                                    canUsePremium = doc.getBoolean("canUsePremium") ?: true,

                                    // New Schema elements
                                    prenomAdmin = doc.getString("prenomAdmin") ?: docPrenom,
                                    nomAdmin = doc.getString("nomAdmin") ?: docNom,
                                    mailAdmin = doc.getString("mailAdmin") ?: docEmail,
                                    motDePasseAdmin = doc.getString("motDePasseAdmin") ?: docMotDePasse,
                                    roleAdminSuper = doc.getBoolean("roleAdminSuper") ?: (doc.getString("role") == "SUPER_ADMIN"),

                                    permVoirCommandes = doc.getBoolean("permVoirCommandes") ?: true,
                                    permGererCommandes = doc.getBoolean("permGererCommandes") ?: true,
                                    permModifierHeureRamassage = doc.getBoolean("permModifierHeureRamassage") ?: true,
                                    permVoirClients = doc.getBoolean("permVoirClients") ?: true,
                                    permGererClients = doc.getBoolean("permGererClients") ?: true,
                                    permSuspendreClients = doc.getBoolean("permSuspendreClients") ?: true,
                                    permVoirLog = doc.getBoolean("permVoirLog") ?: true,
                                    permGererPromosClient = doc.getBoolean("permGererPromosClient") ?: true,
                                    permVoirCorbeille = doc.getBoolean("permVoirCorbeille") ?: true,
                                    permRestaurerCompte = doc.getBoolean("permRestaurerCompte") ?: true,
                                    permSupprimerDefinitivement = doc.getBoolean("permSupprimerDefinitivement") ?: true,
                                    permVoirStocks = doc.getBoolean("permVoirStocks") ?: true,
                                    permGererStocks = doc.getBoolean("permGererStocks") ?: true,
                                    permGererStocksVisibilite = doc.getBoolean("permGererStocksVisibilite") ?: true,
                                    permVoirCatalogue = doc.getBoolean("permVoirCatalogue") ?: true,
                                    permGererCatalogue = doc.getBoolean("permGererCatalogue") ?: true,
                                    permGererPromos = doc.getBoolean("permGererPromos") ?: true,
                                    permGererPromosAvancees = doc.getBoolean("permGererPromosAvancees") ?: true,
                                    permGererGommage = doc.getBoolean("permGererGommage") ?: true,
                                    permVoirParametres = doc.getBoolean("permVoirParametres") ?: true,
                                    permGererParametres = doc.getBoolean("permGererParametres") ?: true,
                                    permVoirAntiFraudPersonnel = doc.getBoolean("permVoirAntiFraudPersonnel") ?: true,
                                    permGererAntiFraudPersonnel = doc.getBoolean("permGererAntiFraudPersonnel") ?: true,
                                    permVoirAntiFraudGlobal = doc.getBoolean("permVoirAntiFraudGlobal") ?: true,
                                    permGererAntiFraudGlobal = doc.getBoolean("permGererAntiFraudGlobal") ?: true,

                                    suppressionDefinitiveAdmin = doc.getBoolean("suppressionDefinitiveAdmin") ?: false,
                                    restaurerAdmin = doc.getBoolean("restaurerAdmin") ?: false,

                                    prenomUser = doc.getString("prenomUser") ?: docPrenom,
                                    nomUser = doc.getString("nomUser") ?: docNom,
                                    numeroUser = doc.getString("numeroUser") ?: doc.getString("telephone") ?: "",
                                    mailUser = doc.getString("mailUser") ?: docEmail,
                                    adresseUser = doc.getString("adresseUser") ?: doc.getString("coordonnees") ?: "",
                                    motDePasseUser = doc.getString("motDePasseUser") ?: docMotDePasse,
                                    cumulKiloFideliterUser = (doc.get("cumulKiloFideliterUser") as? Number)?.toDouble()
                                        ?: (doc.get("fidelityPointsKg") as? Number)?.toDouble()
                                        ?: 0.0,

                                    autoriseCreatOrder = doc.getBoolean("autoriseCreatOrder") ?: true,
                                    autoriseSaisiNombrePiece = doc.getBoolean("autoriseSaisiNombrePiece") ?: true,
                                    autoriseSelectionCategory = doc.getBoolean("autoriseSelectionCategory") ?: true,
                                    autoriseSelectionCatalogue = doc.getBoolean("autoriseSelectionCatalogue") ?: true,
                                    autoriseSelectionArticlesSpecial = doc.getBoolean("autoriseSelectionArticlesSpecial") ?: true,
                                    autoriseSelectionArticlesGommage = doc.getBoolean("autoriseSelectionArticlesGommage") ?: true,
                                    autoriseServiceLavage = doc.getBoolean("autoriseServiceLavage") ?: true,
                                    autoriseLavageSimple = doc.getBoolean("autoriseLavageSimple") ?: true,
                                    autoriseLavageSechagePliage = doc.getBoolean("autoriseLavageSechagePliage") ?: true,
                                    autoriseLavageSechagePliageRepassage = doc.getBoolean("autoriseLavageSechagePliageRepassage") ?: true,
                                    autoriseRamassageLinges = doc.getBoolean("autoriseRamassageLinges") ?: true,
                                    autoriseFixeeDateHeureRamassage = doc.getBoolean("autoriseFixeeDateHeureRamassage") ?: true,
                                    autoriseDateHeureLivraison = doc.getBoolean("autoriseDateHeureLivraison") ?: true,
                                    autoriseInstructionSpecial = doc.getBoolean("autoriseInstructionSpecial") ?: true,
                                    autoriseServiceExpress = doc.getBoolean("autoriseServiceExpress") ?: true,
                                    autoriseGarantiVIP = doc.getBoolean("autoriseGarantiVIP") ?: true,
                                    autoriseUtiliserCodePromos = doc.getBoolean("autoriseUtiliserCodePromos") ?: true,
                                    autoriseVoirEstimationPrixFinal = doc.getBoolean("autoriseVoirEstimationPrixFinal") ?: true,

                                    utiliseAntiFraudeGlobale = doc.getBoolean("utiliseAntiFraudeGlobale") ?: true,
                                    blocagePersistant = doc.getBoolean("blocagePersistant") ?: false,
                                    blocageLimitee = doc.getBoolean("blocageLimitee") ?: false,
                                    dureeBlocage = (doc.get("dureeBlocage") as? Number)?.toDouble() ?: 0.0
                                )

                                val key = "User_${user.id}"
                                incomingSyncTracker.add(key)
                                repository.insertUser(user)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // ORDERS, ORDRES & COMMANDES MULTI-COLLECTION LISTENER
        val ordersCollections = listOf("orders", "ordres", "commandes")
        for (colName in ordersCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteOrder(doc.id)
                            } else {
                                val order = OrderMapper.fromDocument(doc)

                                val key = "Order_${order.id}"
                                incomingSyncTracker.add(key)
                                repository.insertOrder(order)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // PROMOTIONS & PROMOS DUAL LISTENER
        val promoCollections = listOf("promotions", "promos")
        for (colName in promoCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deletePromo(doc.id)
                            } else {
                                val promo = PromoEntity(
                                    id = doc.id,
                                    code = doc.getString("code") ?: "",
                                    description = doc.getString("description") ?: "",
                                    discountPercent = (doc.get("discountPercent") as? Number)?.toFloat() ?: 0f,
                                    freeKg = (doc.get("freeKg") as? Number)?.toFloat() ?: 0f,
                                    minOrderValue = (doc.get("minOrderValue") as? Number)?.toFloat() ?: 0f,
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    validUntil = doc.getLong("validUntil"),
                                    isPermanent = doc.getBoolean("isPermanent") ?: true,
                                    targetItems = doc.getString("targetItems") ?: "ALL",
                                    discountType = doc.getString("discountType") ?: "PERCENTAGE",
                                    isArchived = doc.getBoolean("isArchived") ?: false
                                )
                                val key = "Promo_${promo.id}"
                                incomingSyncTracker.add(key)
                                repository.insertPromo(promo)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // ANNOUNCEMENTS DUAL LISTENER
        val annCollections = listOf("announcements", "annonces")
        for (colName in annCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteAnnouncement(doc.id)
                            } else {
                                val ann = AnnouncementEntity(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    imageUrl = doc.getString("imageUrl") ?: "",
                                    isVisible = doc.getBoolean("isVisible") ?: true,
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    nomAnnoce = doc.getString("nomAnnoce") ?: "",
                                    descriptionAnnoce = doc.getString("descriptionAnnoce") ?: "",
                                    fichiersMediaAnnoce = doc.getString("fichiersMediaAnnoce") ?: ""
                                )
                                val key = "Ann_${ann.id}"
                                incomingSyncTracker.add(key)
                                repository.insertAnnouncement(ann)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // CATALOG & CATALOGUE DUAL LISTENER
        val catalogCollections = listOf("catalog", "catalogue")
        for (colName in catalogCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteGommageItem(doc.id)
                                repository.deleteSpecialItem(doc.id)
                            } else {
                                val type = doc.getString("type") ?: ""
                                val isArchived = doc.getBoolean("isArchived") ?: false
                                val name = doc.getString("name") ?: ""
                                val price = (doc.get("price") as? Number)?.toFloat() ?: 0f

                                if (type.equals("GOMMAGE", ignoreCase = true) || doc.id.startsWith("gom_", ignoreCase = true)) {
                                    val item = GommageItemEntity(
                                        id = doc.id,
                                        name = name,
                                        price = price,
                                        isArchived = isArchived
                                    )
                                    val key = "Gommage_${item.id}"
                                    incomingSyncTracker.add(key)
                                    repository.insertGommageItem(item)
                                } else {
                                    val item = SpecialItemEntity(
                                        id = doc.id,
                                        name = name,
                                        price = price,
                                        isArchived = isArchived
                                    )
                                    val key = "Special_${item.id}"
                                    incomingSyncTracker.add(key)
                                    repository.insertSpecialItem(item)
                                }
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // STOCKS & STOCK DUAL LISTENER
        val stockCollections = listOf("stocks", "stock")
        for (colName in stockCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteConsumable(doc.id)
                            } else {
                                val docName = doc.getString("name")?.takeIf { it.isNotEmpty() } ?: doc.getString("nomProduit") ?: ""
                                val docQuantity = (doc.get("quantity") as? Number)?.toInt() 
                                    ?: (doc.get("quantiterProduit") as? Number)?.toInt() 
                                    ?: 0
                                val docThreshold = (doc.get("threshold") as? Number)?.toInt() 
                                    ?: (doc.get("produitSeilleAlert") as? Number)?.toInt() 
                                    ?: 0
                                val docIsPublic = doc.getBoolean("isPublic") 
                                    ?: doc.getBoolean("newStocksPublic") 
                                    ?: doc.getBoolean("public") 
                                    ?: false
                                val docIsPrivee = doc.getBoolean("newStocksPrivee") 
                                    ?: doc.getBoolean("privee") 
                                    ?: !docIsPublic

                                val c = ConsumableEntity(
                                    id = doc.id,
                                    name = docName,
                                    quantity = docQuantity,
                                    threshold = docThreshold,
                                    isPublic = docIsPublic,
                                    isArchived = doc.getBoolean("isArchived") ?: false,
                                    nomProduit = doc.getString("nomProduit") ?: docName,
                                    quantiterProduit = (doc.get("quantiterProduit") as? Number)?.toDouble() ?: docQuantity.toDouble(),
                                    produitSeilleAlert = (doc.get("produitSeilleAlert") as? Number)?.toDouble() ?: docThreshold.toDouble(),
                                    newStocksPublic = docIsPublic,
                                    newStocksPrivee = docIsPrivee
                                )
                                val key = "Consumable_${c.id}"
                                incomingSyncTracker.add(key)
                                repository.insertConsumable(c)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // AUTOMATION RULES DUAL LISTENER
        val autoRuleCollections = listOf("automation_rules", "regles")
        for (colName in autoRuleCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteRule(doc.id)
                            } else {
                                val r = RuleEntity(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    triggerCondition = doc.getString("triggerCondition") ?: "",
                                    actionType = doc.getString("actionType") ?: "",
                                    actionValue = doc.getString("actionValue") ?: "",
                                    constraints = doc.getString("constraints") ?: "",
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    isPermanent = doc.getBoolean("isPermanent") ?: true,
                                    validUntil = doc.getLong("validUntil"),
                                    isArchived = doc.getBoolean("isArchived") ?: false
                                )
                                val key = "Rule_${r.id}"
                                incomingSyncTracker.add(key)
                                repository.insertRule(r)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // ANTI FRAUD RULES DUAL LISTENER
        val antiFraudCollections = listOf("anti_fraud_rules", "regles_anti_fraude")
        for (colName in antiFraudCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteAntiFraudRule(doc.id)
                            } else {
                                val r = AntiFraudRuleEntity(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    triggerEvent = doc.getString("triggerEvent") ?: "",
                                    threshold = (doc.get("threshold") as? Number)?.toInt() ?: 0,
                                    durationHours = (doc.get("durationHours") as? Number)?.toInt() ?: 0,
                                    action = doc.getString("action") ?: "BAN",
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    isArchived = doc.getBoolean("isArchived") ?: false,
                                    nomNewAntiFraud = doc.getString("nomNewAntiFraud") ?: "",
                                    evenAntiFraud = doc.getString("evenAntiFraud") ?: "",
                                    seuilAntiFraud = (doc.get("seuilAntiFraud") as? Number)?.toDouble() ?: 0.0,
                                    dureeAntiFraud = (doc.get("dureeAntiFraud") as? Number)?.toDouble() ?: 0.0,
                                    actionAntiFraud = doc.getString("actionAntiFraud") ?: ""
                                )
                                val key = "AntiFraud_${r.id}"
                                incomingSyncTracker.add(key)
                                repository.insertAntiFraudRule(r)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // CATEGORIES LINGE SAVED LISTENER
        db.collection("categories_linge_saved").addSnapshotListener { snapshot, e ->
            if (e != null) {
                if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                    triggerQuotaExhausted()
                }
                handleCriticalFirestoreError(e)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    try {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            repository.deleteCategoryLingeSaved(doc.id)
                        } else {
                            val cat = CategoriesLingeSavedEntity(
                                id = doc.id,
                                newCategorieLinge = doc.getString("newCategorieLinge") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                            val key = "Category_${cat.id}"
                            incomingSyncTracker.add(key)
                            repository.insertCategoryLingeSaved(cat)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // CONFIG & GENERAL SETTINGS DUAL LISTENER
        val configCollections = listOf("config", "general_settings")
        for (colName in configCollections) {
            db.collection(colName).document("main").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                scope.launch {
                    try {
                        val doc = snapshot
                        val baseTransportFee = (doc.get("baseTransportFee") as? Number)?.toFloat()
                            ?: (doc.get("tarifBaseTransort") as? Number)?.toFloat() ?: 1000f
                        val expressMultiplier = (doc.get("expressMultiplier") as? Number)?.toFloat() ?: 1.3f
                        val fidelityThresholdKg = (doc.get("fidelityThresholdKg") as? Number)?.toFloat() ?: 10f
                        val fidelityRewardValue = (doc.get("fidelityRewardValue") as? Number)?.toFloat() ?: 350f
                        val pricePerKm = (doc.get("pricePerKm") as? Number)?.toFloat()
                            ?: (doc.get("prixParKilo") as? Number)?.toFloat() ?: 500f
                        val pressingLat = (doc.get("pressingLat") as? Number)?.toDouble() ?: 12.6392
                        val pressingLng = (doc.get("pressingLng") as? Number)?.toDouble() ?: -8.0029
                        val referralRewardKg = (doc.get("referralRewardKg") as? Number)?.toFloat() ?: 2f
                        val appDownloadLink = doc.getString("appDownloadLink") ?: ""
                        val autoBanEnabled = doc.getBoolean("autoBanEnabled") ?: false
                        val autoBanThreshold = (doc.get("autoBanThreshold") as? Number)?.toInt() ?: 3
                        val autoBanDurationHours = (doc.get("autoBanDurationHours") as? Number)?.toInt() ?: 24
                        val isLoyaltyEnabled = doc.getBoolean("isLoyaltyEnabled") ?: true
                        val loyaltyValidUntil = doc.getLong("loyaltyValidUntil")
                        val isMaintenance = doc.getBoolean("isMaintenance") ?: false
                        val acceptNewOrders = doc.getBoolean("acceptNewOrders") ?: true
                        val isLoyaltyProgramEnabled = doc.getBoolean("isLoyaltyProgramEnabled") ?: isLoyaltyEnabled
                        val loyaltyKgThreshold = (doc.get("loyaltyKgThreshold") as? Number)?.toDouble() ?: 10.0
                        val loyaltyKgReward = (doc.get("loyaltyKgReward") as? Number)?.toDouble() ?: 1.0
                        val loyaltyMaxFreeKgs = (doc.get("loyaltyMaxFreeKgs") as? Number)?.toDouble() ?: 30.0
                        val loyaltyExpirationDays = (doc.get("loyaltyExpirationDays") as? Number)?.toInt() ?: 90
                        val categories = doc.getString("categories") ?: "Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons"
                        val businessName = doc.getString("businessName")
                            ?: doc.getString("nomEntreprise") ?: "BELLA — PRESSING"
                        val contactPhone = doc.getString("contactPhone")
                            ?: doc.getString("numeroEntreprise") ?: ""
                        val contactEmail = doc.getString("contactEmail")
                            ?: doc.getString("emailEntreprise") ?: ""
                        val currencySymbol = doc.getString("currencySymbol") ?: "FCFA"
                        val taxPercentage = (doc.get("taxPercentage") as? Number)?.toFloat() ?: 0f
                        val openingHours = doc.getString("openingHours")
                            ?: doc.getString("openHours") ?: ""
                        val closingHours = doc.getString("closingHours")
                            ?: doc.getString("closeHours") ?: ""
                        val openingDays = doc.getString("openingDays")
                            ?: doc.getString("openDays") ?: "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi"
                        val closingDays = doc.getString("closingDays") ?: "Dimanche"
                        val termsAndConditionsLink = doc.getString("termsAndConditionsLink")
                            ?: doc.getString("linkCGU") ?: ""

                        val configEntity = ConfigEntity(
                            id = "global_config",
                            baseTransportFee = baseTransportFee,
                            expressMultiplier = expressMultiplier,
                            fidelityThresholdKg = fidelityThresholdKg,
                            fidelityRewardValue = fidelityRewardValue,
                            pricePerKm = pricePerKm,
                            pressingLat = pressingLat,
                            pressingLng = pressingLng,
                            referralRewardKg = referralRewardKg,
                            appDownloadLink = appDownloadLink,
                            autoBanEnabled = autoBanEnabled,
                            autoBanThreshold = autoBanThreshold,
                            autoBanDurationHours = autoBanDurationHours,
                            isLoyaltyEnabled = isLoyaltyEnabled,
                            loyaltyValidUntil = loyaltyValidUntil,
                            isMaintenance = isMaintenance,
                            acceptNewOrders = acceptNewOrders,
                            categories = categories,
                            businessName = businessName,
                            contactPhone = contactPhone,
                            contactEmail = contactEmail,
                            currencySymbol = currencySymbol,
                            taxPercentage = taxPercentage,
                            openingHours = openingHours,
                            closingHours = closingHours,
                            openingDays = openingDays,
                            closingDays = closingDays,
                            termsAndConditionsLink = termsAndConditionsLink,
                            tarifBaseTransort = baseTransportFee.toDouble(),
                            prixParKilo = pricePerKm.toDouble(),
                            nomEntreprise = businessName,
                            numeroEntreprise = contactPhone,
                            emailEntreprise = contactEmail,
                            openDays = openingDays,
                            openHours = openingHours,
                            closeHours = closingHours,
                            linkCGU = termsAndConditionsLink,
                            isLoyaltyProgramEnabled = isLoyaltyProgramEnabled,
                            loyaltyKgThreshold = loyaltyKgThreshold,
                            loyaltyKgReward = loyaltyKgReward,
                            loyaltyMaxFreeKgs = loyaltyMaxFreeKgs,
                            loyaltyExpirationDays = loyaltyExpirationDays
                        )

                        val key = "Config_global_config"
                        incomingSyncTracker.add(key)
                        repository.insertConfig(configEntity)
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // TABLEAU DE BORD LISTENER
        db.collection("tableau_de_bord").addSnapshotListener { snapshot, e ->
            if (e != null) {
                if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                    triggerQuotaExhausted()
                }
                handleCriticalFirestoreError(e)
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    try {
                        if (change.type != com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val tdb = TableauDeBordEntity(
                                id = doc.id,
                                enAttend = (doc.get("enAttend") as? Number)?.toDouble() ?: 0.0,
                                acceptee = (doc.get("acceptee") as? Number)?.toDouble() ?: 0.0,
                                rejeter = (doc.get("rejeter") as? Number)?.toDouble() ?: 0.0,
                                termine = (doc.get("termine") as? Number)?.toDouble() ?: 0.0,
                                total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                            val key = "Tdb_${tdb.id}"
                            incomingSyncTracker.add(key)
                            repository.insertTableauDeBord(tdb)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // ORDERS HISTORY LISTENER
        db.collection("orders_history").addSnapshotListener { snapshot, e ->
            if (e != null) {
                if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                    triggerQuotaExhausted()
                }
                handleCriticalFirestoreError(e)
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    try {
                        if (change.type != com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val hist = OrdersHistoryEntity(
                                id = doc.id,
                                allHistory = doc.getString("allHistory") ?: "",
                                attendHistory = doc.getString("attendHistory") ?: "",
                                accepteeHistory = doc.getString("accepteeHistory") ?: "",
                                terminerHistory = doc.getString("terminerHistory") ?: "",
                                rejeterHistory = doc.getString("rejeterHistory") ?: "",
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                            val key = "Hist_${hist.id}"
                            incomingSyncTracker.add(key)
                            repository.insertOrdersHistory(hist)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // GLOBAL TRASH DUAL LISTENER
        val trashCollections = listOf("global_trash", "corbeille_globale")
        for (colName in trashCollections) {
            db.collection(colName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                        triggerQuotaExhausted()
                    }
                    handleCriticalFirestoreError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                repository.deleteGlobalTrash(doc.id)
                            } else {
                                val trash = GlobalTrashEntity(
                                    id = doc.id,
                                    originalId = doc.getString("originalId") ?: "",
                                    itemType = doc.getString("itemType") ?: "",
                                    title = doc.getString("title") ?: "",
                                    details = doc.getString("details") ?: "",
                                    deletedAt = doc.getLong("deletedAt") ?: System.currentTimeMillis(),
                                    adminResponsible = doc.getString("adminResponsible") ?: "",
                                    serializedData = doc.getString("serializedData") ?: ""
                                )
                                val key = "Trash_${trash.id}"
                                incomingSyncTracker.add(key)
                                repository.insertGlobalTrash(trash)
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // ACTIVITY LOGS REAL-TIME LISTENER
        db.collection("activity_logs").addSnapshotListener { snapshot, e ->
            if (e != null) {
                if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                    triggerQuotaExhausted()
                }
                handleCriticalFirestoreError(e)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    try {
                        if (change.type != com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val log = ActivityLogEntity(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                orderId = doc.getString("orderId"),
                                action = doc.getString("action") ?: "",
                                description = doc.getString("description") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                actorId = doc.getString("actorId") ?: doc.getString("adminId")
                            )
                            val key = "Log_${log.id}"
                            incomingSyncTracker.add(key)
                            repository.insertActivityLog(log)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        } // Fin de la désactivation des écouteurs permanents (protection Spark Quota)

        // --- 2. LOCAL ROOM CHANGE COLLECTORS (PUSH TO FIRESTORE) ---

        scope.launch {
            repository.getAllOrders().collect { orders ->
                for (order in orders) {
                    if (incomingSyncTracker.remove("Order_${order.id}")) {
                        continue
                    }
                    pushOrderToFirestore(order)
                }
            }
        }

        scope.launch {
            repository.getAllClients().collect { clients ->
                for (client in clients) {
                    if (incomingSyncTracker.remove("User_${client.id}")) {
                        continue
                    }
                    pushUserToFirestore(client)
                }
            }
        }

        scope.launch {
            repository.getAllAdmins().collect { admins ->
                for (admin in admins) {
                    if (incomingSyncTracker.remove("User_${admin.id}")) {
                        continue
                    }
                    pushUserToFirestore(admin)
                }
            }
        }

        scope.launch {
            repository.getConfig().collect { config ->
                if (config != null) {
                    if (incomingSyncTracker.remove("Config_global_config")) {
                        return@collect
                    }
                    pushConfigToFirestore(config)
                }
            }
        }

        scope.launch {
            repository.getRawAllPromos().collect { promos ->
                for (promo in promos) {
                    if (incomingSyncTracker.remove("Promo_${promo.id}")) {
                        continue
                    }
                    pushPromoToFirestore(promo)
                }
            }
        }

        scope.launch {
            repository.allAnnouncements.collect { announcements ->
                for (ann in announcements) {
                    if (incomingSyncTracker.remove("Ann_${ann.id}")) {
                        continue
                    }
                    pushAnnouncementToFirestore(ann)
                }
            }
        }

        scope.launch {
            repository.getRawAllGommageItems().collect { items ->
                for (item in items) {
                    if (incomingSyncTracker.remove("Gommage_${item.id}")) {
                        continue
                    }
                    pushGommageItemToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getRawAllSpecialItems().collect { items ->
                for (item in items) {
                    if (incomingSyncTracker.remove("Special_${item.id}")) {
                        continue
                    }
                    pushSpecialItemToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getRawAllConsumables().collect { items ->
                for (item in items) {
                    if (incomingSyncTracker.remove("Consumable_${item.id}")) {
                        continue
                    }
                    pushConsumableToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getRawAllRules().collect { items ->
                for (item in items) {
                    if (incomingSyncTracker.remove("Rule_${item.id}")) {
                        continue
                    }
                    pushRuleToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getRawAllAntiFraudRules().collect { items ->
                for (item in items) {
                    if (incomingSyncTracker.remove("AntiFraud_${item.id}")) {
                        continue
                    }
                    pushAntiFraudRuleToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getAllCategoriesLingeSaved().collect { list ->
                for (item in list) {
                    if (incomingSyncTracker.remove("Category_${item.id}")) {
                        continue
                    }
                    pushCategoryLingeSavedToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getTableauDeBord().collect { tdb ->
                if (tdb != null) {
                    if (incomingSyncTracker.remove("Tdb_${tdb.id}")) {
                        return@collect
                    }
                    pushTableauDeBordToFirestore(tdb)
                }
            }
        }

        scope.launch {
            repository.getOrdersHistory().collect { hist ->
                if (hist != null) {
                    if (incomingSyncTracker.remove("Hist_${hist.id}")) {
                        return@collect
                    }
                    pushOrdersHistoryToFirestore(hist)
                }
            }
        }

        scope.launch {
            repository.getAllGlobalTrash().collect { list ->
                for (item in list) {
                    if (incomingSyncTracker.remove("Trash_${item.id}")) {
                        continue
                    }
                    pushGlobalTrashToFirestore(item)
                }
            }
        }

        scope.launch {
            repository.getAllActivityLogs().collect { logs ->
                for (log in logs) {
                    if (incomingSyncTracker.remove("Log_${log.id}")) {
                        continue
                    }
                    pushActivityLogToFirestore(log)
                }
            }
        }
    }

    // --- 3. FIRESTORE WRITE OPERATIONS ---

    private suspend fun pushOrderToFirestore(order: OrderEntity) {
        val db = firestore ?: return
        try {
            val orderMap = mutableMapOf<String, Any?>(
                "kg" to (if (order.weight > 0.0) order.weight else order.kg.toDouble()),
                "status" to order.status,
                "prixFinal" to (if (order.totalPrice > 0.0) order.totalPrice else order.prixFinal.toDouble()),
                "timelineStep" to order.timelineStep,
                "adminMessage" to order.adminMessage,
                "isArchived" to order.isArchived,
                "photoUri" to order.photoUri,
                "imageUrl" to order.photoUri,
                "appareilPhoto" to order.photoUri,

                // New Schema integration
                "autoCompletionInfos" to order.autoCompletionInfos,
                "nombrePieces" to order.nombrePieces,
                "choixCategory" to order.choixCategory,
                "lavageSimple" to order.lavageSimple,
                "lavageSechagePliage" to order.lavageSechagePliage,
                "lavageSechagePliageRepassage" to order.lavageSechagePliageRepassage,
                "articlesUniquement" to order.articlesUniquement,
                "choixArticleSpeciaux" to order.choixArticleSpeciaux,
                "choixArticleGommage" to order.choixArticleGommage,
                "estimationKilo" to order.estimationKilo,
                "depotAgence" to order.depotAgence,
                "dateHeureDepot" to order.dateHeureDepot,
                "dateHeureRetrait" to order.dateHeureRetrait,
                "ramassage" to order.ramassage,
                "coordonneesMaps" to order.coordonneesMaps,
                "instructionsSpecial" to order.instructionsSpecial,
                "serviceExpress" to order.serviceExpress,
                "garantiVIP" to order.garantiVIP,
                "saisiCodePromos" to order.saisiCodePromos,
                "prixEstimerAutoCalcul" to order.prixEstimerAutoCalcul,
                "orderRecu" to order.orderRecu,
                "enPleinLavage" to order.enPleinLavage,
                "enSechage" to order.enSechage,
                "orderPret" to order.orderPret,
                "orderLivree" to order.orderLivree,
                "avisUsers" to order.avisUsers,
                "fidelityKgUsed" to order.fidelityKgUsed,
                "fideliteKgUtilise" to order.fideliteKgUtilise,
                "fidelityCredited" to order.fidelityCredited,
                "fideliteCredite" to order.fideliteCredite,

                // Enriched Admin & Livreur fields
                "weight" to order.weight,
                "totalPrice" to order.totalPrice,
                "adminNotes" to order.adminNotes,
                "deliveryBoyId" to order.deliveryBoyId,
                "customerName" to order.customerName,
                "customerPhone" to order.customerPhone,
                "deliveryAddress" to order.deliveryAddress
            )
            if (order.transportFee > 0) orderMap["transportFee"] = order.transportFee
            if (order.pickupDate.isNotEmpty()) orderMap["pickupDate"] = order.pickupDate
            if (order.deliveryDate.isNotEmpty()) orderMap["deliveryDate"] = order.deliveryDate
            if (order.pieces > 0) orderMap["pieces"] = order.pieces
            if (order.categorie.isNotEmpty()) orderMap["categorie"] = order.categorie
            if (order.lavageType.isNotEmpty()) orderMap["lavageType"] = order.lavageType
            if (order.createdAt > 0) orderMap["createdAt"] = order.createdAt
            if (order.rating > 0) orderMap["rating"] = order.rating
            if (order.clientComment.isNotEmpty()) orderMap["clientComment"] = order.clientComment
            if (order.adresseLivraison.isNotEmpty()) orderMap["adresseLivraison"] = order.adresseLivraison
            if (order.isExpress) orderMap["isExpress"] = order.isExpress
            if (order.instructionsSpeciales.isNotEmpty()) orderMap["instructionsSpeciales"] = order.instructionsSpeciales
            if (order.deliveryType.isNotEmpty()) orderMap["deliveryType"] = order.deliveryType
            if (order.specialItemsData.isNotEmpty()) orderMap["specialItemsData"] = order.specialItemsData
            if (order.latitude != 0.0) orderMap["latitude"] = order.latitude
            if (order.longitude != 0.0) orderMap["longitude"] = order.longitude
            if (order.distanceKm > 0) orderMap["distanceKm"] = order.distanceKm
            if (order.promoCode.isNotEmpty()) orderMap["promoCode"] = order.promoCode
            if (order.discountPercentage > 0) orderMap["discountPercentage"] = order.discountPercentage
            if (order.discountFixed > 0) orderMap["discountFixed"] = order.discountFixed
            if (order.hasPremiumGuarantee) orderMap["hasPremiumGuarantee"] = order.hasPremiumGuarantee
            if (order.insuranceFee > 0) orderMap["insuranceFee"] = order.insuranceFee

            // Push to both "orders" and "ordres"!
            db.collection("orders").document(order.id).set(orderMap, SetOptions.merge()).await()
            db.collection("ordres").document(order.id).set(orderMap, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushUserToFirestore(user: UserEntity) {
        val db = firestore ?: return
        try {
            if (user.email == "admin@bellapressing.com" || user.email == "client@test.com") {
                val existingSnap = db.collection("users").whereEqualTo("email", user.email).get().await()
                if (!existingSnap.isEmpty) {
                    val remoteDoc = existingSnap.documents.first()
                    if (remoteDoc.id != user.id) {
                        Log.d("FirestoreSyncManager", "Skipping duplicate/placeholder push for user: ${user.email}")
                        return
                    }
                }
            }
            val userMap = mapOf(
                "nom" to user.nom,
                "prenom" to user.prenom,
                "email" to user.email,
                "motDePasse" to user.motDePasse,
                "role" to user.role,
                "telephone" to user.telephone,
                "coordonnees" to user.coordonnees,
                "token" to user.token,
                "fidelityPointsKg" to user.fidelityPointsKg,
                "myReferralCode" to user.myReferralCode,
                "referredBy" to user.referredBy,
                "isSuspended" to user.isSuspended,
                "estSuspendu" to user.isSuspended,
                "suspensionReason" to user.suspensionReason,
                "raisonSuspension" to user.suspensionReason,
                "suspensionUntil" to user.suspensionUntil,

                "isBanned" to user.isBanned,
                "estBanni" to user.isBanned,
                "banni" to user.isBanned,
                "banReason" to user.banReason,
                "raisonBannissement" to user.banReason,
                "motifBannissement" to user.banReason,
                "isBanPermanent" to user.isBanPermanent,
                "bannissementPermanent" to user.isBanPermanent,
                "banUntil" to user.banUntil,
                "dateFinBannissement" to user.banUntil,

                "canUsePromo" to user.canUsePromo,
                "canUsePromoUntil" to user.canUsePromoUntil,
                "canOrder" to user.canOrder,
                "isArchived" to user.isArchived,
                "permissions" to user.permissions,
                "isLoyaltyEnabled" to user.isLoyaltyEnabled,
                "loyaltyValidUntil" to user.loyaltyValidUntil,
                "accumulatedFreeKgs" to user.accumulatedFreeKgs,
                "currentKgTowardsReward" to user.currentKgTowardsReward,
                "lifetimeKgWashed" to user.lifetimeKgWashed,
                "customAutoBanEnabled" to user.customAutoBanEnabled,
                "customAutoBanThreshold" to user.customAutoBanThreshold,
                "customAutoBanDurationHours" to user.customAutoBanDurationHours,
                "canSelectPieces" to user.canSelectPieces,
                "canSelectCategories" to user.canSelectCategories,
                "canEstimateKg" to user.canEstimateKg,
                "canUseExpress" to user.canUseExpress,
                "canUsePremium" to user.canUsePremium,

                // New Schema
                "prenomAdmin" to user.prenomAdmin,
                "nomAdmin" to user.nomAdmin,
                "mailAdmin" to user.mailAdmin,
                "motDePasseAdmin" to user.motDePasseAdmin,
                "roleAdminSuper" to user.roleAdminSuper,

                "permVoirCommandes" to user.permVoirCommandes,
                "permGererCommandes" to user.permGererCommandes,
                "permModifierHeureRamassage" to user.permModifierHeureRamassage,
                "permVoirClients" to user.permVoirClients,
                "permGererClients" to user.permGererClients,
                "permSuspendreClients" to user.permSuspendreClients,
                "permVoirLog" to user.permVoirLog,
                "permGererPromosClient" to user.permGererPromosClient,
                "permVoirCorbeille" to user.permVoirCorbeille,
                "permRestaurerCompte" to user.permRestaurerCompte,
                "permSupprimerDefinitivement" to user.permSupprimerDefinitivement,
                "permVoirStocks" to user.permVoirStocks,
                "permGererStocks" to user.permGererStocks,
                "permGererStocksVisibilite" to user.permGererStocksVisibilite,
                "permVoirCatalogue" to user.permVoirCatalogue,
                "permGererCatalogue" to user.permGererCatalogue,
                "permGererPromos" to user.permGererPromos,
                "permGererPromosAvancees" to user.permGererPromosAvancees,
                "permGererGommage" to user.permGererGommage,
                "permVoirParametres" to user.permVoirParametres,
                "permGererParametres" to user.permGererParametres,
                "permVoirAntiFraudPersonnel" to user.permVoirAntiFraudPersonnel,
                "permGererAntiFraudPersonnel" to user.permGererAntiFraudPersonnel,
                "permVoirAntiFraudGlobal" to user.permVoirAntiFraudGlobal,
                "permGererAntiFraudGlobal" to user.permGererAntiFraudGlobal,

                "suppressionDefinitiveAdmin" to user.suppressionDefinitiveAdmin,
                "restaurerAdmin" to user.restaurerAdmin,

                "prenomUser" to user.prenomUser,
                "nomUser" to user.nomUser,
                "numeroUser" to user.numeroUser,
                "mailUser" to user.mailUser,
                "adresseUser" to user.adresseUser,
                "motDePasseUser" to user.motDePasseUser,
                "cumulKiloFideliterUser" to user.cumulKiloFideliterUser,

                "autoriseCreatOrder" to user.autoriseCreatOrder,
                "autoriseSaisiNombrePiece" to user.autoriseSaisiNombrePiece,
                "autoriseSelectionCategory" to user.autoriseSelectionCategory,
                "autoriseSelectionCatalogue" to user.autoriseSelectionCatalogue,
                "autoriseSelectionArticlesSpecial" to user.autoriseSelectionArticlesSpecial,
                "autoriseSelectionArticlesGommage" to user.autoriseSelectionArticlesGommage,
                "autoriseServiceLavage" to user.autoriseServiceLavage,
                "autoriseLavageSimple" to user.autoriseLavageSimple,
                "autoriseLavageSechagePliage" to user.autoriseLavageSechagePliage,
                "autoriseLavageSechagePliageRepassage" to user.autoriseLavageSechagePliageRepassage,
                "autoriseRamassageLinges" to user.autoriseRamassageLinges,
                "autoriseFixeeDateHeureRamassage" to user.autoriseFixeeDateHeureRamassage,
                "autoriseDateHeureLivraison" to user.autoriseDateHeureLivraison,
                "autoriseInstructionSpecial" to user.autoriseInstructionSpecial,
                "autoriseServiceExpress" to user.autoriseServiceExpress,
                "autoriseGarantiVIP" to user.autoriseGarantiVIP,
                "autoriseUtiliserCodePromos" to user.autoriseUtiliserCodePromos,
                "autoriseVoirEstimationPrixFinal" to user.autoriseVoirEstimationPrixFinal,

                "utiliseAntiFraudeGlobale" to user.utiliseAntiFraudeGlobale,
                "blocagePersistant" to user.blocagePersistant,
                "blocageLimitee" to user.blocageLimitee,
                "dureeBlocage" to user.dureeBlocage,
                "raisonSuspension" to user.raisonSuspension
            )

            // Push to both "users" and "utilisateurs"!
            val collections = listOf("users", "utilisateurs")
            for (colName in collections) {
                db.collection(colName).document(user.id).set(userMap, SetOptions.merge()).await()
                if (user.email.isNotBlank()) {
                    try {
                        val snapByEmail = db.collection(colName).whereEqualTo("email", user.email).get().await()
                        for (doc in snapByEmail.documents) {
                            if (doc.id != user.id) {
                                doc.reference.set(userMap, SetOptions.merge()).await()
                            }
                        }
                        val snapByMailUser = db.collection(colName).whereEqualTo("mailUser", user.email).get().await()
                        for (doc in snapByMailUser.documents) {
                            if (doc.id != user.id) {
                                doc.reference.set(userMap, SetOptions.merge()).await()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("FirestoreSyncManager", "Secondary email push failed for ${user.email}", e)
                    }
                }
            }
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushConfigToFirestore(config: ConfigEntity) {
        val db = firestore ?: return
        try {
            if (config.id == "default_config") {
                Log.d("FirestoreSyncManager", "Skipping push of default local config to prevent overwriting cloud")
                return
            }
            val configMap = mapOf(
                "baseTransportFee" to config.baseTransportFee,
                "expressMultiplier" to config.expressMultiplier,
                "fidelityThresholdKg" to config.fidelityThresholdKg,
                "fidelityRewardValue" to config.fidelityRewardValue,
                "pricePerKm" to config.pricePerKm,
                "pressingLat" to config.pressingLat,
                "pressingLng" to config.pressingLng,
                "referralRewardKg" to config.referralRewardKg,
                "appDownloadLink" to config.appDownloadLink,
                "autoBanEnabled" to config.autoBanEnabled,
                "autoBanThreshold" to config.autoBanThreshold,
                "autoBanDurationHours" to config.autoBanDurationHours,
                "isLoyaltyEnabled" to config.isLoyaltyEnabled,
                "loyaltyValidUntil" to config.loyaltyValidUntil,
                "isMaintenance" to config.isMaintenance,
                "acceptNewOrders" to config.acceptNewOrders,
                "categories" to config.categories,
                "businessName" to config.businessName,
                "contactPhone" to config.contactPhone,
                "contactEmail" to config.contactEmail,
                "currencySymbol" to config.currencySymbol,
                "taxPercentage" to config.taxPercentage,
                "openingHours" to config.openingHours,
                "closingHours" to config.closingHours,
                "openingDays" to config.openingDays,
                "closingDays" to config.closingDays,
                "termsAndConditionsLink" to config.termsAndConditionsLink,
                "tarifBaseTransort" to config.tarifBaseTransort,
                "prixParKilo" to config.prixParKilo,
                "nomEntreprise" to config.nomEntreprise,
                "numeroEntreprise" to config.numeroEntreprise,
                "emailEntreprise" to config.emailEntreprise,
                "openDays" to config.openDays,
                "openHours" to config.openHours,
                "closeHours" to config.closeHours,
                "linkCGU" to config.linkCGU,
                "isLoyaltyProgramEnabled" to config.isLoyaltyProgramEnabled,
                "loyaltyKgThreshold" to config.loyaltyKgThreshold,
                "loyaltyKgReward" to config.loyaltyKgReward,
                "loyaltyMaxFreeKgs" to config.loyaltyMaxFreeKgs,
                "loyaltyExpirationDays" to config.loyaltyExpirationDays
            )
            db.collection("general_settings").document("main").set(configMap, SetOptions.merge()).await()
            db.collection("config").document("main").set(configMap, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushPromoToFirestore(promo: PromoEntity) {
        val db = firestore ?: return
        try {
            val promoMap = mapOf(
                "code" to promo.code,
                "description" to promo.description,
                "discountPercent" to promo.discountPercent,
                "freeKg" to promo.freeKg,
                "minOrderValue" to promo.minOrderValue,
                "isActive" to promo.isActive,
                "validUntil" to promo.validUntil,
                "isPermanent" to promo.isPermanent,
                "targetItems" to promo.targetItems,
                "discountType" to promo.discountType,
                "isArchived" to promo.isArchived
            )
            db.collection("promotions").document(promo.id).set(promoMap, SetOptions.merge()).await()
            db.collection("promos").document(promo.id).set(promoMap, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushAnnouncementToFirestore(ann: AnnouncementEntity) {
        val db = firestore ?: return
        try {
            val annMap = mapOf(
                "title" to ann.title,
                "message" to ann.message,
                "imageUrl" to ann.imageUrl,
                "isVisible" to ann.isVisible,
                "createdAt" to ann.createdAt,
                "nomAnnoce" to ann.nomAnnoce,
                "descriptionAnnoce" to ann.descriptionAnnoce,
                "fichiersMediaAnnoce" to ann.fichiersMediaAnnoce
            )
            db.collection("announcements").document(ann.id).set(annMap, SetOptions.merge()).await()
            db.collection("annonces").document(ann.id).set(annMap, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushGommageItemToFirestore(item: GommageItemEntity) {
        val db = firestore ?: return
        try {
            val itemMap = mapOf(
                "name" to item.name,
                "price" to item.price,
                "isArchived" to item.isArchived,
                "type" to "GOMMAGE"
            )
            db.collection("catalog").document(item.id).set(itemMap, SetOptions.merge()).await()
            db.collection("catalogue").document(item.id).set(itemMap, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushSpecialItemToFirestore(item: SpecialItemEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "name" to item.name,
                "price" to item.price,
                "isArchived" to item.isArchived,
                "type" to "SPECIAL"
            )
            db.collection("catalog").document(item.id).set(map, SetOptions.merge()).await()
            db.collection("catalogue").document(item.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushConsumableToFirestore(c: ConsumableEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "name" to c.name,
                "quantity" to c.quantity,
                "threshold" to c.threshold,
                "isPublic" to c.isPublic,
                "isArchived" to c.isArchived,
                "nomProduit" to c.nomProduit,
                "quantiterProduit" to c.quantiterProduit,
                "produitSeilleAlert" to c.produitSeilleAlert,
                "newStocksPublic" to c.newStocksPublic,
                "newStocksPrivee" to c.newStocksPrivee
            )
            db.collection("stocks").document(c.id).set(map, SetOptions.merge()).await()
            db.collection("stock").document(c.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushRuleToFirestore(r: RuleEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "name" to r.name,
                "triggerCondition" to r.triggerCondition,
                "actionType" to r.actionType,
                "actionValue" to r.actionValue,
                "constraints" to r.constraints,
                "isActive" to r.isActive,
                "isPermanent" to r.isPermanent,
                "validUntil" to r.validUntil,
                "isArchived" to r.isArchived
            )
            db.collection("automation_rules").document(r.id).set(map, SetOptions.merge()).await()
            db.collection("regles").document(r.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushAntiFraudRuleToFirestore(r: AntiFraudRuleEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "name" to r.name,
                "triggerEvent" to r.triggerEvent,
                "threshold" to r.threshold,
                "durationHours" to r.durationHours,
                "action" to r.action,
                "isActive" to r.isActive,
                "isArchived" to r.isArchived,
                "nomNewAntiFraud" to r.nomNewAntiFraud,
                "evenAntiFraud" to r.evenAntiFraud,
                "seuilAntiFraud" to r.seuilAntiFraud,
                "dureeAntiFraud" to r.dureeAntiFraud,
                "actionAntiFraud" to r.actionAntiFraud
            )
            db.collection("anti_fraud_rules").document(r.id).set(map, SetOptions.merge()).await()
            db.collection("regles_anti_fraude").document(r.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushCategoryLingeSavedToFirestore(cat: CategoriesLingeSavedEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "newCategorieLinge" to cat.newCategorieLinge,
                "createdAt" to cat.createdAt
            )
            db.collection("categories_linge_saved").document(cat.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushTableauDeBordToFirestore(tdb: TableauDeBordEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "enAttend" to tdb.enAttend,
                "acceptee" to tdb.acceptee,
                "rejeter" to tdb.rejeter,
                "termine" to tdb.termine,
                "total" to tdb.total,
                "updatedAt" to tdb.updatedAt
            )
            db.collection("tableau_de_bord").document(tdb.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushOrdersHistoryToFirestore(hist: OrdersHistoryEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "allHistory" to hist.allHistory,
                "attendHistory" to hist.attendHistory,
                "accepteeHistory" to hist.accepteeHistory,
                "terminerHistory" to hist.terminerHistory,
                "rejeterHistory" to hist.rejeterHistory,
                "updatedAt" to hist.updatedAt
            )
            db.collection("orders_history").document(hist.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushGlobalTrashToFirestore(trash: GlobalTrashEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "originalId" to trash.originalId,
                "itemType" to trash.itemType,
                "title" to trash.title,
                "details" to trash.details,
                "deletedAt" to trash.deletedAt,
                "adminResponsible" to trash.adminResponsible,
                "serializedData" to trash.serializedData
            )
            db.collection("global_trash").document(trash.id).set(map, SetOptions.merge()).await()
            db.collection("corbeille_globale").document(trash.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }

    private suspend fun pushActivityLogToFirestore(log: ActivityLogEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf(
                "userId" to log.userId,
                "orderId" to log.orderId,
                "action" to log.action,
                "description" to log.description,
                "timestamp" to log.timestamp,
                "actorId" to log.actorId
            )
            db.collection("activity_logs").document(log.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            e.printStackTrace()
        }
    }
}
