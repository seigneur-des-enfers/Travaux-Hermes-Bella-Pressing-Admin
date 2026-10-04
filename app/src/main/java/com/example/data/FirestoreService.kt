package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
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
        try { 
            db.collection("orders").document(orderId).update(map).await() 
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                FirestoreSyncManager.triggerQuotaExhausted()
            }
        }
        try { 
            db.collection("ordres").document(orderId).update(map).await() 
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                FirestoreSyncManager.triggerQuotaExhausted()
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        try { 
            db.collection("orders").document(orderId).update("status", status).await() 
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                FirestoreSyncManager.triggerQuotaExhausted()
            }
        }
        try { 
            db.collection("ordres").document(orderId).update("status", status).await() 
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                FirestoreSyncManager.triggerQuotaExhausted()
            }
        }
    }
}
