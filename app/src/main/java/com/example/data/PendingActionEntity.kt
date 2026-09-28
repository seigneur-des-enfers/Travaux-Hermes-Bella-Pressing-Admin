package com.example.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

/**
 * Entité Room pour la file d'attente des actions hors-ligne (Offline-First / Quota Exhaustion).
 * Permet de différer et de rejouer de manière idempotente toutes les modifications Administrateur
 * vers Firestore dès que le réseau ou le quota est rétabli.
 */
@Keep
@IgnoreExtraProperties
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actionType: String = "",
    val payloadJson: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

object PendingActionTypes {
    const val UPDATE_ORDER = "UPDATE_ORDER"
    const val UPDATE_ORDER_STATUS = "UPDATE_ORDER_STATUS"
    const val UPDATE_ORDER_WEIGHT_PRICE = "UPDATE_ORDER_WEIGHT_PRICE"
    const val ASSIGN_DELIVERY = "ASSIGN_DELIVERY"
    const val DELETE_ORDER = "DELETE_ORDER"
    
    const val CREATE_USER = "CREATE_USER"
    const val UPDATE_USER = "UPDATE_USER"
    const val DELETE_USER = "DELETE_USER"

    const val CREATE_STOCK = "CREATE_STOCK"
    const val UPDATE_STOCK = "UPDATE_STOCK"
    const val DELETE_STOCK = "DELETE_STOCK"

    const val CREATE_PROMO = "CREATE_PROMO"
    const val UPDATE_PROMO = "UPDATE_PROMO"
    const val DELETE_PROMO = "DELETE_PROMO"

    const val CREATE_RULE = "CREATE_RULE"
    const val UPDATE_RULE = "UPDATE_RULE"
    const val DELETE_RULE = "DELETE_RULE"

    const val CREATE_ANNOUNCEMENT = "CREATE_ANNOUNCEMENT"
    const val DELETE_ANNOUNCEMENT = "DELETE_ANNOUNCEMENT"

    const val CREATE_SPECIAL_ITEM = "CREATE_SPECIAL_ITEM"
    const val UPDATE_SPECIAL_ITEM = "UPDATE_SPECIAL_ITEM"
    const val DELETE_SPECIAL_ITEM = "DELETE_SPECIAL_ITEM"

    const val CREATE_GOMMAGE_ITEM = "CREATE_GOMMAGE_ITEM"
    const val UPDATE_GOMMAGE_ITEM = "UPDATE_GOMMAGE_ITEM"
    const val DELETE_GOMMAGE_ITEM = "DELETE_GOMMAGE_ITEM"

    const val UPDATE_CONFIG = "UPDATE_CONFIG"
    const val UPDATE_ANTI_FRAUD = "UPDATE_ANTI_FRAUD"
}
