package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.di.Graph
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Worker WorkManager pour le traitement différé et résilient des actions en file d'attente
 * suite à l'épuisement des quotas Firestore (RESOURCE_EXHAUSTED) ou aux coupures réseau (UNAVAILABLE).
 */
class QuotaRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "QuotaRetryWorker"
        const val UNIQUE_WORK_NAME = "BellaPressing_QuotaRetryWork"

        /**
         * Planifie le Worker avec contrainte réseau et politique de backoff exponentiel (15 min).
         */
        fun enqueueWork(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<QuotaRetryWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15,
                        TimeUnit.MINUTES
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    policy,
                    workRequest
                )
                Log.d(TAG, "QuotaRetryWorker planifié avec politique $policy")
            } catch (e: Throwable) {
                Log.e(TAG, "Erreur planification QuotaRetryWorker: ${e.message}", e)
            }
        }

        /**
         * Déclenche une tentative immédiate de synchronisation de la file d'attente locale.
         */
        fun triggerImmediate(context: Context) {
            enqueueWork(context, ExistingWorkPolicy.REPLACE)
        }
    }

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    override suspend fun doWork(): Result {
        Log.d(TAG, "Démarrage du traitement de la file d'attente Quota / Hors-ligne...")
        val pendingDao = try {
            Graph.db.pendingActionDao()
        } catch (e: Throwable) {
            Log.e(TAG, "Base de données inaccessible dans QuotaRetryWorker", e)
            return Result.retry()
        }

        val actions = pendingDao.getAllPendingActions()
        if (actions.isEmpty()) {
            Log.d(TAG, "Aucune action en attente. Succès.")
            return Result.success()
        }

        Log.d(TAG, "${actions.size} action(s) trouvée(s) dans la file d'attente.")
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase non initialisé", e)
            return Result.retry()
        }

        for (action in actions) {
            // Protection anti-corruption : si plus de 5 tentatives infructueuses (retryCount > 5), on supprime pour ne pas bloquer la file
            if (action.retryCount > 5) {
                Log.w(TAG, "Action ${action.id} abandonnée et supprimée après ${action.retryCount} tentatives infructueuses (protection payload corrompu).")
                pendingDao.deletePendingAction(action.id)
                continue
            }

            try {
                executeAction(db, action)
                // Succès : retrait de la file locale
                pendingDao.deletePendingAction(action.id)
                Log.d(TAG, "Action ${action.actionType} (ID: ${action.id}) synchronisée avec succès.")
            } catch (e: Throwable) {
                Log.e(TAG, "Échec lors de l'exécution de l'action ${action.actionType}: ${e.message}", e)

                // Vérification si l'erreur est liée au quota ou au réseau
                val isQuotaOrNetwork = when (e) {
                    is FirebaseFirestoreException -> {
                        e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ||
                                e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                                e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
                    }
                    is java.io.IOException, is java.net.SocketTimeoutException, is java.net.UnknownHostException -> true
                    else -> false
                }

                // Incrémenter le retryCount
                val newRetryCount = action.retryCount + 1
                if (newRetryCount > 5) {
                    Log.w(TAG, "Action ${action.id} dépasse la limite de 5 tentatives suite à l'échec courant. Suppression définitive.")
                    pendingDao.deletePendingAction(action.id)
                } else {
                    pendingDao.updatePendingAction(action.copy(retryCount = newRetryCount))
                }

                if (isQuotaOrNetwork) {
                    Log.w(TAG, "Quota dépassé ou réseau indisponible. Suspension de la file d'attente.")
                    return Result.retry()
                }
            }
        }

        // Si des actions restent encore à traiter après la boucle
        val remainingCount = pendingDao.getPendingActionCount()
        return if (remainingCount > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    /**
     * Routage idempotent selon le type d'action
     */
    private suspend fun executeAction(db: FirebaseFirestore, action: PendingActionEntity) {
        val payloadMap: Map<String, Any?> = try {
            gson.fromJson(action.payloadJson, mapType) ?: emptyMap()
        } catch (_: Throwable) {
            emptyMap()
        }

        when (action.actionType) {
            PendingActionTypes.UPDATE_ORDER,
            PendingActionTypes.UPDATE_ORDER_STATUS,
            PendingActionTypes.UPDATE_ORDER_WEIGHT_PRICE,
            PendingActionTypes.ASSIGN_DELIVERY -> {
                val orderId = payloadMap["id"]?.toString()
                    ?: payloadMap["orderId"]?.toString()
                    ?: throw IllegalArgumentException("Identifiant de commande manquant dans le payload")

                val cleanMap = payloadMap.filterValues { it != null }

                // Idempotence : SetOptions.merge() pour préserver les modifications concurrentes
                db.collection("orders").document(orderId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("ordres").document(orderId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_ORDER -> {
                val orderId = payloadMap["id"]?.toString()
                    ?: payloadMap["orderId"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")

                if (orderId.isNotBlank()) {
                    db.collection("orders").document(orderId).delete().await()
                    try {
                        db.collection("ordres").document(orderId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_USER -> {
                val userId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID utilisateur manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("users").document(userId).set(cleanMap).await()
                try {
                    db.collection("utilisateurs").document(userId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.UPDATE_USER -> {
                val userId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID utilisateur manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("users").document(userId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("utilisateurs").document(userId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_USER -> {
                val userId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (userId.isNotBlank()) {
                    db.collection("users").document(userId).delete().await()
                    try {
                        db.collection("utilisateurs").document(userId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_STOCK -> {
                val stockId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID stock manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("stocks").document(stockId).set(cleanMap).await()
                try {
                    db.collection("stock").document(stockId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.UPDATE_STOCK -> {
                val stockId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID stock manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("stocks").document(stockId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("stock").document(stockId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_STOCK -> {
                val stockId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (stockId.isNotBlank()) {
                    db.collection("stocks").document(stockId).delete().await()
                    try {
                        db.collection("stock").document(stockId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_PROMO -> {
                val promoId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID promo manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("promos").document(promoId).set(cleanMap).await()
                try {
                    db.collection("promotions").document(promoId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.UPDATE_PROMO -> {
                val promoId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID promo manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("promos").document(promoId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("promotions").document(promoId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_PROMO -> {
                val promoId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (promoId.isNotBlank()) {
                    db.collection("promos").document(promoId).delete().await()
                    try {
                        db.collection("promotions").document(promoId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_RULE -> {
                val ruleId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID règle manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("automation_rules").document(ruleId).set(cleanMap).await()
                try {
                    db.collection("regles").document(ruleId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.UPDATE_RULE -> {
                val ruleId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID règle manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("automation_rules").document(ruleId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("regles").document(ruleId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_RULE -> {
                val ruleId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (ruleId.isNotBlank()) {
                    db.collection("automation_rules").document(ruleId).delete().await()
                    try {
                        db.collection("regles").document(ruleId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_ANNOUNCEMENT -> {
                val annId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID annonce manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("announcements").document(annId).set(cleanMap).await()
                try {
                    db.collection("annonces").document(annId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_ANNOUNCEMENT -> {
                val annId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (annId.isNotBlank()) {
                    db.collection("announcements").document(annId).delete().await()
                    try {
                        db.collection("annonces").document(annId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.CREATE_SPECIAL_ITEM,
            PendingActionTypes.CREATE_GOMMAGE_ITEM -> {
                val itemId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID catalogue manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("catalog").document(itemId).set(cleanMap).await()
                try {
                    db.collection("catalogue").document(itemId).set(cleanMap).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.UPDATE_SPECIAL_ITEM,
            PendingActionTypes.UPDATE_GOMMAGE_ITEM -> {
                val itemId = payloadMap["id"]?.toString()
                    ?: throw IllegalArgumentException("ID catalogue manquant")
                val cleanMap = payloadMap.filterValues { it != null }

                db.collection("catalog").document(itemId).set(cleanMap, SetOptions.merge()).await()
                try {
                    db.collection("catalogue").document(itemId).set(cleanMap, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            PendingActionTypes.DELETE_SPECIAL_ITEM,
            PendingActionTypes.DELETE_GOMMAGE_ITEM -> {
                val itemId = payloadMap["id"]?.toString()
                    ?: action.payloadJson.trim().removeSurrounding("\"")
                if (itemId.isNotBlank()) {
                    db.collection("catalog").document(itemId).delete().await()
                    try {
                        db.collection("catalogue").document(itemId).delete().await()
                    } catch (_: Throwable) {}
                }
            }

            PendingActionTypes.UPDATE_CONFIG -> {
                val configId = payloadMap["id"]?.toString() ?: "global_config"
                val cleanMap = payloadMap.filterValues { it != null }
                db.collection("config").document(configId).set(cleanMap, SetOptions.merge()).await()
            }

            PendingActionTypes.UPDATE_ANTI_FRAUD -> {
                val ruleId = payloadMap["id"]?.toString() ?: "anti_fraud_global"
                val cleanMap = payloadMap.filterValues { it != null }
                db.collection("anti_fraud_rules").document(ruleId).set(cleanMap, SetOptions.merge()).await()
            }

            else -> {
                Log.w(TAG, "Type d'action non reconnu: ${action.actionType}. Aucune opération réseau.")
            }
        }
    }
}
