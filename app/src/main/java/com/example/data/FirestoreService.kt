package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreService(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    
    // Commandes (Orders)
    fun getPendingOrders() = db.collection("orders")
        .whereEqualTo("status", "EN_ATTENTE")

    suspend fun updateOrderWeightAndPrice(orderId: String, weight: Float, newPrice: Float) {
        val map = mapOf(
            "kg" to weight,
            "price" to newPrice,
            "prixFinal" to newPrice,
            "status" to "TRAITEMENT"
        )
        try { db.collection("orders").document(orderId).update(map).await() } catch (_: Throwable) {}
        try { db.collection("ordres").document(orderId).update(map).await() } catch (_: Throwable) {}
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        try { db.collection("orders").document(orderId).update("status", status).await() } catch (_: Throwable) {}
        try { db.collection("ordres").document(orderId).update("status", status).await() } catch (_: Throwable) {}
    }
}
