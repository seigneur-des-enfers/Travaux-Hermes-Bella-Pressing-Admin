package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdminOrderRepository
import com.example.data.OrderEntity
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel pour l'administration et les livreurs de Bella Pressing.
 * Architecture Offline-First stricte : l'UI consomme uniquement les Flow issus de Room (SSOT).
 * Gère le cycle de vie du Listener temps réel, la résilience aux quotas (RESOURCE_EXHAUSTED / UNAVAILABLE),
 * et la file d'attente locale WorkManager.
 */
class AdminOrderViewModel(
    private val repository: AdminOrderRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AdminOrderViewModel"
    }

    private val _isFirestoreQuotaExhausted = MutableStateFlow(false)
    val isFirestoreQuotaExhausted: StateFlow<Boolean> = _isFirestoreQuotaExhausted.asStateFlow()

    fun triggerQuotaAlert() {
        _isFirestoreQuotaExhausted.value = true
    }

    // Référence aux listeners Firestore actifs pour libération dans onCleared
    private val activeOrdersListeners = mutableListOf<ListenerRegistration>()

    // =========================================================================
    // 1. FLUX RÉACTIFS ROOM (SSOT EXCLUSIF)
    // =========================================================================

    /**
     * Liste réactive des commandes actives (En attente, En cours, Prêt, etc.)
     */
    val activeOrders: StateFlow<List<OrderEntity>> = repository.getActiveOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Liste réactive de l'historique des commandes (Livrées, Clôturées, Annulées)
     */
    val historicalOrders: StateFlow<List<OrderEntity>> = repository.getHistoricalOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Indicateur de saturation quota Firestore ou coupure réseau (Mode dégradé résilient)
     */
    val isQuotaExceeded: StateFlow<Boolean> = repository.isQuotaExceeded

    /**
     * Nombre d'actions Administrateur enregistrées localement en attente de synchronisation
     */
    val pendingActionsCount: StateFlow<Int> = repository.pendingActionsCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // =========================================================================
    // 2. ÉTATS DE L'INTERFACE UTILISATEUR (UI STATES)
    // =========================================================================

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        // Démarrage automatique sécurisé sur Dispatchers.IO pour éviter tout blocage UI
        viewModelScope.launch(Dispatchers.IO) {
            startRealtimeSync()
        }
    }

    // =========================================================================
    // 3. GESTION DE LA SYNCHRONISATION & CYCLE DE VIE
    // =========================================================================

    /**
     * Démarre l'écouteur temps réel filtré sur les commandes actives.
     */
    fun startRealtimeSync() {
        if (activeOrdersListeners.isNotEmpty()) return

        _isSyncing.value = true
        try {
            val registrations = repository.syncActiveOrdersRealtime()
            activeOrdersListeners.addAll(registrations)
            _isOffline.value = false
        } catch (e: Throwable) {
            Log.e(TAG, "Erreur démarrage synchronisation temps réel: ${e.message}", e)
            _isOffline.value = true
            _errorMessage.value = "Mode hors-ligne : affichage des données locales en cache."
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Déclenche une tentative de reconnexion manuelle et rejoue la file d'attente.
     */
    fun retryConnection(context: Context? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.retryConnection(context)
            _isSyncing.value = false
            _successMessage.value = "Tentative de synchronisation relancée."
        }
    }

    /**
     * Récupère l'historique ponctuel des commandes terminées/annulées sans écouteur permanent.
     */
    fun fetchHistoricalOrders(limit: Int = 50) {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            val result = repository.fetchHistoricalOrders(limit)
            _isLoadingHistory.value = false

            result.onSuccess {
                _isOffline.value = false
                Log.d(TAG, "Historique récupéré et inséré dans Room : ${it.size} commandes.")
            }.onFailure { error ->
                _isOffline.value = true
                _errorMessage.value = "Historique en local : ${error.localizedMessage ?: "Réseau ou quota indisponible"}"
            }
        }
    }

    // =========================================================================
    // 4. ACTIONS MÉTIER (ADMINISTRATION & LIVRAISON)
    // =========================================================================

    /**
     * Met à jour le statut d'une commande (ex: EN_COURS, PRET, LIVRE, etc.).
     * Enregistre localement dans Room et planifie l'envoi en arrière-plan.
     */
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        adminNotes: String? = null,
        deliveryBoyId: String? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, newStatus, adminNotes, deliveryBoyId, context)
            result.onSuccess {
                _successMessage.value = "Statut enregistré : $newStatus (sauvegarde locale garantie)"
            }.onFailure { error ->
                _errorMessage.value = "Échec statut : ${error.localizedMessage}"
            }
        }
    }

    /**
     * Enregistre le poids et calcule le prix final de la commande.
     */
    fun updateOrderWeightAndPrice(
        orderId: String,
        weightKg: Double,
        totalPrice: Double,
        adminNotes: String? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val result = repository.updateOrderWeightAndPrice(orderId, weightKg, totalPrice, adminNotes, context)
            result.onSuccess {
                _successMessage.value = "Poids ($weightKg kg) et prix ($totalPrice F) validés et enregistrés localement."
            }.onFailure { error ->
                _errorMessage.value = "Échec de l'envoi : ${error.localizedMessage}"
            }
        }
    }

    fun updateOrderWeightAndPrice(
        orderId: String,
        weightKg: Float,
        totalPrice: Float,
        adminNotes: String? = null,
        context: Context? = null
    ) = updateOrderWeightAndPrice(orderId, weightKg.toDouble(), totalPrice.toDouble(), adminNotes, context)

    /**
     * Téléverse une photo de preuve (Cloudinary) et met à jour Firestore.
     * Si quota dépassé, bloque l'action avec un message explicite.
     */
    fun uploadOrderPhoto(context: Context, orderId: String, fileUri: Uri) {
        viewModelScope.launch {
            if (isQuotaExceeded.value) {
                _errorMessage.value = "Téléversement Cloudinary bloqué : Quota Firestore saturé. Utilisez le mode hors-ligne."
                return@launch
            }

            _isSyncing.value = true
            val result = repository.uploadAndSaveOrderPhoto(context, orderId, fileUri)
            _isSyncing.value = false

            result.onSuccess { url ->
                _successMessage.value = "Photo transmise et enregistrée avec succès."
            }.onFailure { error ->
                _errorMessage.value = "Erreur photo : ${error.localizedMessage ?: "Réseau ou Cloudinary inaccessible"}"
            }
        }
    }

    /**
     * Supprime une commande de manière persistante (Room + File d'attente Firestore)
     */
    fun deleteOrder(orderId: String, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteOrder(orderId, context)
            _successMessage.value = "Commande supprimée localement (en attente de sync en ligne)."
        }
    }

    /**
     * Vide le cache local à la déconnexion.
     */
    fun clearCache() {
        viewModelScope.launch {
            repository.clearLocalCache()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // =========================================================================
    // 5. GESTION IMPÉRATIVE DU CYCLE DE VIE
    // =========================================================================

    override fun onCleared() {
        super.onCleared()
        // Nettoyage impératif des listeners Firestore pour éviter les fuites mémoire et l'épuisement des quotas
        activeOrdersListeners.forEach { it.remove() }
        activeOrdersListeners.clear()
        repository.stopRealtimeSync()
        Log.d(TAG, "AdminOrderViewModel détruit: Listeners Firestore désabonnés avec succès.")
    }
}

/**
 * Factory pour instancier AdminOrderViewModel avec injection du Repository.
 */
class AdminOrderViewModelFactory(
    private val repository: AdminOrderRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminOrderViewModel::class.java)) {
            return AdminOrderViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel inconnue: ${modelClass.name}")
    }
}
