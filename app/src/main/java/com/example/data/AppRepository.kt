package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AppRepository(
    private val dao: AppDao,
    private val pendingDao: PendingActionDao? = null
) {
    private val gson = Gson()

    suspend fun fetchClients(): Result<List<UserEntity>> = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = withTimeout(10000L) {
                var snap = db.collection("users")
                    .whereEqualTo("role", "CLIENT")
                    .get()
                    .await()
                if (snap.isEmpty) {
                    snap = db.collection("utilisateurs")
                        .whereEqualTo("role", "CLIENT")
                        .get()
                        .await()
                }
                snap
            }
            val clients = snapshot.documents.map { doc ->
                val nom = doc.getString("nom") ?: doc.getString("nomUser") ?: ""
                val prenom = doc.getString("prenom") ?: doc.getString("prenomUser") ?: ""
                val email = doc.getString("email") ?: doc.getString("mailUser") ?: ""
                val telephone = doc.getString("telephone") ?: doc.getString("numeroUser") ?: ""
                val role = doc.getString("role") ?: "CLIENT"
                val coordonnees = doc.getString("coordonnees") ?: doc.getString("adresseUser") ?: ""
                val accumulatedFreeKgs = (doc.get("accumulatedFreeKgs") as? Number)?.toDouble() ?: (doc.get("kilosGratuitsDisponibles") as? Number)?.toDouble() ?: 0.0
                val lifetimeKgWashed = (doc.get("lifetimeKgWashed") as? Number)?.toDouble() ?: 0.0
                val currentKgTowardsReward = (doc.get("currentKgTowardsReward") as? Number)?.toDouble() ?: 0.0
                val isLoyaltyEnabled = doc.getBoolean("isLoyaltyEnabled") ?: true

                UserEntity(
                    id = doc.id,
                    role = role,
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    telephone = telephone,
                    motDePasse = doc.getString("motDePasse") ?: "",
                    coordonnees = coordonnees,
                    accumulatedFreeKgs = accumulatedFreeKgs,
                    lifetimeKgWashed = lifetimeKgWashed,
                    currentKgTowardsReward = currentKgTowardsReward,
                    isLoyaltyEnabled = isLoyaltyEnabled,
                    fidelityPointsKg = (doc.get("fidelityPointsKg") as? Number)?.toFloat() ?: 0f,
                    isArchived = doc.getBoolean("isArchived") ?: false
                )
            }
            dao.syncClients(clients)
            Result.success(clients)
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.e("AppRepository", "Index composite Firestore requis pour les clients ! Créez l'index via ce lien : ${e.message}", e)
                } else if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    FirestoreSyncManager.triggerQuotaExhausted()
                }
            }
            Log.e("AppRepository", "Erreur fetchClients: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // 1. Articles speciaux et Gommage (catalog / catalogue)
            val catalogSnap = withTimeout(10000L) {
                var snap = db.collection("catalog").get().await()
                if (snap.isEmpty) {
                    snap = db.collection("catalogue").get().await()
                }
                snap
            }
            val specialItems = mutableListOf<SpecialItemEntity>()
            val gommageItems = mutableListOf<GommageItemEntity>()
            for (doc in catalogSnap.documents) {
                val type = doc.getString("type") ?: ""
                val isArchived = doc.getBoolean("isArchived") ?: false
                val name = doc.getString("name") ?: ""
                val price = (doc.get("price") as? Number)?.toFloat() ?: 0f

                if (type.equals("GOMMAGE", ignoreCase = true) || doc.id.startsWith("gom_", ignoreCase = true)) {
                    gommageItems.add(GommageItemEntity(id = doc.id, name = name, price = price, isArchived = isArchived))
                } else {
                    specialItems.add(SpecialItemEntity(id = doc.id, name = name, price = price, isArchived = isArchived))
                }
            }
            dao.syncSpecialItems(specialItems)
            dao.syncGommageItems(gommageItems)

            // 2. Promotions (promotions / promos)
            val promoSnap = withTimeout(10000L) {
                var snap = db.collection("promotions").get().await()
                if (snap.isEmpty) {
                    snap = db.collection("promos").get().await()
                }
                snap
            }
            val promos = promoSnap.documents.map { doc ->
                PromoEntity(
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
            }
            dao.syncPromos(promos)

            // 3. Consommables / Stocks (stocks / stock)
            val stockSnap = withTimeout(10000L) {
                var snap = db.collection("stocks").get().await()
                if (snap.isEmpty) {
                    snap = db.collection("stock").get().await()
                }
                snap
            }
            val consumables = stockSnap.documents.map { doc ->
                val docName = doc.getString("name")?.takeIf { it.isNotEmpty() } ?: doc.getString("nomProduit") ?: ""
                val docQuantity = (doc.get("quantity") as? Number)?.toInt()
                    ?: (doc.get("quantiterProduit") as? Number)?.toInt() ?: 0
                val docThreshold = (doc.get("threshold") as? Number)?.toInt()
                    ?: (doc.get("produitSeilleAlert") as? Number)?.toInt() ?: 0
                val docIsPublic = doc.getBoolean("isPublic") ?: doc.getBoolean("newStocksPublic") ?: false
                val docIsPrivee = doc.getBoolean("newStocksPrivee") ?: !docIsPublic

                ConsumableEntity(
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
            }
            dao.syncConsumables(consumables)

            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.e("AppRepository", "Index composite Firestore requis pour le catalogue ! Créez l'index via ce lien : ${e.message}", e)
                } else if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    FirestoreSyncManager.triggerQuotaExhausted()
                }
            }
            Log.e("AppRepository", "Erreur fetchCatalog: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchSettings(): Result<ConfigEntity> = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = withTimeout(10000L) {
                var d = db.collection("config").document("global_config").get().await()
                if (!d.exists()) {
                    d = db.collection("config").document("main").get().await()
                }
                if (!d.exists()) {
                    d = db.collection("general_settings").document("main").get().await()
                }
                d
            }
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Document de configuration introuvable"))
            }

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

            dao.insertConfig(configEntity)
            Result.success(configEntity)
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.e("AppRepository", "Index composite Firestore requis pour les paramètres ! Créez l'index via ce lien : ${e.message}", e)
                } else if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    FirestoreSyncManager.triggerQuotaExhausted()
                }
            }
            Log.e("AppRepository", "Erreur fetchSettings: ${e.message}", e)
            Result.failure(e)
        }
    }
    fun getPendingActionsFlow(): Flow<List<PendingActionEntity>> = pendingDao?.getPendingActionsFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getPendingActionsCountFlow(): Flow<Int> = pendingDao?.getPendingActionCountFlow() ?: kotlinx.coroutines.flow.flowOf(0)
    suspend fun insertPendingAction(action: PendingActionEntity) { pendingDao?.insertPendingAction(action) }
    suspend fun deletePendingAction(id: String) { pendingDao?.deletePendingAction(id) }
    suspend fun clearPendingActions() { pendingDao?.clearAllPendingActions() }

    suspend fun getUserByEmail(email: String): UserEntity? = dao.getUserByEmail(email)
    fun getUserByIdFlow(id: String): Flow<UserEntity?> = dao.getUserByIdFlow(id)
    suspend fun getUserById(id: String): UserEntity? = dao.getUserById(id)
    fun getAllClients(): Flow<List<UserEntity>> = dao.getAllClients()
    fun getAllAdmins(): Flow<List<UserEntity>> = dao.getAllAdmins()
    suspend fun insertUser(user: UserEntity) {
        val existing = dao.getUserByEmail(user.email)
        if (existing != null && existing.id != user.id) {
            dao.deleteUser(existing.id)
        }
        dao.insertUser(user)
    }
    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    /**
     * Sauvegarde résiliente Offline-First d'un utilisateur (mise à jour du solde fidélité, profil, etc.).
     * 1. Met à jour la table Room (SSOT instantané).
     * 2. Sérialise l'objet UserDto.
     * 3. Insère une PendingActionEntity avec actionType = UPDATE_USER.
     * 4. Déclenche le QuotaRetryWorker pour la synchronisation asynchrone Firestore.
     */
    suspend fun saveUserResilient(user: UserEntity, context: Context? = null) {
        // 1. Room SSOT
        dao.updateUser(user)

        // 2. Sérialisation DTO
        val userDto = user.toDto()
        val payloadJson = gson.toJson(userDto)

        // 3. PendingAction
        val pendingAction = PendingActionEntity(
            id = "user_${user.id}_${System.currentTimeMillis()}",
            actionType = PendingActionTypes.UPDATE_USER,
            payloadJson = payloadJson,
            timestamp = System.currentTimeMillis()
        )

        // 4. Persistence & Worker
        pendingDao?.insertPendingAction(pendingAction)
        val ctx = context ?: try { com.example.di.Graph.appContext } catch (_: Throwable) { null }
        ctx?.let { QuotaRetryWorker.enqueueWork(it) }
    }

    suspend fun deleteUser(id: String) = dao.deleteUser(id)
    
    fun getOrdersForUser(userId: String): Flow<List<OrderEntity>> = dao.getOrdersForUser(userId)
    suspend fun getOrdersForUserList(userId: String): List<OrderEntity> = dao.getOrdersForUserList(userId)
    fun getActiveOrders(): Flow<List<OrderEntity>> = dao.getActiveOrders()
    fun getHistoricalOrders(): Flow<List<OrderEntity>> = dao.getHistoricalOrders()
    fun getAllOrders(): Flow<List<OrderEntity>> = dao.getAllOrders()
    suspend fun insertOrder(order: OrderEntity) = dao.insertOrder(order)
    suspend fun insertOrders(orders: List<OrderEntity>) = dao.insertOrders(orders)
    suspend fun updateOrder(order: OrderEntity) = dao.updateOrder(order)
    suspend fun deleteOrder(id: String) = dao.deleteOrderById(id)
    suspend fun deleteOrderById(id: String) = dao.deleteOrderById(id)
    suspend fun clearOrders() = dao.clearAllOrders()
    suspend fun clearAllOrders() = dao.clearAllOrders()
    
    fun getActivityLogsForUser(userId: String): Flow<List<ActivityLogEntity>> = dao.getActivityLogsForUser(userId)
    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>> = dao.getAllActivityLogs()
    suspend fun insertActivityLog(log: ActivityLogEntity) = dao.insertActivityLog(log)
    
    fun getAllConsumables(): Flow<List<ConsumableEntity>> = dao.getAllConsumables()
    fun getRawAllConsumables(): Flow<List<ConsumableEntity>> = dao.getRawAllConsumables()
    suspend fun insertConsumable(consumable: ConsumableEntity) {
        val updated = consumable.copy(
            nomProduit = consumable.name,
            quantiterProduit = consumable.quantity.toDouble(),
            produitSeilleAlert = consumable.threshold.toDouble(),
            newStocksPublic = consumable.isPublic,
            newStocksPrivee = !consumable.isPublic
        )
        dao.insertConsumable(updated)
    }
    suspend fun updateConsumable(consumable: ConsumableEntity) {
        val updated = consumable.copy(
            nomProduit = consumable.name,
            quantiterProduit = consumable.quantity.toDouble(),
            produitSeilleAlert = consumable.threshold.toDouble(),
            newStocksPublic = consumable.isPublic,
            newStocksPrivee = !consumable.isPublic
        )
        dao.insertConsumable(updated)
    }
    suspend fun deleteConsumable(id: String) = dao.deleteConsumable(id)

    fun getConfig(): Flow<ConfigEntity?> = dao.getConfig()
    suspend fun insertConfig(config: ConfigEntity) = dao.insertConfig(config)

    /**
     * Enregistrement résilient (Offline-First) de la configuration et du programme de fidélité.
     * 1. Met à jour la table locale ConfigEntity instantanément (SSOT).
     * 2. Sérialise l'objet ConfigDto complet mis à jour.
     * 3. Enregistre une action dans PendingActionEntity avec actionType = "UPDATE_CONFIG".
     * 4. Déclenche le QuotaRetryWorker pour la synchronisation différée.
     */
    suspend fun saveConfigResilient(config: ConfigEntity, context: Context? = null) {
        // 1. Mise à jour Room SSOT
        dao.insertConfig(config)

        // 2. Sérialisation ConfigDto
        val configDto = config.toDto()
        val payloadJson = gson.toJson(configDto)

        // 3. PendingActionEntity
        val pendingAction = PendingActionEntity(
            id = "config_${System.currentTimeMillis()}",
            actionType = PendingActionTypes.UPDATE_CONFIG,
            payloadJson = payloadJson,
            timestamp = System.currentTimeMillis()
        )

        // 4. Persistence de l'action & WorkManager
        pendingDao?.insertPendingAction(pendingAction)
        val ctx = context ?: try { com.example.di.Graph.appContext } catch (_: Throwable) { null }
        ctx?.let { QuotaRetryWorker.enqueueWork(it) }
    }

    fun getAllSpecialItems(): Flow<List<SpecialItemEntity>> = dao.getAllSpecialItems()
    fun getRawAllSpecialItems(): Flow<List<SpecialItemEntity>> = dao.getRawAllSpecialItems()
    suspend fun insertSpecialItem(item: SpecialItemEntity) {
        dao.insertSpecialItem(item)
    }
    suspend fun updateSpecialItem(item: SpecialItemEntity) {
        dao.insertSpecialItem(item)
    }
    suspend fun deleteSpecialItem(id: String) = dao.deleteSpecialItem(id)

    fun getAllGommageItems(): Flow<List<GommageItemEntity>> = dao.getAllGommageItems()
    fun getRawAllGommageItems(): Flow<List<GommageItemEntity>> = dao.getRawAllGommageItems()
    suspend fun insertGommageItem(item: GommageItemEntity) {
        dao.insertGommageItem(item)
    }
    suspend fun updateGommageItem(item: GommageItemEntity) {
        dao.insertGommageItem(item)
    }
    suspend fun deleteGommageItem(id: String) = dao.deleteGommageItem(id)

    fun getAllPromos(): Flow<List<PromoEntity>> = dao.getAllPromos()
    fun getRawAllPromos(): Flow<List<PromoEntity>> = dao.getRawAllPromos()
    suspend fun insertPromo(promo: PromoEntity) {
        val existing = dao.getPromoByCode(promo.code)
        if (existing != null && existing.id != promo.id) {
            dao.deletePromo(existing.id)
        }
        dao.insertPromo(promo)
    }
    suspend fun updatePromo(promo: PromoEntity) = dao.updatePromo(promo)
    suspend fun deletePromo(id: String) = dao.deletePromo(id)
    
    fun getAllRules(): Flow<List<RuleEntity>> = dao.getAllRules()
    fun getRawAllRules(): Flow<List<RuleEntity>> = dao.getRawAllRules()
    suspend fun insertRule(rule: RuleEntity) = dao.insertRule(rule)
    suspend fun updateRule(rule: RuleEntity) = dao.updateRule(rule)
    suspend fun deleteRule(id: String) = dao.deleteRule(id)
    
    fun getAllAntiFraudRules(): Flow<List<AntiFraudRuleEntity>> = dao.getAllAntiFraudRules()
    fun getRawAllAntiFraudRules(): Flow<List<AntiFraudRuleEntity>> = dao.getRawAllAntiFraudRules()
    suspend fun insertAntiFraudRule(rule: AntiFraudRuleEntity) = dao.insertAntiFraudRule(rule)
    suspend fun updateAntiFraudRule(rule: AntiFraudRuleEntity) = dao.updateAntiFraudRule(rule)
    suspend fun deleteAntiFraudRule(id: String) = dao.deleteAntiFraudRule(id)

    fun getArchivedClients(): Flow<List<UserEntity>> = dao.getArchivedClients()
    fun getArchivedAdmins(): Flow<List<UserEntity>> = dao.getArchivedAdmins()
    fun getArchivedOrders(): Flow<List<OrderEntity>> = dao.getArchivedOrders()
    fun getArchivedConsumables(): Flow<List<ConsumableEntity>> = dao.getArchivedConsumables()
    fun getArchivedSpecialItems(): Flow<List<SpecialItemEntity>> = dao.getArchivedSpecialItems()
    fun getArchivedGommageItems(): Flow<List<GommageItemEntity>> = dao.getArchivedGommageItems()
    fun getArchivedPromos(): Flow<List<PromoEntity>> = dao.getArchivedPromos()
    fun getArchivedRules(): Flow<List<RuleEntity>> = dao.getArchivedRules()
    fun getArchivedAntiFraudRules(): Flow<List<AntiFraudRuleEntity>> = dao.getArchivedAntiFraudRules()

    suspend fun archiveRule(id: String) = dao.archiveRule(id)
    suspend fun restoreRule(id: String) = dao.restoreRule(id)
    suspend fun archiveAntiFraudRule(id: String) = dao.archiveAntiFraudRule(id)
    suspend fun restoreAntiFraudRule(id: String) = dao.restoreAntiFraudRule(id)
    suspend fun archiveSpecialItem(id: String) = dao.archiveSpecialItem(id)
    suspend fun restoreSpecialItem(id: String) = dao.restoreSpecialItem(id)
    suspend fun archiveGommageItem(id: String) = dao.archiveGommageItem(id)
    suspend fun restoreGommageItem(id: String) = dao.restoreGommageItem(id)
    suspend fun archivePromo(id: String) = dao.archivePromo(id)
    suspend fun restorePromo(id: String) = dao.restorePromo(id)
    suspend fun archiveConsumable(id: String) = dao.archiveConsumable(id)
    suspend fun restoreConsumable(id: String) = dao.restoreConsumable(id)
    suspend fun archiveOrder(id: String) = dao.archiveOrder(id)
    suspend fun restoreOrder(id: String) = dao.restoreOrder(id)
    suspend fun archiveUser(id: String) = dao.archiveUser(id)
    suspend fun restoreUser(id: String) = dao.restoreUser(id)
    val allAnnouncements: kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>> = dao.getAllAnnouncements()
    
    suspend fun insertAnnouncement(announcement: AnnouncementEntity) = dao.insertAnnouncement(announcement)
    suspend fun deleteAnnouncement(id: String) = dao.deleteAnnouncement(id)

    suspend fun fetchAnnouncements(): Result<List<AnnouncementEntity>> = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val collections = listOf("announcements", "annonces")
            val fetchedList = mutableListOf<AnnouncementEntity>()

            for (col in collections) {
                try {
                    val snapshot = withTimeout(10000L) {
                        db.collection(col).get().await()
                    }
                    for (doc in snapshot.documents) {
                        val title = doc.getString("title")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("nomAnnoce")
                            ?: doc.getString("titre") ?: ""
                        val message = doc.getString("message")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("descriptionAnnoce")
                            ?: doc.getString("texte") ?: ""
                        val imageUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("fichiersMediaAnnoce")
                            ?: doc.getString("image") ?: ""
                        val isVisible = doc.getBoolean("isVisible") ?: doc.getBoolean("visible") ?: true
                        val createdAt = doc.getLong("createdAt") ?: doc.getLong("dateCreation") ?: System.currentTimeMillis()

                        fetchedList.add(
                            AnnouncementEntity(
                                id = doc.id,
                                title = title,
                                message = message,
                                imageUrl = imageUrl,
                                isVisible = isVisible,
                                createdAt = createdAt,
                                nomAnnoce = title,
                                descriptionAnnoce = message,
                                fichiersMediaAnnoce = imageUrl
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("AppRepository", "fetchAnnouncements warning sur $col: ${e.message}")
                }
            }

            val distinctList = fetchedList.distinctBy { it.id }
            if (distinctList.isNotEmpty()) {
                dao.syncAnnouncements(distinctList)
            }
            Result.success(distinctList)
        } catch (e: Throwable) {
            Log.e("AppRepository", "Erreur fetchAnnouncements: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchPromos(): Result<List<PromoEntity>> = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val promoSnap = withTimeout(10000L) {
                var snap = db.collection("promotions").get().await()
                if (snap.isEmpty) {
                    snap = db.collection("promos").get().await()
                }
                snap
            }
            val promos = promoSnap.documents.map { doc ->
                PromoEntity(
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
            }
            dao.syncPromos(promos)
            Result.success(promos)
        } catch (e: Throwable) {
            Log.e("AppRepository", "Erreur fetchPromos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- NEW REPOSITORY OPERATIONS ---
    fun getAllCategoriesLingeSaved(): Flow<List<CategoriesLingeSavedEntity>> = dao.getAllCategoriesLingeSaved()
    suspend fun insertCategoryLingeSaved(category: CategoriesLingeSavedEntity) = dao.insertCategoryLingeSaved(category)
    suspend fun deleteCategoryLingeSaved(id: String) = dao.deleteCategoryLingeSaved(id)

    fun getTableauDeBord(): Flow<TableauDeBordEntity?> = dao.getTableauDeBord()
    suspend fun insertTableauDeBord(dashboard: TableauDeBordEntity) = dao.insertTableauDeBord(dashboard)

    fun getOrdersHistory(): Flow<OrdersHistoryEntity?> = dao.getOrdersHistory()
    suspend fun insertOrdersHistory(history: OrdersHistoryEntity) = dao.insertOrdersHistory(history)

    fun getAllGlobalTrash(): Flow<List<GlobalTrashEntity>> = dao.getAllGlobalTrash()
    suspend fun insertGlobalTrash(trash: GlobalTrashEntity) = dao.insertGlobalTrash(trash)
    suspend fun deleteGlobalTrash(id: String) = dao.deleteGlobalTrash(id)
    suspend fun getGlobalTrashItem(id: String): GlobalTrashEntity? = dao.getGlobalTrashItem(id)

    suspend fun cleanupDuplicates() {
        // 1. Clean Users by Email (case-insensitive)
        val users = dao.getAllUsersList()
        val usersByEmail = users.groupBy { it.email.trim().lowercase() }
        for ((email, list) in usersByEmail) {
            if (list.size > 1) {
                val sorted = list.sortedWith(compareByDescending<UserEntity> { 
                    when (it.role) {
                        "SUPER_ADMIN" -> 3
                        "ADMIN" -> 2
                        else -> 1
                    }
                }.thenByDescending { it.id.startsWith("DEFAULT_") })
                
                val toKeep = sorted.first()
                for (user in sorted.drop(1)) {
                    dao.deleteUser(user.id)
                }
            }
        }

        // 2. Clean Consumables by Name (case-insensitive)
        val consumables = dao.getAllConsumablesList()
        val consumablesByName = consumables.groupBy { it.name.trim().lowercase() }
        for ((name, list) in consumablesByName) {
            if (list.size > 1) {
                val sorted = list.sortedBy { it.id }
                val toKeep = sorted.first()
                for (c in sorted.drop(1)) {
                    dao.deleteConsumable(c.id)
                }
            }
        }

        // 3. Clean Special Items by Name
        val specialItems = dao.getAllSpecialItemsList()
        val specialsByName = specialItems.groupBy { it.name.trim().lowercase() }
        for ((name, list) in specialsByName) {
            if (list.size > 1) {
                val sorted = list.sortedBy { it.id }
                for (item in sorted.drop(1)) {
                    dao.deleteSpecialItem(item.id)
                }
            }
        }

        // 4. Clean Gommage Items by Name
        val gommageItems = dao.getAllGommageItemsList()
        val gommagesByName = gommageItems.groupBy { it.name.trim().lowercase() }
        for ((name, list) in gommagesByName) {
            if (list.size > 1) {
                val sorted = list.sortedBy { it.id }
                for (item in sorted.drop(1)) {
                    dao.deleteGommageItem(item.id)
                }
            }
        }

        // 5. Clean Promos by Code
        val promos = dao.getAllPromosList()
        val promosByCode = promos.groupBy { it.code.trim().uppercase() }
        for ((code, list) in promosByCode) {
            if (list.size > 1) {
                val sorted = list.sortedBy { it.id }
                for (promo in sorted.drop(1)) {
                    dao.deletePromo(promo.id)
                }
            }
        }
    }
}
