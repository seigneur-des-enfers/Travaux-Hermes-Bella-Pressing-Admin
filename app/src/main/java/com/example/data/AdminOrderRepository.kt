package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repository spécialisé pour la gestion des commandes par l'Administration et les Livreurs.
 * Architecture Offline-First avec Room (OrderDao) comme Single Source of Truth (SSOT).
 * Protection des quotas Firebase Spark Plan par détection Quota/Réseau (RESOURCE_EXHAUSTED, UNAVAILABLE),
 * gestion d'une file d'attente locale (PendingActionDao) et traitement différé via WorkManager.
 */
class AdminOrderRepository(
    private val orderDao: OrderDao,
    private val pendingActionDao: PendingActionDao,
    private val appContext: Context? = null,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    companion object {
        private const val TAG = "AdminOrderRepository"
        private const val CLOUDINARY_CLOUD_NAME = "dvh5vzg4m"
        private const val CLOUDINARY_PRESET = "bella_pressing_preset"
        private const val CLOUDINARY_FOLDER = "bella_pressing"
        private const val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"
    }

    // Référence active du listener Firestore
    private var activeOrdersListener: ListenerRegistration? = null

    // État réactif de saturation de quota / coupure réseau
    private val _isQuotaExceeded = MutableStateFlow(false)
    val isQuotaExceeded: StateFlow<Boolean> = _isQuotaExceeded.asStateFlow()

    // Nombre d'actions en file d'attente de synchronisation
    val pendingActionsCount: Flow<Int> = pendingActionDao.getPendingActionCountFlow()

    // =========================================================================
    // 1. FLUX RÉACTIFS ROOM (SSOT EXCLUSIF POUR L'INTERFACE UTILISATEUR)
    // =========================================================================

    /**
     * Expose le flux des commandes actives (En attente, En cours, Prêt, etc.)
     * RÈGLE ABSOLUE : Les Flows Room ne sont jamais coupés en cas de saturation quota/réseau.
     */
    fun getActiveOrders(): Flow<List<OrderEntity>> = orderDao.getActiveOrders()

    /**
     * Expose le flux des commandes archivées / clôturées (Historique)
     */
    fun getHistoricalOrders(): Flow<List<OrderEntity>> = orderDao.getHistoricalOrders()

    /**
     * Expose toutes les commandes non archivées
     */
    fun getAllOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrders()

    /**
     * Observe une commande unique par son identifiant
     */
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?> = orderDao.getOrderByIdFlow(orderId)

    /**
     * Récupération ponctuelle locale d'une commande
     */
    suspend fun getOrderById(orderId: String): OrderEntity? = withContext(Dispatchers.IO) {
        orderDao.getOrderById(orderId)
    }

    // =========================================================================
    // 2. SYNCHRONISATION TEMPS RÉEL AVEC PROTECTION QUOTA & RÉSEAU
    // =========================================================================

    /**
     * Écoute en temps réel UNIQUEMENT les commandes actives pour préserver les quotas Spark.
     * En cas d'erreur RESOURCE_EXHAUSTED ou UNAVAILABLE, coupe immédiatement le listener
     * sans détruire les Flows Room.
     */
    @Synchronized
    fun syncActiveOrdersRealtime(): ListenerRegistration? {
        if (activeOrdersListener != null) return activeOrdersListener

        val activeStatuses = listOf("EN_ATTENTE", "EN_COURS", "PRET", "ACCEPTE", "EN_LIVRAISON")

        activeOrdersListener = db.collection("orders")
            .whereIn("status", activeStatuses)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ||
                        error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
                    ) {
                        Log.w(TAG, "Quota Firestore épuisé (${error.code}). Arrêt de l'écouteur pour protéger le Spark Plan.")
                        activeOrdersListener?.remove()
                        activeOrdersListener = null
                        _isQuotaExceeded.value = true

                        // Planification du Worker pour traitement différé automatique
                        appContext?.let { QuotaRetryWorker.enqueueWork(it) }
                        return@addSnapshotListener
                    }
                    Log.e(TAG, "Erreur écoute temps réel des commandes actives: ${error.message}", error)
                    return@addSnapshotListener
                }

                // Réseau actif et données valides
                _isQuotaExceeded.value = false

                // SÉCURITÉ 1 : Ne rien faire si le snapshot est null ou vide
                if (snapshot == null || snapshot.isEmpty) {
                    return@addSnapshotListener
                }

                // SÉCURITÉ 2 : Ignore les événements locaux pour éviter les boucles d'actualisation UI
                if (snapshot.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }

                // Traitement asynchrone sur Dispatchers.IO
                ioScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val orderEntity = OrderMapper.fromDocument(doc)
                                    orderDao.insertOrder(orderEntity)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    orderDao.deleteOrderById(doc.id)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Erreur insertion Room des commandes actives: ${e.message}", e)
                    }
                }
            }

        return activeOrdersListener
    }

    /**
     * Stoppe le listener actif pour libérer les ressources.
     */
    @Synchronized
    fun stopRealtimeSync() {
        activeOrdersListener?.remove()
        activeOrdersListener = null
    }

    /**
     * Notification globale de dépassement de quota ou coupure réseau.
     */
    fun notifyQuotaOrNetworkExhausted() {
        Log.w(TAG, "Notification globale quota/réseau épuisé. Coupure des écouteurs.")
        _isQuotaExceeded.value = true
        stopRealtimeSync()
        appContext?.let { QuotaRetryWorker.enqueueWork(it) }
    }

    /**
     * Réinitialise le mode dégradé, reconnecte les listeners et déclenche le WorkManager immédiatement.
     */
    fun retryConnection(context: Context? = appContext) {
        Log.d(TAG, "Tentative de reconnexion / réinitialisation quota demandée...")
        _isQuotaExceeded.value = false
        syncActiveOrdersRealtime()
        context?.let {
            QuotaRetryWorker.triggerImmediate(it)
        }
    }

    // =========================================================================
    // 3. HISTORIQUE PONCTUEL SANS LISTENER PERMANENT
    // =========================================================================

    /**
     * Récupère l'historique ponctuel sans écouteur permanent.
     */
    suspend fun fetchHistoricalOrders(limit: Int = 50): Result<List<OrderEntity>> = withContext(Dispatchers.IO) {
        if (_isQuotaExceeded.value) {
            return@withContext Result.failure(Exception("Mode dégradé actif : consultation des commandes historiques locales."))
        }

        try {
            val historicalStatuses = listOf("LIVRE", "TERMINE", "ANNULE", "REJETE")

            val snapshot = withTimeout(10000L) {
                db.collection("orders")
                    .whereIn("status", historicalStatuses)
                    .limit(limit.toLong())
                    .get()
                    .await()
            }

            val entities = snapshot.documents.map { OrderMapper.fromDocument(it) }
            if (entities.isNotEmpty()) {
                orderDao.insertOrders(entities)
            }

            _isQuotaExceeded.value = false
            Result.success(entities)
        } catch (e: Throwable) {
            if (e is FirebaseFirestoreException && (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE)) {
                _isQuotaExceeded.value = true
                stopRealtimeSync()
                appContext?.let { QuotaRetryWorker.enqueueWork(it) }
            }
            Log.e(TAG, "Erreur lors de la récupération de l'historique: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. ACTIONS D'ÉCRITURE OFFLINE-FIRST AVEC FILE D'ATTENTE IDEMPOTENTE
    // =========================================================================

    /**
     * Met à jour le statut d'une commande :
     * 1. Met à jour Room localement de manière immédiate (SSOT UI).
     * 2. Enregistre l'action dans PendingActionDao.
     * 3. Tente l'envoi direct si le quota n'est pas saturé.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        adminNotes: String? = null,
        deliveryBoyId: String? = null,
        context: Context? = appContext
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Mise à jour locale immédiate dans Room
        val current = orderDao.getOrderById(orderId)
        if (current != null) {
            val updated = current.copy(
                status = newStatus,
                orderRecu = (newStatus in listOf("EN_COURS", "PRET", "LIVRE", "TERMINE", "ACCEPTE")),
                enPleinLavage = (newStatus == "EN_COURS"),
                orderPret = (newStatus == "PRET"),
                orderLivree = (newStatus in listOf("LIVRE", "TERMINE")),
                adminNotes = adminNotes ?: current.adminNotes,
                adminMessage = adminNotes ?: current.adminMessage,
                deliveryBoyId = deliveryBoyId ?: current.deliveryBoyId
            )
            orderDao.insertOrder(updated)
        }

        // 2. Préparation du payload et mise en file d'attente locale
        val updates = hashMapOf<String, Any?>(
            "id" to orderId,
            "status" to newStatus,
            "statut" to newStatus,
            "orderRecu" to (newStatus in listOf("EN_COURS", "PRET", "LIVRE", "TERMINE", "ACCEPTE")),
            "enPleinLavage" to (newStatus == "EN_COURS"),
            "orderPret" to (newStatus == "PRET"),
            "orderLivree" to (newStatus in listOf("LIVRE", "TERMINE")),
            "updatedAt" to System.currentTimeMillis()
        )
        if (adminNotes != null) {
            updates["adminNotes"] = adminNotes
            updates["adminMessage"] = adminNotes
        }
        if (deliveryBoyId != null) {
            updates["deliveryBoyId"] = deliveryBoyId
            updates["livreurId"] = deliveryBoyId
        }

        val actionId = UUID.randomUUID().toString()
        val pendingAction = PendingActionEntity(
            id = actionId,
            actionType = PendingActionTypes.UPDATE_ORDER_STATUS,
            payloadJson = gson.toJson(updates),
            timestamp = System.currentTimeMillis()
        )
        pendingActionDao.insertPendingAction(pendingAction)

        // 3. Si quota dépassé, on délègue au WorkManager et on confirme localement
        if (_isQuotaExceeded.value) {
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            return@withContext Result.success(Unit)
        }

        // 4. Tentative de synchronisation directe vers Firestore
        try {
            val cleanUpdates = updates.filterValues { it != null }
            withTimeout(5000L) {
                db.collection("orders").document(orderId).set(cleanUpdates, SetOptions.merge()).await()
                try {
                    db.collection("ordres").document(orderId).set(cleanUpdates, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }
            // Succès réseau immédiat -> suppression de la file d'attente
            pendingActionDao.deletePendingAction(actionId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.w(TAG, "Échec mise à jour directe Firestore ($orderId). Conservé en file locale: ${e.message}")
            if (e is FirebaseFirestoreException && (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE)) {
                _isQuotaExceeded.value = true
                stopRealtimeSync()
            }
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            // Retourne success car la modification est déjà enregistrée en local et en file d'attente
            Result.success(Unit)
        }
    }

    /**
     * Enregistre le poids et calcule le prix final :
     * 1. Mise à jour Room instantanée
     * 2. Enregistrement PendingActionEntity
     * 3. Tentative Firestore directe ou synchronisation différée
     */
    suspend fun updateOrderWeightAndPrice(
        orderId: String,
        weightKg: Double,
        totalPrice: Double,
        adminNotes: String? = null,
        context: Context? = appContext
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Mise à jour locale Room
        val current = orderDao.getOrderById(orderId)
        if (current != null) {
            val updated = current.copy(
                weight = weightKg,
                kg = weightKg.toFloat(),
                totalPrice = totalPrice,
                prixFinal = totalPrice.toFloat(),
                timelineStep = 1,
                adminNotes = adminNotes ?: current.adminNotes,
                adminMessage = adminNotes ?: current.adminMessage
            )
            orderDao.insertOrder(updated)
        }

        // 2. Enregistrement file d'attente
        val updates = hashMapOf<String, Any?>(
            "id" to orderId,
            "weight" to weightKg,
            "kg" to weightKg,
            "totalPrice" to totalPrice,
            "prixFinal" to totalPrice,
            "timelineStep" to 1,
            "updatedAt" to System.currentTimeMillis()
        )
        if (adminNotes != null) {
            updates["adminNotes"] = adminNotes
            updates["adminMessage"] = adminNotes
        }

        val actionId = UUID.randomUUID().toString()
        val pendingAction = PendingActionEntity(
            id = actionId,
            actionType = PendingActionTypes.UPDATE_ORDER_WEIGHT_PRICE,
            payloadJson = gson.toJson(updates),
            timestamp = System.currentTimeMillis()
        )
        pendingActionDao.insertPendingAction(pendingAction)

        if (_isQuotaExceeded.value) {
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            return@withContext Result.success(Unit)
        }

        // 3. Tentative Firestore directe
        try {
            val cleanUpdates = updates.filterValues { it != null }
            withTimeout(5000L) {
                db.collection("orders").document(orderId).set(cleanUpdates, SetOptions.merge()).await()
                try {
                    db.collection("ordres").document(orderId).set(cleanUpdates, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }
            pendingActionDao.deletePendingAction(actionId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.w(TAG, "Échec mise à jour poids directe ($orderId). En file locale: ${e.message}")
            if (e is FirebaseFirestoreException && (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED || e.code == FirebaseFirestoreException.Code.UNAVAILABLE)) {
                _isQuotaExceeded.value = true
                stopRealtimeSync()
            }
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            Result.success(Unit)
        }
    }

    suspend fun updateOrderWeightAndPrice(
        orderId: String,
        weightKg: Float,
        totalPrice: Float,
        adminNotes: String? = null,
        context: Context? = appContext
    ): Result<Unit> = updateOrderWeightAndPrice(
        orderId = orderId,
        weightKg = weightKg.toDouble(),
        totalPrice = totalPrice.toDouble(),
        adminNotes = adminNotes,
        context = context
    )

    /**
     * Suppression d'une commande avec synchronisation hors-ligne
     */
    suspend fun deleteOrder(orderId: String, context: Context? = appContext): Result<Unit> = withContext(Dispatchers.IO) {
        orderDao.deleteOrderById(orderId)

        val actionId = UUID.randomUUID().toString()
        val pendingAction = PendingActionEntity(
            id = actionId,
            actionType = PendingActionTypes.DELETE_ORDER,
            payloadJson = gson.toJson(mapOf("id" to orderId)),
            timestamp = System.currentTimeMillis()
        )
        pendingActionDao.insertPendingAction(pendingAction)

        if (_isQuotaExceeded.value) {
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            return@withContext Result.success(Unit)
        }

        try {
            withTimeout(5000L) {
                db.collection("orders").document(orderId).delete().await()
                try {
                    db.collection("ordres").document(orderId).delete().await()
                } catch (_: Throwable) {}
            }
            pendingActionDao.deletePendingAction(actionId)
            Result.success(Unit)
        } catch (e: Throwable) {
            context?.let { QuotaRetryWorker.enqueueWork(it) }
            Result.success(Unit)
        }
    }

    // =========================================================================
    // 5. GESTION CLOUDINARY AVEC BLOCAGE QUOTA (CRITIQUE)
    // =========================================================================

    /**
     * Téléverse une photo vers Cloudinary en utilisant l'API REST HTTP non-signée.
     * BLOQUÉ automatiquement si le quota Firestore est saturé pour éviter la surcharge.
     */
    suspend fun uploadPhotoToCloudinary(context: Context, fileUri: Uri): String? = withContext(Dispatchers.IO) {
        if (_isQuotaExceeded.value) {
            Log.w(TAG, "Téléversement Cloudinary bloqué : Quota dépassé.")
            return@withContext null
        }

        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext null
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.isEmpty()) return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", CLOUDINARY_PRESET)
                .addFormDataPart("folder", CLOUDINARY_FOLDER)
                .addFormDataPart(
                    "file",
                    "order_photo_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url").takeIf { it.isNotBlank() }
                    ?: json.optString("url")
                return@withContext secureUrl
            } else {
                Log.e(TAG, "Échec Cloudinary HTTP code: ${response.code} body: ${response.body?.string()}")
                return@withContext null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception lors du téléversement Cloudinary: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Téléverse la photo sur Cloudinary puis enregistre l'URL dans Firestore et Room.
     * Si quota dépassé, bloque l'action et retourne une erreur explicite.
     */
    suspend fun uploadAndSaveOrderPhoto(
        context: Context,
        orderId: String,
        fileUri: Uri,
        fieldName: String = "photoUri"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (_isQuotaExceeded.value) {
            return@withContext Result.failure(
                Exception("Téléversement Cloudinary désactivé : quota Firestore saturé ou réseau indisponible. Action suspendue.")
            )
        }

        try {
            val uploadedUrl = uploadPhotoToCloudinary(context, fileUri)
                ?: return@withContext Result.failure(Exception("Échec de l'envoi de l'image vers Cloudinary (Réseau ou fichier)"))

            // Mise à jour Room locale
            val current = orderDao.getOrderById(orderId)
            if (current != null) {
                val updated = current.copy(photoUri = uploadedUrl, appareilPhoto = uploadedUrl)
                orderDao.insertOrder(updated)
            }

            withTimeout(5000L) {
                val updates = hashMapOf<String, Any>(
                    fieldName to uploadedUrl,
                    "photoUri" to uploadedUrl,
                    "imageUrl" to uploadedUrl,
                    "photoUrl" to uploadedUrl,
                    "appareilPhoto" to uploadedUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                db.collection("orders").document(orderId).set(updates, SetOptions.merge()).await()
                try {
                    db.collection("ordres").document(orderId).set(updates, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            Result.success(uploadedUrl)
        } catch (e: Throwable) {
            Log.e(TAG, "Échec enregistrement photo de commande $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. VIDAGE DU CACHE LOCAL (DÉCONNEXION SÉCURISÉE)
    // =========================================================================

    /**
     * Supprime toutes les commandes stockées localement dans Room à la déconnexion.
     */
    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        try {
            orderDao.clearAllOrders()
        } catch (e: Throwable) {
            Log.e(TAG, "Erreur lors du vidage du cache des commandes: ${e.message}", e)
        }
    }
}
