package com.example.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val dao: AppDao,
    private val pendingDao: PendingActionDao? = null
) {
    private val gson = Gson()
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
