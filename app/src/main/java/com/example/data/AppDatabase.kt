package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun safeExec(db: SupportSQLiteDatabase, sql: String) {
    try {
        db.execSQL(sql)
    } catch (_: Throwable) {}
}

// Migration v27 → v28: Add ConfigEntity table with loyalty program fields
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create ConfigEntity table
        safeExec(db, """
            CREATE TABLE IF NOT EXISTS config (
                id TEXT PRIMARY KEY NOT NULL,
                baseTransportFee REAL NOT NULL DEFAULT 1000.0,
                expressMultiplier REAL NOT NULL DEFAULT 1.3,
                fidelityThresholdKg REAL NOT NULL DEFAULT 10.0,
                fidelityRewardValue REAL NOT NULL DEFAULT 350.0,
                pricePerKm REAL NOT NULL DEFAULT 500.0,
                pressingLat REAL NOT NULL DEFAULT 12.6392,
                pressingLng REAL NOT NULL DEFAULT -8.0029,
                referralRewardKg REAL NOT NULL DEFAULT 2.0,
                appDownloadLink TEXT NOT NULL DEFAULT '',
                autoBanEnabled INTEGER NOT NULL DEFAULT 0,
                autoBanThreshold INTEGER NOT NULL DEFAULT 3,
                autoBanDurationHours INTEGER NOT NULL DEFAULT 24,
                isLoyaltyEnabled INTEGER NOT NULL DEFAULT 1,
                loyaltyValidUntil INTEGER,
                isMaintenance INTEGER NOT NULL DEFAULT 0,
                acceptNewOrders INTEGER NOT NULL DEFAULT 1,
                categories TEXT NOT NULL DEFAULT 'Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons',
                businessName TEXT NOT NULL DEFAULT 'Bella Pressing',
                contactPhone TEXT NOT NULL DEFAULT '',
                contactEmail TEXT NOT NULL DEFAULT '',
                currencySymbol TEXT NOT NULL DEFAULT 'FCFA',
                taxPercentage REAL NOT NULL DEFAULT 0.0,
                openingHours TEXT NOT NULL DEFAULT '',
                closingHours TEXT NOT NULL DEFAULT '',
                openingDays TEXT NOT NULL DEFAULT 'Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi',
                closingDays TEXT NOT NULL DEFAULT 'Dimanche',
                termsAndConditionsLink TEXT NOT NULL DEFAULT '',
                tarifBaseTransort REAL NOT NULL DEFAULT 1000.0,
                prixParKilo REAL NOT NULL DEFAULT 350.0,
                nomEntreprise TEXT NOT NULL DEFAULT 'BELLA — PRESSING',
                numeroEntreprise TEXT NOT NULL DEFAULT '',
                emailEntreprise TEXT NOT NULL DEFAULT '',
                openDays TEXT NOT NULL DEFAULT 'Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi',
                openHours TEXT NOT NULL DEFAULT '',
                closeHours TEXT NOT NULL DEFAULT '',
                linkCGU TEXT NOT NULL DEFAULT '',
                isLoyaltyProgramEnabled INTEGER NOT NULL DEFAULT 1,
                loyaltyKgThreshold REAL NOT NULL DEFAULT 10.0,
                loyaltyKgReward REAL NOT NULL DEFAULT 1.0,
                loyaltyMaxFreeKgs REAL NOT NULL DEFAULT 30.0,
                loyaltyExpirationDays INTEGER NOT NULL DEFAULT 90
            )
        """.trimIndent())

        // Insert default config row with Admin's chosen default values
        safeExec(db, """
            INSERT OR IGNORE INTO config (
                id, baseTransportFee, expressMultiplier, fidelityThresholdKg, fidelityRewardValue,
                pricePerKm, pressingLat, pressingLng, referralRewardKg, appDownloadLink,
                autoBanEnabled, autoBanThreshold, autoBanDurationHours, isLoyaltyEnabled,
                loyaltyValidUntil, isMaintenance, acceptNewOrders, categories, businessName,
                contactPhone, contactEmail, currencySymbol, taxPercentage, openingHours,
                closingHours, openingDays, closingDays, termsAndConditionsLink,
                tarifBaseTransort, prixParKilo, nomEntreprise, numeroEntreprise, emailEntreprise,
                openDays, openHours, closeHours, linkCGU,
                isLoyaltyProgramEnabled, loyaltyKgThreshold, loyaltyKgReward, loyaltyMaxFreeKgs, loyaltyExpirationDays
            ) VALUES (
                'global_config', 1000.0, 1.3, 10.0, 350.0,
                500.0, 12.6392, -8.0029, 2.0, '',
                0, 3, 24, 1,
                NULL, 0, 1, 'Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons', 'Bella Pressing',
                '', '', 'FCFA', 0.0, '',
                '', 'Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi', 'Dimanche', '',
                1000.0, 350.0, 'BELLA — PRESSING', '', '',
                'Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi', '', '', '',
                1, 10.0, 1.0, 30.0, 90
            )
        """.trimIndent())
    }
}

// Migration v28 → v29: Add missing columns to UserEntity to match Client (accumulatedFreeKgs, currentKgTowardsReward, lifetimeKgWashed, lastLoyaltyAdjustment fields)
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // UserEntity loyalty fields
        safeExec(db, "ALTER TABLE users ADD COLUMN accumulatedFreeKgs REAL NOT NULL DEFAULT 0.0")
        safeExec(db, "ALTER TABLE users ADD COLUMN currentKgTowardsReward REAL NOT NULL DEFAULT 0.0")
        safeExec(db, "ALTER TABLE users ADD COLUMN lifetimeKgWashed REAL NOT NULL DEFAULT 0.0")
        
        // Last loyalty adjustment tracking
        safeExec(db, "ALTER TABLE users ADD COLUMN lastLoyaltyAdjustmentDelta REAL NOT NULL DEFAULT 0.0")
        safeExec(db, "ALTER TABLE users ADD COLUMN lastLoyaltyAdjustmentNote TEXT NOT NULL DEFAULT ''")
        safeExec(db, "ALTER TABLE users ADD COLUMN lastLoyaltyAdjustmentAt INTEGER NOT NULL DEFAULT 0")
        
        // OrderEntity loyalty fields
        safeExec(db, "ALTER TABLE orders ADD COLUMN isLoyaltyPointsGranted INTEGER NOT NULL DEFAULT 0")
        safeExec(db, "ALTER TABLE orders ADD COLUMN usedFreeKgs REAL NOT NULL DEFAULT 0.0")
    }
}

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
], version = 29, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun orderDao(): OrderDao
    abstract fun pendingActionDao(): PendingActionDao
}