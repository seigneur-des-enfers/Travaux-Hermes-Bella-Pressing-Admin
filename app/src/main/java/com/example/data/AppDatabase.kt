package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    UserEntity::class, 
    OrderEntity::class, 
    ActivityLogEntity::class, 
    ConsumableEntity::class, 
    ConfigEntity::class, 
    SpecialItemEntity::class, 
    GommageItemEntity::class, 
    PromoEntity::class, 
    RuleEntity::class, 
    AntiFraudRuleEntity::class, 
    AnnouncementEntity::class,
    CategoriesLingeSavedEntity::class,
    TableauDeBordEntity::class,
    OrdersHistoryEntity::class,
    GlobalTrashEntity::class,
    PendingActionEntity::class
], version = 27, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun orderDao(): OrderDao
    abstract fun pendingActionDao(): PendingActionDao
}
