package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.OrderEntity
import com.example.data.AntiFraudRuleEntity
import com.example.data.UserEntity
import com.example.data.ActivityLogEntity
import com.example.data.ConsumableEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import com.example.data.ConfigEntity
import com.example.data.SpecialItemEntity
import com.example.data.GommageItemEntity
import com.example.data.PromoEntity
import com.example.data.RuleEntity

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val _isFirestoreQuotaExhausted = MutableStateFlow(false)
    val isFirestoreQuotaExhausted: StateFlow<Boolean> = _isFirestoreQuotaExhausted.asStateFlow()

    fun triggerQuotaAlert() {
        _isFirestoreQuotaExhausted.value = true
    }

    fun dismissQuotaAlert() {
        _isFirestoreQuotaExhausted.value = false
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshClients() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchClients()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Erreur lors du rafraîchissement des clients: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshOrderHistory() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                com.example.di.Graph.adminOrderRepository.fetchHistoricalOrders(100)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Erreur lors du rafraîchissement de l'historique: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchCatalog()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Erreur lors du rafraîchissement du catalogue: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchSettings()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Erreur lors du rafraîchissement des paramètres: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchClients()
                repository.fetchCatalog()
                repository.fetchSettings()
                com.example.di.Graph.adminOrderRepository.fetchHistoricalOrders(100)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Erreur lors du rafraîchissement global: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.example.di.Graph.adminOrderRepository.stopRealtimeSync()
        } catch (_: Throwable) {}
    }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()
    val allAnnouncements = repository.allAnnouncements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allActivityLogs = repository.getAllActivityLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    

    init {
        viewModelScope.launch {
            com.example.data.FirestoreSyncManager.isQuotaExhausted.collect { isExhausted ->
                if (isExhausted) {
                    triggerQuotaAlert()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupDuplicates()
            val conf = repository.getConfig().firstOrNull()
            if (conf == null) {
                repository.insertConfig(ConfigEntity())
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupDuplicates()
            val adminEmail = "admin@bellapressing.com"
            val existingAdmin = repository.getUserByEmail(adminEmail)
            if (existingAdmin == null) {
                repository.insertUser(UserEntity(
                    id = "DEFAULT_SUPER_ADMIN_ID",
                    role = "SUPER_ADMIN",
                    nom = "Bella",
                    prenom = "Founè",
                    email = adminEmail,
                    telephone = "0000000000",
                    motDePasse = "admin",
                    coordonnees = "Direction Bella Pressing"
                ))
            } else if (existingAdmin.role == "ADMIN") {
                repository.updateUser(existingAdmin.copy(role = "SUPER_ADMIN"))
            }
            val clientEmail = "client@test.com"
            if (repository.getUserByEmail(clientEmail) == null) {
                repository.insertUser(UserEntity(
                    id = "DEFAULT_CLIENT_ID",
                    role = "CLIENT",
                    nom = "Testeur",
                    prenom = "Client",
                    email = clientEmail,
                    telephone = "0102030405",
                    motDePasse = "client",
                    coordonnees = "Adresse de test"
                ))
            }
        }
    }

    fun login(email: String, role: String, onResult: (Int, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var user = repository.getUserByEmail(email)
            if (user == null) {
                // FALLBACK: Query Firestore directly for the email to prevent data loss amnesia!
                try {
                    val db = FirebaseFirestore.getInstance()
                    var snapshot = db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .await()
                    if (snapshot.isEmpty) {
                        snapshot = db.collection("utilisateurs")
                            .whereEqualTo("email", email)
                            .get()
                            .await()
                    }
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val docNom = doc.getString("nom")?.takeIf { it.isNotEmpty() } ?: doc.getString("nomAdmin") ?: doc.getString("nomUser") ?: ""
                        val docPrenom = doc.getString("prenom")?.takeIf { it.isNotEmpty() } ?: doc.getString("prenomAdmin") ?: doc.getString("prenomUser") ?: ""
                        val docEmail = doc.getString("email")?.takeIf { it.isNotEmpty() } ?: doc.getString("mailAdmin") ?: doc.getString("mailUser") ?: ""
                        val docMotDePasse = doc.getString("motDePasse")?.takeIf { it.isNotEmpty() } ?: doc.getString("motDePasseAdmin") ?: doc.getString("motDePasseUser") ?: ""

                        val fetchedUser = UserEntity(
                            id = doc.id,
                            nom = docNom,
                            prenom = docPrenom,
                            email = docEmail,
                            motDePasse = docMotDePasse,
                            role = doc.getString("role") ?: "CLIENT",
                            telephone = doc.getString("telephone") ?: doc.getString("numeroUser") ?: "",
                            coordonnees = doc.getString("coordonnees") ?: doc.getString("adresseUser") ?: "",
                            token = doc.getString("token") ?: "",
                            fidelityPointsKg = doc.getDouble("fidelityPointsKg")?.toFloat() ?: 0f,
                            myReferralCode = doc.getString("myReferralCode") ?: "",
                            referredBy = doc.getString("referredBy") ?: "",
                            isSuspended = doc.getBoolean("isSuspended") ?: false,
                            canUsePromo = doc.getBoolean("canUsePromo") ?: true,
                            canUsePromoUntil = doc.getLong("canUsePromoUntil"),
                            canOrder = doc.getBoolean("canOrder") ?: true,
                            suspensionReason = doc.getString("suspensionReason") ?: "",
                            suspensionUntil = doc.getLong("suspensionUntil"),
                            isArchived = doc.getBoolean("isArchived") ?: false,
                            permissions = doc.getString("permissions") ?: "ALL",
                            isLoyaltyEnabled = doc.getBoolean("isLoyaltyEnabled") ?: true,
                            loyaltyValidUntil = doc.getLong("loyaltyValidUntil"),
                            customAutoBanEnabled = doc.getBoolean("customAutoBanEnabled") ?: false,
                            customAutoBanThreshold = doc.getLong("customAutoBanThreshold")?.toInt() ?: 0,
                            customAutoBanDurationHours = doc.getLong("customAutoBanDurationHours")?.toInt() ?: 0,
                            canSelectPieces = doc.getBoolean("canSelectPieces") ?: true,
                            canSelectCategories = doc.getBoolean("canSelectCategories") ?: true,
                            canEstimateKg = doc.getBoolean("canEstimateKg") ?: true,
                            canUseExpress = doc.getBoolean("canUseExpress") ?: true,
                            canUsePremium = doc.getBoolean("canUsePremium") ?: true,

                            prenomAdmin = doc.getString("prenomAdmin") ?: docPrenom,
                            nomAdmin = doc.getString("nomAdmin") ?: docNom,
                            mailAdmin = doc.getString("mailAdmin") ?: docEmail,
                            motDePasseAdmin = doc.getString("motDePasseAdmin") ?: docMotDePasse,
                            roleAdminSuper = doc.getBoolean("roleAdminSuper") ?: (doc.getString("role") == "SUPER_ADMIN"),

                            prenomUser = doc.getString("prenomUser") ?: docPrenom,
                            nomUser = doc.getString("nomUser") ?: docNom,
                            numeroUser = doc.getString("numeroUser") ?: doc.getString("telephone") ?: "",
                            mailUser = doc.getString("mailUser") ?: docEmail,
                            adresseUser = doc.getString("adresseUser") ?: doc.getString("coordonnees") ?: "",
                            motDePasseUser = doc.getString("motDePasseUser") ?: docMotDePasse
                        )
                        repository.insertUser(fetchedUser)
                        user = fetchedUser
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            if (user != null && (user.role == role || (role == "ADMIN" && user.role == "SUPER_ADMIN"))) {
                if (user.isArchived) {
                    withContext(Dispatchers.Main) {
                        onResult(1, null) // Archived user cannot login
                    }
                    return@launch
                }
                if (user.isSuspended) {
                    if (user.suspensionUntil != null && System.currentTimeMillis() > user.suspensionUntil) {
                        val unSuspended = user.copy(isSuspended = false, suspensionReason = "", suspensionUntil = null)
                        updateUser(unSuspended)
                        _currentUser.value = unSuspended
                        withContext(Dispatchers.Main) {
                            onResult(0, null)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult(2, user.suspensionReason) // Suspended
                        }
                    }
                } else {
                    _currentUser.value = user
                    withContext(Dispatchers.Main) {
                        onResult(0, null) // Success
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(1, null) // Invalid
                }
            }
        }
    }

    fun register(
        role: String,
        nom: String,
        prenom: String,
        email: String,
        telephone: String,
        motDePasse: String,
        coordonnees: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var existing = repository.getUserByEmail(email)
            if (existing == null) {
                // Check Firestore as well to prevent duplicates if local DB was wiped
                try {
                    val db = FirebaseFirestore.getInstance()
                    var snapshot = db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .await()
                    if (snapshot.isEmpty) {
                        snapshot = db.collection("utilisateurs")
                            .whereEqualTo("email", email)
                            .get()
                            .await()
                    }
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        existing = UserEntity(
                            id = doc.id,
                            nom = doc.getString("nom") ?: "",
                            prenom = doc.getString("prenom") ?: "",
                            email = doc.getString("email") ?: "",
                            motDePasse = doc.getString("motDePasse") ?: "",
                            role = doc.getString("role") ?: "CLIENT",
                            telephone = doc.getString("telephone") ?: ""
                        )
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            if (existing != null) {
                withContext(Dispatchers.Main) {
                    onResult(false) // Already exists
                }
            } else {
                val newUser = UserEntity(
                    role = role,
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    telephone = telephone,
                    motDePasse = motDePasse,
                    coordonnees = coordonnees
                )
                insertUser(newUser)
                _currentUser.value = newUser
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // Orders for current user (Client)
    @OptIn(ExperimentalCoroutinesApi::class)
    val userOrders: StateFlow<List<OrderEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getOrdersForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insertUser(user: UserEntity) {
        viewModelScope.launch {
            repository.insertUser(user)
            pushUserToFirestoreDirect(user)
        }
    }

    fun updateUser(user: UserEntity) {
        val updatedUser = user.copy(
            nomUser = user.nom,
            prenomUser = user.prenom,
            mailUser = user.email,
            numeroUser = user.telephone,
            adresseUser = user.coordonnees,
            motDePasseUser = user.motDePasse,
            cumulKiloFideliterUser = user.fidelityPointsKg.toDouble(),

            autoriseCreatOrder = user.canOrder,
            autoriseSaisiNombrePiece = user.canSelectPieces,
            autoriseSelectionCategory = user.canSelectCategories,
            autoriseServiceExpress = user.canUseExpress,
            autoriseUtiliserCodePromos = user.canUsePromo,
            autoriseVoirEstimationPrixFinal = user.canEstimateKg,

            prenomAdmin = if (user.role != "CLIENT") user.prenom else user.prenomAdmin,
            nomAdmin = if (user.role != "CLIENT") user.nom else user.nomAdmin,
            mailAdmin = if (user.role != "CLIENT") user.email else user.mailAdmin,
            motDePasseAdmin = if (user.role != "CLIENT") user.motDePasse else user.motDePasseAdmin,

            estSuspendu = user.isSuspended,
            raisonSuspension = user.suspensionReason,
            estBanni = user.isBanned,
            raisonBannissement = user.banReason,
            bannissementPermanent = user.isBanPermanent,
            dateFinBannissement = user.banUntil
        )
        viewModelScope.launch {
            repository.saveUserResilient(updatedUser)
            if (_currentUser.value?.id == updatedUser.id) {
                _currentUser.value = updatedUser
            }
            pushUserToFirestoreDirect(updatedUser)
        }
    }

    fun suspendUser(user: UserEntity, reason: String, durationHours: Long? = null) {
        val until = if (durationHours != null && durationHours > 0) System.currentTimeMillis() + (durationHours * 60 * 60 * 1000L) else null
        val updated = user.copy(
            isSuspended = true,
            estSuspendu = true,
            suspensionReason = reason.ifBlank { "Mesure administrative conservatoire" },
            raisonSuspension = reason.ifBlank { "Mesure administrative conservatoire" },
            suspensionUntil = until
        )
        updateUser(updated)
        logAdminAction(currentUser.value?.id, user.id, "SUSPENSION", "Compte client suspendu. Motif: ${reason.ifBlank { "Mesure conservatoire" }} (Durée: ${durationHours ?: "Indéterminée"}h)")
    }

    fun banUser(user: UserEntity, reason: String, isPermanent: Boolean, durationHours: Long? = null) {
        val until = if (!isPermanent && durationHours != null && durationHours > 0) {
            System.currentTimeMillis() + (durationHours * 60 * 60 * 1000L)
        } else null

        val updated = user.copy(
            isBanned = true,
            estBanni = true,
            banReason = reason.ifBlank { if (isPermanent) "Radiation définitive du service" else "Sanction disciplinaire temporaire" },
            raisonBannissement = reason.ifBlank { if (isPermanent) "Radiation définitive du service" else "Sanction disciplinaire temporaire" },
            isBanPermanent = isPermanent,
            bannissementPermanent = isPermanent,
            banUntil = until,
            dateFinBannissement = until
        )
        updateUser(updated)
        logAdminAction(currentUser.value?.id, user.id, "BAN", "Compte client banni (${if (isPermanent) "DÉFINITIF" else "${durationHours ?: 0}h"}). Motif: $reason")
    }

    fun liftSanctions(user: UserEntity) {
        val updated = user.copy(
            isSuspended = false,
            estSuspendu = false,
            suspensionReason = "",
            raisonSuspension = "",
            suspensionUntil = null,

            isBanned = false,
            estBanni = false,
            banReason = "",
            raisonBannissement = "",
            isBanPermanent = false,
            bannissementPermanent = false,
            banUntil = null,
            dateFinBannissement = null
        )
        updateUser(updated)
        logAdminAction(currentUser.value?.id, user.id, "UNBAN", "Sanctions levées / Compte réactivé par l'administrateur.")
    }

    private suspend fun pushUserToFirestoreDirect(user: UserEntity) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userMap = mapOf(
                "nom" to user.nom,
                "prenom" to user.prenom,
                "email" to user.email,
                "motDePasse" to user.motDePasse,
                "role" to user.role,
                "telephone" to user.telephone,
                "coordonnees" to user.coordonnees,
                "token" to user.token,
                "fidelityPointsKg" to user.fidelityPointsKg,
                "myReferralCode" to user.myReferralCode,
                "referredBy" to user.referredBy,
                
                "isSuspended" to user.isSuspended,
                "estSuspendu" to user.isSuspended,
                "suspensionReason" to user.suspensionReason,
                "raisonSuspension" to user.suspensionReason,
                "suspensionUntil" to user.suspensionUntil,

                "isBanned" to user.isBanned,
                "estBanni" to user.isBanned,
                "banni" to user.isBanned,
                "banReason" to user.banReason,
                "raisonBannissement" to user.banReason,
                "motifBannissement" to user.banReason,
                "isBanPermanent" to user.isBanPermanent,
                "bannissementPermanent" to user.isBanPermanent,
                "banUntil" to user.banUntil,
                "dateFinBannissement" to user.banUntil,

                "canUsePromo" to user.canUsePromo,
                "canUsePromoUntil" to user.canUsePromoUntil,
                "canOrder" to user.canOrder,
                "isArchived" to user.isArchived,
                "permissions" to user.permissions,
                "isLoyaltyEnabled" to user.isLoyaltyEnabled,
                "loyaltyValidUntil" to user.loyaltyValidUntil,
                "accumulatedFreeKgs" to user.accumulatedFreeKgs,
                "currentKgTowardsReward" to user.currentKgTowardsReward,
                "lifetimeKgWashed" to user.lifetimeKgWashed,
                "customAutoBanEnabled" to user.customAutoBanEnabled,
                "customAutoBanThreshold" to user.customAutoBanThreshold,
                "customAutoBanDurationHours" to user.customAutoBanDurationHours,
                "canSelectPieces" to user.canSelectPieces,
                "canSelectCategories" to user.canSelectCategories,
                "canEstimateKg" to user.canEstimateKg,
                "canUseExpress" to user.canUseExpress,
                "canUsePremium" to user.canUsePremium,

                // New Schema
                "prenomAdmin" to user.prenomAdmin,
                "nomAdmin" to user.nomAdmin,
                "mailAdmin" to user.mailAdmin,
                "motDePasseAdmin" to user.motDePasseAdmin,
                "roleAdminSuper" to user.roleAdminSuper,

                "permVoirCommandes" to user.permVoirCommandes,
                "permGererCommandes" to user.permGererCommandes,
                "permModifierHeureRamassage" to user.permModifierHeureRamassage,
                "permVoirClients" to user.permVoirClients,
                "permGererClients" to user.permGererClients,
                "permSuspendreClients" to user.permSuspendreClients,
                "permVoirLog" to user.permVoirLog,
                "permGererPromosClient" to user.permGererPromosClient,
                "permVoirCorbeille" to user.permVoirCorbeille,
                "permRestaurerCompte" to user.permRestaurerCompte,
                "permSupprimerDefinitivement" to user.permSupprimerDefinitivement,
                "permVoirStocks" to user.permVoirStocks,
                "permGererStocks" to user.permGererStocks,
                "permGererStocksVisibilite" to user.permGererStocksVisibilite,
                "permVoirCatalogue" to user.permVoirCatalogue,
                "permGererCatalogue" to user.permGererCatalogue,
                "permGererPromos" to user.permGererPromos,
                "permGererPromosAvancees" to user.permGererPromosAvancees,
                "permGererGommage" to user.permGererGommage,
                "permVoirParametres" to user.permVoirParametres,
                "permGererParametres" to user.permGererParametres,
                "permVoirAntiFraudPersonnel" to user.permVoirAntiFraudPersonnel,
                "permGererAntiFraudPersonnel" to user.permGererAntiFraudPersonnel,
                "permVoirAntiFraudGlobal" to user.permVoirAntiFraudGlobal,
                "permGererAntiFraudGlobal" to user.permGererAntiFraudGlobal,

                "suppressionDefinitiveAdmin" to user.suppressionDefinitiveAdmin,
                "restaurerAdmin" to user.restaurerAdmin,

                "prenomUser" to user.prenomUser,
                "nomUser" to user.nomUser,
                "numeroUser" to user.numeroUser,
                "mailUser" to user.mailUser,
                "adresseUser" to user.adresseUser,
                "motDePasseUser" to user.motDePasseUser,
                "cumulKiloFideliterUser" to user.cumulKiloFideliterUser,

                "autoriseCreatOrder" to user.autoriseCreatOrder,
                "autoriseSaisiNombrePiece" to user.autoriseSaisiNombrePiece,
                "autoriseSelectionCategory" to user.autoriseSelectionCategory,
                "autoriseSelectionCatalogue" to user.autoriseSelectionCatalogue,
                "autoriseSelectionArticlesSpecial" to user.autoriseSelectionArticlesSpecial,
                "autoriseSelectionArticlesGommage" to user.autoriseSelectionArticlesGommage,
                "autoriseServiceLavage" to user.autoriseServiceLavage,
                "autoriseLavageSimple" to user.autoriseLavageSimple,
                "autoriseLavageSechagePliage" to user.autoriseLavageSechagePliage,
                "autoriseLavageSechagePliageRepassage" to user.autoriseLavageSechagePliageRepassage,
                "autoriseRamassageLinges" to user.autoriseRamassageLinges,
                "autoriseFixeeDateHeureRamassage" to user.autoriseFixeeDateHeureRamassage,
                "autoriseDateHeureLivraison" to user.autoriseDateHeureLivraison,
                "autoriseInstructionSpecial" to user.autoriseInstructionSpecial,
                "autoriseServiceExpress" to user.autoriseServiceExpress,
                "autoriseGarantiVIP" to user.autoriseGarantiVIP,
                "autoriseUtiliserCodePromos" to user.autoriseUtiliserCodePromos,
                "autoriseVoirEstimationPrixFinal" to user.autoriseVoirEstimationPrixFinal,

                "utiliseAntiFraudeGlobale" to user.utiliseAntiFraudeGlobale,
                "blocagePersistant" to user.blocagePersistant,
                "blocageLimitee" to user.blocageLimitee,
                "dureeBlocage" to user.dureeBlocage,
                "raisonSuspension" to user.raisonSuspension
            )

            val collections = listOf("users", "utilisateurs")
            for (colName in collections) {
                // Direct push by document ID
                db.collection(colName).document(user.id).set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
                
                // Secondary push by email match to ensure client document created under a different ID is also updated
                if (user.email.isNotBlank()) {
                    try {
                        val snapByEmail = db.collection(colName).whereEqualTo("email", user.email).get().await()
                        for (doc in snapByEmail.documents) {
                            if (doc.id != user.id) {
                                doc.reference.set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
                            }
                        }
                        val snapByMailUser = db.collection(colName).whereEqualTo("mailUser", user.email).get().await()
                        for (doc in snapByMailUser.documents) {
                            if (doc.id != user.id) {
                                doc.reference.set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("AppViewModel", "Secondary email match push failed for ${user.email}", e)
                    }
                }
            }
            Log.d("AppViewModel", "Directly pushed user ${user.email} (${user.id}) to Firestore in users & utilisateurs.")
        } catch (e: Throwable) {
            Log.e("AppViewModel", "Error directly pushing user ${user.email} to Firestore", e)
        }
    }

    fun deleteUser(id: String) {
        viewModelScope.launch {
            repository.deleteUser(id)
            hardDeleteFromFirestore(id, "users")
        }
    }

    fun getActivityLogsForUser(userId: String): Flow<List<ActivityLogEntity>> {
        return repository.getActivityLogsForUser(userId)
    }

    fun logAdminAction(adminId: String?, targetUserId: String, action: String, description: String) {
        viewModelScope.launch {
            repository.insertActivityLog(ActivityLogEntity(
                userId = targetUserId,
                action = action,
                description = description,
                actorId = adminId
            ))
        }
    }

    // All orders (Admin)
    val allOrders: StateFlow<List<OrderEntity>> = repository.getAllOrders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    // All clients (Admin)
    val allClients: StateFlow<List<UserEntity>> = repository.getAllClients().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    // All admins (Admin)
    val allAdmins: StateFlow<List<UserEntity>> = repository.getAllAdmins().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val config: StateFlow<ConfigEntity?> = repository.getConfig().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // États spécifiques du programme de fidélité dynamique (Moteur Kilo Gratuit)
    val isLoyaltyProgramEnabled: StateFlow<Boolean> = config.map { it?.isLoyaltyProgramEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val loyaltyKgThreshold: StateFlow<Double> = config.map { it?.loyaltyKgThreshold ?: 10.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10.0)

    val loyaltyKgReward: StateFlow<Double> = config.map { it?.loyaltyKgReward ?: 1.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val loyaltyMaxFreeKgs: StateFlow<Double> = config.map { it?.loyaltyMaxFreeKgs ?: 30.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30.0)

    val loyaltyExpirationDays: StateFlow<Int> = config.map { it?.loyaltyExpirationDays ?: 90 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    val allSpecialItems: StateFlow<List<SpecialItemEntity>> = repository.getAllSpecialItems().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val allGommageItems: StateFlow<List<GommageItemEntity>> = repository.getAllGommageItems().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val allPromos: StateFlow<List<PromoEntity>> = repository.getAllPromos().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    
    val allRules: StateFlow<List<RuleEntity>> = repository.getAllRules().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Archived items flows for Trash tab
    val archivedOrders: StateFlow<List<OrderEntity>> = repository.getArchivedOrders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedClients: StateFlow<List<UserEntity>> = repository.getArchivedClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedAdmins: StateFlow<List<UserEntity>> = repository.getArchivedAdmins().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedSpecialItems: StateFlow<List<SpecialItemEntity>> = repository.getArchivedSpecialItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedGommageItems: StateFlow<List<GommageItemEntity>> = repository.getArchivedGommageItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedPromos: StateFlow<List<PromoEntity>> = repository.getArchivedPromos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedRules: StateFlow<List<RuleEntity>> = repository.getArchivedRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedAntiFraudRules: StateFlow<List<AntiFraudRuleEntity>> = repository.getArchivedAntiFraudRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedConsumables: StateFlow<List<ConsumableEntity>> = repository.getArchivedConsumables().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allGlobalTrash: StateFlow<List<com.example.data.GlobalTrashEntity>> = repository.getAllGlobalTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateConfig(newConfig: ConfigEntity) {
        viewModelScope.launch {
            repository.saveConfigResilient(newConfig)
        }
    }

    fun updateLoyaltySettings(
        enabled: Boolean,
        kgThreshold: Double,
        kgReward: Double,
        maxFreeKgs: Double,
        expirationDays: Int
    ) {
        viewModelScope.launch {
            val current = config.value ?: ConfigEntity()
            val updated = current.copy(
                isLoyaltyProgramEnabled = enabled,
                isLoyaltyEnabled = enabled,
                loyaltyKgThreshold = kgThreshold,
                loyaltyKgReward = kgReward,
                loyaltyMaxFreeKgs = maxFreeKgs,
                loyaltyExpirationDays = expirationDays
            )
            repository.saveConfigResilient(updated)
        }
    }

    fun addSpecialItem(name: String, price: Float) {
        viewModelScope.launch {
            repository.insertSpecialItem(SpecialItemEntity(name = name, price = price))
        }
    }

    fun updateSpecialItem(item: SpecialItemEntity) {
        viewModelScope.launch {
            repository.updateSpecialItem(item)
        }
    }

    fun deleteSpecialItem(id: String) {
        viewModelScope.launch {
            repository.archiveSpecialItem(id)
            addToGlobalTrash(id, "SPECIAL_ITEM", collectionName = "catalog")
        }
    }
    
    fun addGommageItem(name: String, price: Float) {
        viewModelScope.launch {
            val item = com.example.data.GommageItemEntity(name = name, price = price)
            repository.insertGommageItem(item)
            logAdminAction(currentUser.value?.id, "", "CREATE_GOMMAGE_ITEM", "Création de l'article de gommage ${item.name}")
        }
    }
    fun updateGommageItem(item: com.example.data.GommageItemEntity) {
        viewModelScope.launch {
            repository.updateGommageItem(item)
            logAdminAction(currentUser.value?.id, "", "UPDATE_GOMMAGE_ITEM", "Modification de l'article de gommage ${item.name}")
        }
    }
    fun deleteGommageItem(id: String) {
        viewModelScope.launch {
            repository.archiveGommageItem(id)
            addToGlobalTrash(id, "GOMMAGE_ITEM", collectionName = "catalog")
        }
    }

    fun addPromo(code: String, description: String, discountPercent: Float, freeKg: Float, minOrderValue: Float, validUntil: Long? = null, isPermanent: Boolean = true, targetItems: String = "ALL", discountType: String = "PERCENTAGE") {
        viewModelScope.launch {
            repository.insertPromo(PromoEntity(
                code = code, 
                description = description, 
                discountPercent = discountPercent, 
                freeKg = freeKg, 
                minOrderValue = minOrderValue,
                validUntil = validUntil,
                isPermanent = isPermanent,
                targetItems = targetItems,
                discountType = discountType
            ))
        }
    }
    
    fun togglePromoStatus(promo: PromoEntity) {
        viewModelScope.launch {
            repository.updatePromo(promo.copy(isActive = !promo.isActive))
        }
    }
    
    fun deletePromo(id: String) {
        viewModelScope.launch {
            repository.archivePromo(id)
            addToGlobalTrash(id, "PROMO", collectionName = "promotions")
        }
    }
    
    fun addRule(name: String, triggerCondition: String, actionType: String, actionValue: String, constraints: String, isPermanent: Boolean = true, validUntil: Long? = null) {
        viewModelScope.launch {
            repository.insertRule(RuleEntity(
                name = name,
                triggerCondition = triggerCondition,
                actionType = actionType,
                actionValue = actionValue,
                constraints = constraints,
                isPermanent = isPermanent,
                validUntil = validUntil
            ))
        }
    }

    fun updateRuleInfo(rule: RuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun toggleRuleStatus(rule: RuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isActive = !rule.isActive))
        }
    }
    
    fun deleteRule(id: String) {
        viewModelScope.launch {
            repository.archiveRule(id)
            addToGlobalTrash(id, "RULE", collectionName = "automation_rules")
        }
    }

    val allAntiFraudRules: StateFlow<List<AntiFraudRuleEntity>> = repository.getAllAntiFraudRules().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addAntiFraudRule(name: String, triggerEvent: String, threshold: Int, durationHours: Int, action: String) {
        viewModelScope.launch {
            repository.insertAntiFraudRule(AntiFraudRuleEntity(
                name = name,
                triggerEvent = triggerEvent,
                threshold = threshold,
                durationHours = durationHours,
                action = action
            ))
        }
    }

    fun toggleAntiFraudRuleStatus(rule: AntiFraudRuleEntity) {
        viewModelScope.launch {
            repository.updateAntiFraudRule(rule.copy(isActive = !rule.isActive))
        }
    }

    fun deleteAntiFraudRule(id: String) {
        viewModelScope.launch {
            repository.archiveAntiFraudRule(id)
            addToGlobalTrash(id, "ANTI_FRAUD_RULE", collectionName = "anti_fraud_rules")
        }
    }
    
    // We can also fetch on demand to avoid stale state depending on currentUser changes
    fun getUserOrdersFlow(userId: String) = repository.getOrdersForUser(userId)
    
    val allConsumables: StateFlow<List<ConsumableEntity>> = repository.getAllConsumables().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addConsumable(name: String, quantity: Int, threshold: Int, isPublic: Boolean = false) {
        viewModelScope.launch {
            repository.insertConsumable(
                ConsumableEntity(
                    name = name,
                    quantity = quantity,
                    threshold = threshold,
                    isPublic = isPublic,
                    nomProduit = name,
                    quantiterProduit = quantity.toDouble(),
                    produitSeilleAlert = threshold.toDouble(),
                    newStocksPublic = isPublic,
                    newStocksPrivee = !isPublic
                )
            )
        }
    }
    
    fun updateConsumableVisibility(consumable: ConsumableEntity, isPublic: Boolean) {
        viewModelScope.launch {
            repository.updateConsumable(
                consumable.copy(
                    isPublic = isPublic,
                    newStocksPublic = isPublic,
                    newStocksPrivee = !isPublic
                )
            )
        }
    }

    fun updateConsumableQuantity(consumable: ConsumableEntity, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateConsumable(
                consumable.copy(
                    quantity = newQuantity,
                    quantiterProduit = newQuantity.toDouble()
                )
            )
        }
    }
    
    fun deleteConsumable(id: String) {
        viewModelScope.launch {
            repository.archiveConsumable(id)
            addToGlobalTrash(id, "CONSUMABLE", collectionName = "stocks")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userActivityLogs: StateFlow<List<ActivityLogEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getActivityLogsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun createOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.insertOrder(order)
            repository.insertActivityLog(
                ActivityLogEntity(
                    userId = order.userId,
                    orderId = order.id,
                    action = "COMMANDE_CREEE",
                    description = "Nouvelle commande créée (${order.pieces} pièces, ${order.categorie})."
                )
            )
        }
    }

    fun updateOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.updateOrder(order)
            
            val isFinished = order.status == "TERMINE" || order.status == "LIVRE" || order.orderLivree
            val orderWeight = if (order.weight > 0.0) order.weight else order.kg.toDouble()
            if (isFinished && orderWeight > 0.0 && !order.isLoyaltyPointsGranted && !order.fidelityCredited) {
                // Update fidelity points & Kilos Gratuits
                val user = repository.getUserById(order.userId)
                val currentConfig = config.value
                val globalLoyaltyEnabled = currentConfig?.isLoyaltyProgramEnabled == true ||
                        (currentConfig?.isLoyaltyEnabled == true && (currentConfig.loyaltyValidUntil == null || currentConfig.loyaltyValidUntil > System.currentTimeMillis()))
                
                if (user != null && globalLoyaltyEnabled) {
                    val userLoyaltyEnabled = user.isLoyaltyEnabled && (user.loyaltyValidUntil == null || user.loyaltyValidUntil > System.currentTimeMillis())
                    if (userLoyaltyEnabled) {
                        val validatedKg = orderWeight
                        val newLifetime = user.lifetimeKgWashed + validatedKg
                        var newTowardsReward = user.currentKgTowardsReward + validatedKg
                        val threshold = if ((currentConfig?.loyaltyKgThreshold ?: 0.0) > 0.0) currentConfig!!.loyaltyKgThreshold else 10.0
                        val reward = if ((currentConfig?.loyaltyKgReward ?: 0.0) > 0.0) currentConfig!!.loyaltyKgReward else 1.0
                        val maxFreeKgs = if ((currentConfig?.loyaltyMaxFreeKgs ?: 0.0) > 0.0) currentConfig!!.loyaltyMaxFreeKgs else 30.0

                        var freeKgsEarned = 0.0
                        while (newTowardsReward >= threshold) {
                            newTowardsReward -= threshold
                            freeKgsEarned += reward
                        }
                        val newAccumulated = (user.accumulatedFreeKgs + freeKgsEarned).coerceAtMost(maxFreeKgs)
                        val updatedLegacyPoints = user.fidelityPointsKg + orderWeight.toFloat()

                        val updatedUser = user.copy(
                            accumulatedFreeKgs = newAccumulated,
                            currentKgTowardsReward = newTowardsReward,
                            lifetimeKgWashed = newLifetime,
                            fidelityPointsKg = updatedLegacyPoints,
                            cumulKiloFideliterUser = updatedLegacyPoints.toDouble()
                        )
                        updateUser(updatedUser)

                        // Journalisation dans l'ActivityLog (Audit)
                        if (freeKgsEarned > 0.0) {
                            logAdminAction(
                                adminId = currentUser.value?.id ?: "SYSTEM",
                                targetUserId = user.id,
                                action = "FIDELITE_RECOMPENSE",
                                description = "Ajustement Fidélité : +${freeKgsEarned} Kg pour le client ${user.prenom} ${user.nom} via Commande #${order.id}. Motif : Palier de fidélité atteint (Poids validé: ${validatedKg} Kg, Nouveau solde: ${newAccumulated} Kg, VIP: ${newLifetime} Kg)"
                            )
                        } else {
                            logAdminAction(
                                adminId = currentUser.value?.id ?: "SYSTEM",
                                targetUserId = user.id,
                                action = "FIDELITE_PROGRESSION",
                                description = "Progression Fidélité : +${validatedKg} Kg pour le client ${user.prenom} ${user.nom} via Commande #${order.id}. Motif : Progression vers palier (Jauge: ${newTowardsReward}/${threshold} Kg, VIP: ${newLifetime} Kg)"
                            )
                        }
                    }
                }
                val creditedOrder = order.copy(
                    fidelityCredited = true,
                    fideliteCredite = true,
                    isLoyaltyPointsGranted = true
                )
                repository.updateOrder(creditedOrder)
            }
            
            if (order.status == "REJETE") {
                val conf = config.value
                val user = repository.getUserById(order.userId)
                
                if (conf != null && user != null) {
                    val isAutoBanEnabled = if (user.customAutoBanEnabled) true else conf.autoBanEnabled
                    
                    if (isAutoBanEnabled) {
                        val threshold = if (user.customAutoBanEnabled && user.customAutoBanThreshold > 0) user.customAutoBanThreshold else conf.autoBanThreshold
                        val durationHours = if (user.customAutoBanEnabled && user.customAutoBanDurationHours > 0) user.customAutoBanDurationHours else conf.autoBanDurationHours
                        
                        val userOrders = repository.getOrdersForUserList(order.userId)
                        var consecutiveRejects = 0
                        for (o in userOrders) {
                            if (o.status == "REJETE") consecutiveRejects++
                            else break
                        }
                        
                        if (consecutiveRejects >= threshold && !user.isSuspended) {
                            val banDurationMs = durationHours * 60 * 60 * 1000L
                            val suspendedUser = user.copy(
                                isSuspended = true,
                                suspensionReason = "Suspension automatique : Trop de commandes rejetées consécutivement.",
                                suspensionUntil = System.currentTimeMillis() + banDurationMs
                            )
                            updateUser(suspendedUser)
                            logAdminAction(null, user.id, "AUTO_BAN", "Le système a suspendu l'utilisateur automatiquement après $consecutiveRejects commandes rejetées.")
                        }
                    }
                }
            }
            
            val description = when (order.status) {
                "PROPOSITION_ADMIN" -> "L'administrateur a modifié votre commande et attend votre confirmation."
                "ACCEPTE" -> "Votre commande a été acceptée et validée."
                "REJETE" -> "Votre commande a été annulée/rejetée."
                "TERMINE" -> "Votre commande est terminée et prête."
                else -> "Mise à jour de l'état de votre commande."
            }
            
            repository.insertActivityLog(
                ActivityLogEntity(
                    userId = order.userId,
                    orderId = order.id,
                    action = "STATUT_COMMANDE",
                    description = description
                )
            )
        }
    }

    private fun getDualCollectionsFor(collectionName: String): List<String> {
        return when (collectionName.lowercase().trim()) {
            "users", "utilisateurs", "user" -> listOf("users", "utilisateurs")
            "orders", "ordres", "order" -> listOf("orders", "ordres")
            "catalog", "catalogue", "special_item", "gommage_item", "special", "gommage" -> listOf("catalog", "catalogue")
            "stocks", "stock", "consumable", "consommables" -> listOf("stocks", "stock")
            "promotions", "promos", "promo" -> listOf("promotions", "promos")
            "automation_rules", "regles", "rule", "automation_rule" -> listOf("automation_rules", "regles")
            "anti_fraud_rules", "regles_anti_fraude", "anti_fraud", "anti_fraud_rule" -> listOf("anti_fraud_rules", "regles_anti_fraude")
            "global_trash", "corbeille_globale", "trash" -> listOf("global_trash", "corbeille_globale")
            "announcements", "annonces", "announcement" -> listOf("announcements", "annonces")
            else -> listOf(collectionName)
        }
    }

    private fun addToGlobalTrash(entityId: String, type: String, title: String = "", details: String = "", collectionName: String = "") {
        val adminName = currentUser.value?.let { "${it.prenom} ${it.nom}" } ?: "Admin"
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val trashEntity = com.example.data.GlobalTrashEntity(
            id = entityId,
            originalId = entityId,
            itemType = type,
            title = title.ifBlank { "Élément $entityId ($type)" },
            details = details,
            deletedAt = System.currentTimeMillis(),
            adminResponsible = adminName
        )
        val trashDoc = mapOf(
            "id" to trashEntity.id,
            "originalId" to trashEntity.originalId,
            "itemType" to trashEntity.itemType,
            "title" to trashEntity.title,
            "details" to trashEntity.details,
            "deletedAt" to trashEntity.deletedAt,
            "adminResponsible" to trashEntity.adminResponsible
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertGlobalTrash(trashEntity)
                db.collection("global_trash").document(entityId).set(trashDoc, com.google.firebase.firestore.SetOptions.merge()).await()
                db.collection("corbeille_globale").document(entityId).set(trashDoc, com.google.firebase.firestore.SetOptions.merge()).await()
                
                val colToUpdate = collectionName.ifEmpty { type }
                val collectionsToUpdate = getDualCollectionsFor(colToUpdate)
                for (col in collectionsToUpdate) {
                    try {
                        db.collection(col).document(entityId).set(mapOf("isArchived" to true), com.google.firebase.firestore.SetOptions.merge()).await()
                    } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun removeFromGlobalTrash(entityId: String, originalCollection: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteGlobalTrash(entityId)
                db.collection("global_trash").document(entityId).delete().await()
                db.collection("corbeille_globale").document(entityId).delete().await()
                
                val collectionsToUpdate = getDualCollectionsFor(originalCollection)
                for (col in collectionsToUpdate) {
                    try {
                        db.collection(col).document(entityId).set(mapOf("isArchived" to false), com.google.firebase.firestore.SetOptions.merge()).await()
                    } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun hardDeleteFromFirestore(entityId: String, collectionName: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteGlobalTrash(entityId)
                db.collection("global_trash").document(entityId).delete().await()
                db.collection("corbeille_globale").document(entityId).delete().await()
                
                val collectionsToDelete = getDualCollectionsFor(collectionName)
                for (col in collectionsToDelete) {
                    try {
                        db.collection(col).document(entityId).delete().await()
                    } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun hardDeleteSpecialItem(id: String) { 
        viewModelScope.launch { 
            repository.deleteSpecialItem(id) 
            hardDeleteFromFirestore(id, "catalog")
        } 
    }
    fun hardDeleteGommageItem(id: String) { 
        viewModelScope.launch { 
            repository.deleteGommageItem(id) 
            hardDeleteFromFirestore(id, "catalog")
        } 
    }
    fun hardDeletePromo(id: String) { 
        viewModelScope.launch { 
            repository.deletePromo(id) 
            hardDeleteFromFirestore(id, "promotions")
        } 
    }
    fun hardDeleteRule(id: String) { 
        viewModelScope.launch { 
            repository.deleteRule(id) 
            hardDeleteFromFirestore(id, "automation_rules")
        } 
    }
    fun hardDeleteAntiFraudRule(id: String) { 
        viewModelScope.launch { 
            repository.deleteAntiFraudRule(id) 
            hardDeleteFromFirestore(id, "anti_fraud_rules")
        } 
    }
    fun hardDeleteConsumable(id: String) { 
        viewModelScope.launch { 
            repository.deleteConsumable(id) 
            hardDeleteFromFirestore(id, "stocks")
        } 
    }
    fun archiveOrder(id: String) { 
        viewModelScope.launch { 
            repository.archiveOrder(id) 
            addToGlobalTrash(id, "ORDER", collectionName = "orders")
        } 
    }
    fun hardDeleteOrder(id: String) { 
        viewModelScope.launch { 
            repository.deleteOrder(id) 
            hardDeleteFromFirestore(id, "orders")
        } 
    }
    fun hardDeleteUser(id: String) { 
        viewModelScope.launch { 
            repository.deleteUser(id) 
            hardDeleteFromFirestore(id, "users")
        } 
    }
    fun restoreSpecialItem(id: String) { 
        viewModelScope.launch { 
            repository.restoreSpecialItem(id) 
            removeFromGlobalTrash(id, "catalog")
        } 
    }
    fun restoreGommageItem(id: String) { 
        viewModelScope.launch { 
            repository.restoreGommageItem(id) 
            removeFromGlobalTrash(id, "catalog")
        } 
    }
    fun restorePromo(id: String) { 
        viewModelScope.launch { 
            repository.restorePromo(id) 
            removeFromGlobalTrash(id, "promotions")
        } 
    }
    fun restoreRule(id: String) { 
        viewModelScope.launch { 
            repository.restoreRule(id) 
            removeFromGlobalTrash(id, "automation_rules")
        } 
    }
    fun restoreAntiFraudRule(id: String) { 
        viewModelScope.launch { 
            repository.restoreAntiFraudRule(id) 
            removeFromGlobalTrash(id, "anti_fraud_rules")
        } 
    }
    fun restoreConsumable(id: String) { 
        viewModelScope.launch { 
            repository.restoreConsumable(id) 
            removeFromGlobalTrash(id, "stocks")
        } 
    }
    fun restoreOrder(id: String) { 
        viewModelScope.launch { 
            repository.restoreOrder(id) 
            removeFromGlobalTrash(id, "orders")
        } 
    }
    fun restoreUser(id: String) { 
        viewModelScope.launch { 
            repository.restoreUser(id) 
            removeFromGlobalTrash(id, "users")
        } 
    }
    fun archiveUser(id: String) { 
        viewModelScope.launch { 
            repository.archiveUser(id) 
            addToGlobalTrash(id, "USER", collectionName = "users")
        } 
    }

    fun addAnnouncement(title: String, message: String, imageUrl: String) {
        viewModelScope.launch {
            val announcement = com.example.data.AnnouncementEntity(title = title, message = message, imageUrl = imageUrl)
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "CREATE_ANNOUNCEMENT", "Création de l'annonce $title")
        }
    }
    
    fun updateAnnouncement(announcement: com.example.data.AnnouncementEntity) {
        viewModelScope.launch {
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "UPDATE_ANNOUNCEMENT", "Mise à jour de l'annonce ${announcement.title}")
        }
    }
    
    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            hardDeleteFromFirestore(id, "announcements")
            logAdminAction(currentUser.value?.id, "", "DELETE_ANNOUNCEMENT", "Suppression d'une annonce")
        }
    }
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(repository) as T
    }

}
