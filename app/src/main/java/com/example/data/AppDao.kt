package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'CLIENT' AND isArchived = 0 ORDER BY nom ASC")
    fun getAllClients(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'CLIENT' AND isArchived = 1 ORDER BY nom ASC")
    fun getArchivedClients(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role IN ('ADMIN', 'SUPER_ADMIN') AND isArchived = 0 ORDER BY nom ASC")
    fun getAllAdmins(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role IN ('ADMIN', 'SUPER_ADMIN') AND isArchived = 1 ORDER BY nom ASC")
    fun getArchivedAdmins(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)
    @Query("UPDATE users SET isArchived = 1 WHERE id = :id")
    suspend fun archiveUser(id: String)
    @Query("UPDATE users SET isArchived = 0 WHERE id = :id")
    suspend fun restoreUser(id: String)

    @Query("DELETE FROM users WHERE role = 'CLIENT'")
    suspend fun deleteAllClients()

    @androidx.room.Transaction
    suspend fun syncClients(clients: List<UserEntity>) {
        deleteAllClients()
        for (client in clients) {
            insertUser(client)
        }
    }

    @Query("SELECT * FROM orders WHERE (status IN ('EN_ATTENTE', 'EN_COURS', 'PRET', 'ACCEPTE', 'EN_LIVRAISON') OR (orderLivree = 0 AND status NOT IN ('LIVRE', 'TERMINE', 'ANNULE', 'REJETE'))) AND isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status IN ('LIVRE', 'TERMINE', 'ANNULE', 'REJETE') OR orderLivree = 1 OR isArchived = 1 ORDER BY createdAt DESC")
    fun getHistoricalOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId AND isArchived = 0 ORDER BY id DESC")
    fun getOrdersForUser(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId AND isArchived = 0 ORDER BY id DESC")
    suspend fun getOrdersForUserList(userId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE isArchived = 0 ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isArchived = 1 ORDER BY id DESC")
    fun getArchivedOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()

    @Query("UPDATE orders SET isArchived = 1 WHERE id = :id")
    suspend fun archiveOrder(id: String)
    @Query("UPDATE orders SET isArchived = 0 WHERE id = :id")
    suspend fun restoreOrder(id: String)
    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: String)
    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getActivityLogsForUser(userId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM consumables WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllConsumables(): Flow<List<ConsumableEntity>>

    @Query("SELECT * FROM consumables ORDER BY name ASC")
    fun getRawAllConsumables(): Flow<List<ConsumableEntity>>

    @Query("SELECT * FROM consumables WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedConsumables(): Flow<List<ConsumableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumable(consumable: ConsumableEntity)

    @Update
    suspend fun updateConsumable(consumable: ConsumableEntity)
    
    @Query("DELETE FROM consumables WHERE id = :id")
    suspend fun deleteConsumable(id: String)
    @Query("UPDATE consumables SET isArchived = 1 WHERE id = :id")
    suspend fun archiveConsumable(id: String)
    @Query("UPDATE consumables SET isArchived = 0 WHERE id = :id")
    suspend fun restoreConsumable(id: String)
    @Query("DELETE FROM consumables")
    suspend fun deleteAllConsumables()

    @androidx.room.Transaction
    suspend fun syncConsumables(consumables: List<ConsumableEntity>) {
        deleteAllConsumables()
        for (c in consumables) {
            insertConsumable(c)
        }
    }

    @Query("SELECT * FROM config LIMIT 1")
    fun getConfig(): Flow<ConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)

    @Query("SELECT * FROM special_items WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllSpecialItems(): Flow<List<SpecialItemEntity>>

    @Query("SELECT * FROM special_items ORDER BY name ASC")
    fun getRawAllSpecialItems(): Flow<List<SpecialItemEntity>>

    @Query("SELECT * FROM special_items WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedSpecialItems(): Flow<List<SpecialItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecialItem(item: SpecialItemEntity)
    
    @Update
    suspend fun updateSpecialItem(item: SpecialItemEntity)

    @Query("DELETE FROM special_items WHERE id = :id")
    suspend fun deleteSpecialItem(id: String)
    @Query("UPDATE special_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveSpecialItem(id: String)
    @Query("DELETE FROM special_items")
    suspend fun deleteAllSpecialItems()

    @androidx.room.Transaction
    suspend fun syncSpecialItems(items: List<SpecialItemEntity>) {
        deleteAllSpecialItems()
        for (item in items) {
            insertSpecialItem(item)
        }
    }

    @Query("SELECT * FROM gommage_items WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllGommageItems(): Flow<List<GommageItemEntity>>

    @Query("SELECT * FROM gommage_items ORDER BY name ASC")
    fun getRawAllGommageItems(): Flow<List<GommageItemEntity>>

    @Query("SELECT * FROM gommage_items WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedGommageItems(): Flow<List<GommageItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGommageItem(item: GommageItemEntity)
    
    @Update
    suspend fun updateGommageItem(item: GommageItemEntity)

    @Query("DELETE FROM gommage_items WHERE id = :id")
    suspend fun deleteGommageItem(id: String)
    @Query("UPDATE gommage_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveGommageItem(id: String)
    @Query("UPDATE gommage_items SET isArchived = 0 WHERE id = :id")
    suspend fun restoreGommageItem(id: String)
    @Query("UPDATE special_items SET isArchived = 0 WHERE id = :id")
    suspend fun restoreSpecialItem(id: String)
    @Query("DELETE FROM gommage_items")
    suspend fun deleteAllGommageItems()

    @androidx.room.Transaction
    suspend fun syncGommageItems(items: List<GommageItemEntity>) {
        deleteAllGommageItems()
        for (item in items) {
            insertGommageItem(item)
        }
    }

    @Query("SELECT * FROM promos WHERE isArchived = 0 ORDER BY code ASC")
    fun getAllPromos(): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos ORDER BY code ASC")
    fun getRawAllPromos(): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE isArchived = 1 ORDER BY code ASC")
    fun getArchivedPromos(): Flow<List<PromoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromo(promo: PromoEntity)
    
    @Update
    suspend fun updatePromo(promo: PromoEntity)

    @Query("DELETE FROM promos WHERE id = :id")
    suspend fun deletePromo(id: String)
    @Query("UPDATE promos SET isArchived = 1 WHERE id = :id")
    suspend fun archivePromo(id: String)
    @Query("UPDATE promos SET isArchived = 0 WHERE id = :id")
    suspend fun restorePromo(id: String)
    @Query("DELETE FROM promos")
    suspend fun deleteAllPromos()

    @androidx.room.Transaction
    suspend fun syncPromos(promos: List<PromoEntity>) {
        deleteAllPromos()
        for (promo in promos) {
            insertPromo(promo)
        }
    }
    
    @Query("SELECT * FROM rules WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules ORDER BY name ASC")
    fun getRawAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedRules(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)
    
    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRule(id: String)
    @Query("UPDATE rules SET isArchived = 1 WHERE id = :id")
    suspend fun archiveRule(id: String)
    @Query("UPDATE rules SET isArchived = 0 WHERE id = :id")
    suspend fun restoreRule(id: String)
    
    @Query("SELECT * FROM anti_fraud_rules WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllAntiFraudRules(): Flow<List<AntiFraudRuleEntity>>

    @Query("SELECT * FROM anti_fraud_rules ORDER BY name ASC")
    fun getRawAllAntiFraudRules(): Flow<List<AntiFraudRuleEntity>>

    @Query("SELECT * FROM anti_fraud_rules WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedAntiFraudRules(): Flow<List<AntiFraudRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAntiFraudRule(rule: AntiFraudRuleEntity)
    
    @Update
    suspend fun updateAntiFraudRule(rule: AntiFraudRuleEntity)

    @Query("DELETE FROM anti_fraud_rules WHERE id = :id")
    suspend fun deleteAntiFraudRule(id: String)
    @Query("UPDATE anti_fraud_rules SET isArchived = 1 WHERE id = :id")
    suspend fun archiveAntiFraudRule(id: String)
    @Query("UPDATE anti_fraud_rules SET isArchived = 0 WHERE id = :id")
    suspend fun restoreAntiFraudRule(id: String)
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
    
    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    // --- NEW DAO OPERATIONS FOR SCHEMA INTEGRATION ---
    @Query("SELECT * FROM categories_linge_saved ORDER BY createdAt DESC")
    fun getAllCategoriesLingeSaved(): Flow<List<CategoriesLingeSavedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLingeSaved(category: CategoriesLingeSavedEntity)

    @Query("DELETE FROM categories_linge_saved WHERE id = :id")
    suspend fun deleteCategoryLingeSaved(id: String)

    @Query("SELECT * FROM tableau_de_bord LIMIT 1")
    fun getTableauDeBord(): Flow<TableauDeBordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableauDeBord(dashboard: TableauDeBordEntity)

    @Query("SELECT * FROM orders_history LIMIT 1")
    fun getOrdersHistory(): Flow<OrdersHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrdersHistory(history: OrdersHistoryEntity)

    @Query("SELECT * FROM global_trash ORDER BY deletedAt DESC")
    fun getAllGlobalTrash(): Flow<List<GlobalTrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalTrash(trash: GlobalTrashEntity)

    @Query("DELETE FROM global_trash WHERE id = :id")
    suspend fun deleteGlobalTrash(id: String)

    @Query("SELECT * FROM global_trash WHERE id = :id LIMIT 1")
    suspend fun getGlobalTrashItem(id: String): GlobalTrashEntity?

    @Query("SELECT * FROM promos WHERE code = :code LIMIT 1")
    suspend fun getPromoByCode(code: String): PromoEntity?

    @Query("SELECT * FROM special_items WHERE name = :name LIMIT 1")
    suspend fun getSpecialItemByName(name: String): SpecialItemEntity?

    @Query("SELECT * FROM gommage_items WHERE name = :name LIMIT 1")
    suspend fun getGommageItemByName(name: String): GommageItemEntity?

    @Query("SELECT * FROM consumables WHERE name = :name LIMIT 1")
    suspend fun getConsumableByName(name: String): ConsumableEntity?

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM consumables")
    suspend fun getAllConsumablesList(): List<ConsumableEntity>

    @Query("SELECT * FROM special_items")
    suspend fun getAllSpecialItemsList(): List<SpecialItemEntity>

    @Query("SELECT * FROM gommage_items")
    suspend fun getAllGommageItemsList(): List<GommageItemEntity>

    @Query("SELECT * FROM promos")
    suspend fun getAllPromosList(): List<PromoEntity>
}
