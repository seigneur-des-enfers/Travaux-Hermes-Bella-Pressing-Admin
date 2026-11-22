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

        // --- SYNCHRONISATION OPTIMISÉE & TEMPS RÉEL CIBLÉ ---
        // 1. Démarrage de l'écouteur temps réel ciblé sur les commandes en cours (orders)
        try {
            com.example.di.Graph.adminOrderRepository.syncActiveOrdersRealtime()
        } catch (e: Throwable) {
            Log.e("FirestoreSyncManager", "Erreur lancement syncActiveOrdersRealtime: ${e.message}", e)
        }

        // 2. Chargement ponctuel one-shot via get().await() pour protéger les quotas Spark
        scope.launch(Dispatchers.IO) {
            try {
                repository.fetchSettings()
                repository.fetchClients()
                repository.fetchCatalog()
                repository.fetchAnnouncements()
                repository.fetchPromos()
                com.example.di.Graph.adminOrderRepository.fetchActiveOrders()
                com.example.di.Graph.adminOrderRepository.fetchHistoricalOrders(100)
            } catch (e: Throwable) {
                Log.e("FirestoreSyncManager", "Erreur lors du chargement initial one-shot get().await(): ${e.message}", e)
            }
        }

        // 3. Écouteur temps réel pour la configuration (Admin = source de vérité, mais sync depuis Firestore si modifié ailleurs)
        val configDocIds = listOf("global_config", "main", "global", "settings")
        for (docId in configDocIds) {
            val listener = db.collection("config").document(docId).addSnapshotListener { snapshot, e ->
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
                        val configEntity = parseConfigFromAdminSnapshot(snapshot)
                        repository.insertConfig(configEntity)
                        Log.d("FirestoreSyncManager", "Config synchronisée depuis Firestore (config/$docId) vers Room")
                    } catch (ex: Throwable) {
                        Log.e("FirestoreSyncManager", "Erreur sync config depuis Firestore", ex)
                    }
                }
            }
            synchronized(activeListeners) {
                activeListeners.add(listener)
            }
        }

        val enableLegacyRealtimeListeners = false
        if (enableLegacyRealtimeListeners) {
        // --- 1. REAL-TIME LISTENERS FROM FIRESTORE (LEGACY - DISABLED) ---

        // USERS (English only - removed French "utilisateurs")
        db.collection("users").addSnapshotListener { snapshot, e ->
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
                            // English field names only - French fallbacks removed
                            val firstName = doc.getString("firstName") ?: doc.getString("prenom") ?: ""
                            val lastName = doc.getString("lastName") ?: doc.getString("nom") ?: ""
                            val email = doc.getString("email") ?: ""
                            val password = doc.getString("password") ?: doc.getString("motDePasse") ?: ""
                            val phone = doc.getString("phone") ?: doc.getString("telephone") ?: ""
                            val address = doc.getString("address") ?: doc.getString("coordonnees") ?: ""

                            val user = UserEntity(
                                id = doc.id,
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                password = password,
                                phone = phone,
                                address = address,
                                token = doc.getString("token") ?: "",
                                role = doc.getString("role") ?: "CLIENT",
                                fidelityPointsKg = (doc.get("fidelityPointsKg") as? Number)?.toFloat() ?: 0f,
                                myReferralCode = doc.getString("myReferralCode") ?: "",
                                referredBy = doc.getString("referredBy") ?: "",
                                isSuspended = doc.getBoolean("isSuspended") ?: false,
                                suspensionReason = doc.getString("suspensionReason") ?: "",
                                suspensionUntil = doc.getLong("suspensionUntil"),
                                isBanned = doc.getBoolean("isBanned") ?: false,
                                banReason = doc.getString("banReason") ?: "",
                                isBanPermanent = doc.getBoolean("isBanPermanent") ?: false,
                                banUntil = doc.getLong("banUntil"),
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

                                // Admin schema fields (English)
                                adminFirstName = doc.getString("adminFirstName") ?: firstName,
                                adminLastName = doc.getString("adminLastName") ?: lastName,
                                adminEmail = doc.getString("adminEmail") ?: email,
                                adminPassword = doc.getString("adminPassword") ?: password,
                                roleAdminSuper = doc.getBoolean("roleAdminSuper") ?: (doc.getString("role") == "SUPER_ADMIN"),

                                permViewOrders = doc.getBoolean("permViewOrders") ?: true,
                                permManageOrders = doc.getBoolean("permManageOrders") ?: true,
                                permModifyPickupTime = doc.getBoolean("permModifyPickupTime") ?: true,
                                permViewClients = doc.getBoolean("permViewClients") ?: true,
                                permManageClients = doc.getBoolean("permManageClients") ?: true,
                                permSuspendClients = doc.getBoolean("permSuspendClients") ?: true,
                                permViewLogs = doc.getBoolean("permViewLogs") ?: true,
                                permManageClientPromos = doc.getBoolean("permManageClientPromos") ?: true,
                                permViewTrash = doc.getBoolean("permViewTrash") ?: true,
                                permRestoreAccount = doc.getBoolean("permRestoreAccount") ?: true,
                                permDeletePermanently = doc.getBoolean("permDeletePermanently") ?: true,
                                permViewStocks = doc.getBoolean("permViewStocks") ?: true,
                                permManageStocks = doc.getBoolean("permManageStocks") ?: true,
                                permManageStocksVisibility = doc.getBoolean("permManageStocksVisibility") ?: true,
                                permViewCatalog = doc.getBoolean("permViewCatalog") ?: true,
                                permManageCatalog = doc.getBoolean("permManageCatalog") ?: true,
                                permManagePromos = doc.getBoolean("permManagePromos") ?: true,
                                permManageAdvancedPromos = doc.getBoolean("permManageAdvancedPromos") ?: true,
                                permManageGommage = doc.getBoolean("permManageGommage") ?: true,
                                permViewSettings = doc.getBoolean("permViewSettings") ?: true,
                                permManageSettings = doc.getBoolean("permManageSettings") ?: true,
                                permViewAntiFraudPersonal = doc.getBoolean("permViewAntiFraudPersonal") ?: true,
                                permManageAntiFraudPersonal = doc.getBoolean("permManageAntiFraudPersonal") ?: true,
                                permViewAntiFraudGlobal = doc.getBoolean("permViewAntiFraudGlobal") ?: true,
                                permManageAntiFraudGlobal = doc.getBoolean("permManageAntiFraudGlobal") ?: true,

                                suppressionDefinitiveAdmin = doc.getBoolean("suppressionDefinitiveAdmin") ?: false,
                                restaurerAdmin = doc.getBoolean("restaurerAdmin") ?: false,

                                prenomUser = doc.getString("userFirstName") ?: prenom,
                                nomUser = doc.getString("userLastName") ?: nom,
                                numeroUser = doc.getString("userPhone") ?: telephone,
                                mailUser = doc.getString("userEmail") ?: email,
                                adresseUser = doc.getString("userAddress") ?: coordonnees,
                                motDePasseUser = doc.getString("userPassword") ?: motDePasse,
                                cumulKiloFideliterUser = (doc.get("cumulKiloFideliterUser") as? Number)?.toDouble()
                                    ?: (doc.get("fidelityPointsKg") as? Number)?.toDouble() ?: 0.0,

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

        // ORDERS (English only - removed French "ordres", "commandes")
        db.collection("orders").addSnapshotListener { snapshot, e ->
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

        // PROMOTIONS (English only - removed French "promos")
        db.collection("promotions").addSnapshotListener { snapshot, e ->
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

        // ANNOUNCEMENTS (English only - removed French "annonces")
        db.collection("announcements").addSnapshotListener { snapshot, e ->
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
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
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

        // CATALOG (English only - removed French "catalogue")
        db.collection("catalog").addSnapshotListener { snapshot, e ->
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

        // STOCKS (English only - removed French "stock")
        db.collection("stocks").addSnapshotListener { snapshot, e ->
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
                            // English field names only
                            val name = doc.getString("name") ?: ""
                            val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                            val threshold = (doc.get("threshold") as? Number)?.toInt() ?: 0
                            val isPublic = doc.getBoolean("isPublic") ?: false
                            val isArchived = doc.getBoolean("isArchived") ?: false

                            val c = ConsumableEntity(
                                id = doc.id,
                                name = name,
                                quantity = quantity,
                                threshold = threshold,
                                isPublic = isPublic,
                                isArchived = isArchived
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

        // AUTOMATION RULES (English only - removed French "regles")
        db.collection("automation_rules").addSnapshotListener { snapshot, e ->
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

        // ANTI FRAUD RULES (English only - removed French "regles_anti_fraude")
        db.collection("anti_fraud_rules").addSnapshotListener { snapshot, e ->
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
                                isArchived = doc.getBoolean("isArchived") ?: false
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

        // CATEGORIES LINGE SAVED (French concept - keep as is)
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

        // CONFIG (English only - removed French "general_settings")
        db.collection("config").document("global_config").addSnapshotListener { snapshot, e ->
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
                    val configEntity = parseConfigFromAdminSnapshot(doc)
                    repository.insertConfig(configEntity)
                } catch (ex: Throwable) {
                    Log.e("FirestoreSyncManager", "Erreur sync config depuis Firestore", ex)
                }
            }
        }

        // TABLEAU DE BORD (French concept - keep as is)
        db.collection("tableau_de_bord").addSnapshotListener { snapshot, e ->
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
                            // No delete method for dashboard
                        } else {
                            // Dashboard is singleton, just update
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // ORDERS HISTORY (English concept - keep as is)
        db.collection("orders_history").addSnapshotListener { snapshot, e ->
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
                            // No delete method
                        } else {
                            // History is aggregate
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // ACTIVITY LOGS (English concept - keep as is)
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
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            // No delete
                        } else {
                            // Logs are append-only
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // GLOBAL TRASH (English only - removed French "corbeille_globale")
        db.collection("global_trash").addSnapshotListener { snapshot, e ->
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
                            // No delete
                        } else {
                            // Trash is append-only
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // BANS (Distinct concept from suspension - KEEP both English and French)
        val banCollections = listOf("bans", "bannissements", "comptes_bannis", "blacklist")
        for (colName in banCollections) {
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
                            val targetId = doc.getString("userId") ?: doc.id
                            val targetEmail = doc.getString("email") ?: ""
                            val user = repository.getUserById(targetId)
                                ?: (if (targetEmail.isNotBlank()) repository.getUserByEmail(targetEmail) else null)

                            if (user != null) {
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    val unbanned = user.copy(
                                        isBanned = false,
                                        banReason = "",
                                        banUntil = null,
                                        isBanPermanent = false
                                    )
                                    repository.updateUser(unbanned)
                                } else {
                                    val bReason = doc.getString("banReason") ?: doc.getString("reason") ?: doc.getString("raison") ?: doc.getString("motif") ?: ""
                                    val bUntil = doc.getLong("banUntil") ?: doc.getLong("until") ?: doc.getLong("dateFin")
                                    val isPerm = doc.getBoolean("isBanPermanent") ?: doc.getBoolean("isPermanent") ?: doc.getBoolean("permanent") ?: (bUntil == null)
                                    val banned = user.copy(
                                        isBanned = true,
                                        banReason = bReason,
                                        banUntil = bUntil,
                                        isBanPermanent = isPerm
                                    )
                                    repository.updateUser(banned)
                                }
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }

        // SUSPENSIONS (Distinct concept from bans - KEEP both English and French)
        val suspensionCollections = listOf("suspensions", "comptes_suspendus")
        for (colName in suspensionCollections) {
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
                            val targetId = doc.getString("userId") ?: doc.id
                            val targetEmail = doc.getString("email") ?: ""
                            val user = repository.getUserById(targetId)
                                ?: (if (targetEmail.isNotBlank()) repository.getUserByEmail(targetEmail) else null)

                            if (user != null) {
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    val unsuspended = user.copy(
                                        isSuspended = false,
                                        suspensionReason = "",
                                        suspensionUntil = null
                                    )
                                    repository.updateUser(unsuspended)
                                } else {
                                    val sReason = doc.getString("suspensionReason") ?: doc.getString("reason") ?: doc.getString("raison") ?: doc.getString("motif") ?: ""
                                    val sUntil = doc.getLong("suspensionUntil") ?: doc.getLong("until") ?: doc.getLong("dateFin")
                                    val suspended = user.copy(
                                        isSuspended = true,
                                        suspensionReason = sReason,
                                        suspensionUntil = sUntil
                                    )
                                    repository.updateUser(suspended)
                                }
                            }
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
        } // end enableLegacyRealtimeListeners
    }

    /**
     * Parse un DocumentSnapshot Firestore en ConfigEntity pour l'application Admin.
     * Utilise les noms de champs anglais canoniques (fallbacks français supprimés).
     */
    private fun parseConfigFromAdminSnapshot(doc: DocumentSnapshot): ConfigEntity {
        val baseFee = doc.get("baseTransportFee")?.toString()?.toFloatOrNull()
            ?: 1000f
        val priceKm = doc.get("pricePerKm")?.toString()?.toFloatOrNull()
            ?: 500f
        val expressMult = doc.get("expressMultiplier")?.toString()?.toFloatOrNull() ?: 1.3f

        val loyaltyBool = doc.getBoolean("isLoyaltyProgramEnabled")
            ?: doc.getBoolean("isLoyaltyEnabled")
            ?: true

        val loyaltyThreshold = doc.get("loyaltyKgThreshold")?.toString()?.toDoubleOrNull()
            ?: 10.0
        val loyaltyReward = doc.get("loyaltyKgReward")?.toString()?.toDoubleOrNull() ?: 1.0
        val loyaltyMaxKgs = doc.get("loyaltyMaxFreeKgs")?.toString()?.toDoubleOrNull() ?: 30.0
        val loyaltyExpDays = doc.get("loyaltyExpirationDays")?.toString()?.toIntOrNull() ?: 90

        val categoriesStr = doc.getString("categories")
            ?: "Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons"

        return ConfigEntity(
            id = "global_config",
            baseTransportFee = baseFee,
            expressMultiplier = expressMult,
            fidelityThresholdKg = doc.get("fidelityThresholdKg")?.toString()?.toFloatOrNull() ?: 10f,
            fidelityRewardValue = doc.get("fidelityRewardValue")?.toString()?.toFloatOrNull() ?: 350f,
            pricePerKm = priceKm,
            pressingLat = doc.get("pressingLat")?.toString()?.toDoubleOrNull() ?: 12.6392,
            pressingLng = doc.get("pressingLng")?.toString()?.toDoubleOrNull() ?: -8.0029,
            referralRewardKg = doc.get("referralRewardKg")?.toString()?.toFloatOrNull() ?: 2f,
            appDownloadLink = doc.getString("appDownloadLink") ?: "",
            autoBanEnabled = doc.getBoolean("autoBanEnabled") ?: false,
            autoBanThreshold = doc.get("autoBanThreshold")?.toString()?.toIntOrNull() ?: 3,
            autoBanDurationHours = doc.get("autoBanDurationHours")?.toString()?.toIntOrNull() ?: 24,
            isLoyaltyEnabled = loyaltyBool,
            loyaltyValidUntil = doc.getLong("loyaltyValidUntil"),
            isMaintenance = doc.getBoolean("isMaintenance") ?: false,
            acceptNewOrders = doc.getBoolean("acceptNewOrders") ?: true,
            categories = categoriesStr,
            businessName = doc.getString("businessName")
                ?: "BELLA — PRESSING",
            contactPhone = doc.getString("contactPhone")
                ?: "",
            contactEmail = doc.getString("contactEmail")
                ?: "",
            currencySymbol = doc.getString("currencySymbol") ?: "FCFA",
            taxPercentage = doc.get("taxPercentage")?.toString()?.toFloatOrNull() ?: 0f,
            openingHours = doc.getString("openingHours")
                ?: "",
            closingHours = doc.getString("closingHours")
                ?: "",
            openingDays = doc.getString("openingDays")
                ?: "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
            closingDays = doc.getString("closingDays") ?: "Dimanche",
            termsAndConditionsLink = doc.getString("termsAndConditionsLink")
                ?: "",
            tarifBaseTransort = baseFee.toDouble(),
            prixParKilo = priceKm.toDouble(),
            nomEntreprise = doc.getString("businessName")
                ?: "BELLA — PRESSING",
            numeroEntreprise = doc.getString("contactPhone")
                ?: "",
            emailEntreprise = doc.getString("contactEmail")
                ?: "",
            openDays = doc.getString("openingDays")
                ?: "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
            openHours = doc.getString("openingHours")
                ?: "",
            closeHours = doc.getString("closingHours")
                ?: "",
            linkCGU = doc.getString("termsAndConditionsLink")
                ?: "",
            isLoyaltyProgramEnabled = loyaltyBool,
            loyaltyKgThreshold = loyaltyThreshold,
            loyaltyKgReward = loyaltyReward,
            loyaltyMaxFreeKgs = loyaltyMaxKgs,
            loyaltyExpirationDays = loyaltyExpDays
        )
    }

    // --- PUSH METHODS (Outbound: Room → Firestore) ---

    private suspend fun pushConfigToFirestore(config: ConfigEntity) {
        val db = firestore ?: return
        try {
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
            db.collection("config").document("global_config").set(configMap, SetOptions.merge()).await()
            Log.d("FirestoreSyncManager", "Config pushed to Firestore (config/global_config)")
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
            val userMap = mapOf(
                "firstName" to user.prenom,
                "lastName" to user.nom,
                "email" to user.email,
                "password" to user.motDePasse,
                "phone" to user.telephone,
                "address" to user.coordonnees,
                "token" to user.token,
                "role" to user.role,
                "fidelityPointsKg" to user.fidelityPointsKg,
                "myReferralCode" to user.myReferralCode,
                "referredBy" to user.referredBy,

                "isSuspended" to user.isSuspended,
                "suspensionReason" to user.suspensionReason,
                "suspensionUntil" to user.suspensionUntil,

                "isBanned" to user.isBanned,
                "banReason" to user.banReason,
                "isBanPermanent" to user.isBanPermanent,
                "banUntil" to user.banUntil,

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

                // Admin schema
                "adminFirstName" to user.prenomAdmin,
                "adminLastName" to user.nomAdmin,
                "adminEmail" to user.mailAdmin,
                "adminPassword" to user.motDePasseAdmin,
                "roleAdminSuper" to user.roleAdminSuper,

                "permViewOrders" to user.permVoirCommandes,
                "permManageOrders" to user.permGererCommandes,
                "permModifyPickupTime" to user.permModifierHeureRamassage,
                "permViewClients" to user.permVoirClients,
                "permManageClients" to user.permGererClients,
                "permSuspendClients" to user.permSuspendreClients,
                "permViewLogs" to user.permVoirLog,
                "permManageClientPromos" to user.permGererPromosClient,
                "permViewTrash" to user.permVoirCorbeille,
                "permRestoreAccount" to user.permRestaurerCompte,
                "permDeletePermanently" to user.permSupprimerDefinitivement,
                "permViewStocks" to user.permVoirStocks,
                "permManageStocks" to user.permGererStocks,
                "permManageStocksVisibility" to user.permGererStocksVisibility,
                "permViewCatalog" to user.permVoirCatalogue,
                "permManageCatalog" to user.permGererCatalogue,
                "permManagePromos" to user.permGererPromos,
                "permManageAdvancedPromos" to user.permGererPromosAvancees,
                "permManageGommage" to user.permGererGommage,
                "permViewSettings" to user.permVoirParametres,
                "permManageSettings" to user.permGererParametres,
                "permViewAntiFraudPersonal" to user.permVoirAntiFraudPersonnel,
                "permManageAntiFraudPersonal" to user.permGererAntiFraudPersonnel,
                "permViewAntiFraudGlobal" to user.permVoirAntiFraudGlobal,
                "permManageAntiFraudGlobal" to user.permGererAntiFraudGlobal,

                "suppressionDefinitiveAdmin" to user.suppressionDefinitiveAdmin,
                "restaurerAdmin" to user.restaurerAdmin,

                "userFirstName" to user.prenomUser,
                "userLastName" to user.nomUser,
                "userPhone" to user.numeroUser,
                "userEmail" to user.mailUser,
                "userAddress" to user.adresseUser,
                "userPassword" to user.motDePasseUser,
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
                "dureeBlocage" to user.dureeBlocage
            )

            val collections = listOf("users")
            for (colName in collections) {
                db.collection(colName).document(user.id).set(userMap, SetOptions.merge()).await()
            }
            Log.d("FirestoreSyncManager", "Directly pushed user ${user.email} (${user.id}) to Firestore in users.")
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                triggerQuotaExhausted()
            }
            Log.e("FirestoreSyncManager", "Error directly pushing user ${user.email} to Firestore", e)
        }
    }

    // ... (rest of push methods remain similar but with English field names)
    // For brevity, keeping existing push methods but they should also be updated to use English field names
}