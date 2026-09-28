package com.example.data

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data Transfer Object (DTO) pour la synchronisation Firestore et la sérialisation
 * Offline-First (PendingActionEntity) des utilisateurs (Clients et Admins).
 */
@Keep
@IgnoreExtraProperties
data class UserDto(
    val id: String = "",
    val role: String = "CLIENT",
    val nom: String = "",
    val prenom: String = "",
    val email: String = "",
    val telephone: String = "",
    val motDePasse: String = "",
    val coordonnees: String = "",
    val token: String = "",
    val fidelityPointsKg: Float = 0f,
    val myReferralCode: String = "",
    val referredBy: String = "",
    val isSuspended: Boolean = false,
    val canUsePromo: Boolean = true,
    val canUsePromoUntil: Long? = null,
    val canOrder: Boolean = true,
    val suspensionReason: String = "",
    val suspensionUntil: Long? = null,
    val isArchived: Boolean = false,
    val permissions: String = "ALL",
    val isLoyaltyEnabled: Boolean = true,
    val loyaltyValidUntil: Long? = null,

    // --- NOUVEAU MOTEUR DE FIDÉLITÉ DYNAMIQUE CLIENT ---
    val accumulatedFreeKgs: Double = 0.0,
    val currentKgTowardsReward: Double = 0.0,
    val lifetimeKgWashed: Double = 0.0,

    val customAutoBanEnabled: Boolean = false,
    val customAutoBanThreshold: Int = 0,
    val customAutoBanDurationHours: Int = 0,

    val canSelectPieces: Boolean = true,
    val canSelectCategories: Boolean = true,
    val canEstimateKg: Boolean = true,
    val canUseExpress: Boolean = true,
    val canUsePremium: Boolean = true,

    // Schéma dual / legacy
    val prenomAdmin: String = "",
    val nomAdmin: String = "",
    val mailAdmin: String = "",
    val motDePasseAdmin: String = "",
    val roleAdminSuper: Boolean = false,

    val prenomUser: String = "",
    val nomUser: String = "",
    val numeroUser: String = "",
    val mailUser: String = "",
    val adresseUser: String = "",
    val motDePasseUser: String = "",
    val cumulKiloFideliterUser: Double = 0.0
)

fun UserEntity.toDto(): UserDto {
    return UserDto(
        id = this.id,
        role = this.role,
        nom = this.nom,
        prenom = this.prenom,
        email = this.email,
        telephone = this.telephone,
        motDePasse = this.motDePasse,
        coordonnees = this.coordonnees,
        token = this.token,
        fidelityPointsKg = this.fidelityPointsKg,
        myReferralCode = this.myReferralCode,
        referredBy = this.referredBy,
        isSuspended = this.isSuspended,
        canUsePromo = this.canUsePromo,
        canUsePromoUntil = this.canUsePromoUntil,
        canOrder = this.canOrder,
        suspensionReason = this.suspensionReason,
        suspensionUntil = this.suspensionUntil,
        isArchived = this.isArchived,
        permissions = this.permissions,
        isLoyaltyEnabled = this.isLoyaltyEnabled,
        loyaltyValidUntil = this.loyaltyValidUntil,
        accumulatedFreeKgs = this.accumulatedFreeKgs,
        currentKgTowardsReward = this.currentKgTowardsReward,
        lifetimeKgWashed = this.lifetimeKgWashed,
        customAutoBanEnabled = this.customAutoBanEnabled,
        customAutoBanThreshold = this.customAutoBanThreshold,
        customAutoBanDurationHours = this.customAutoBanDurationHours,
        canSelectPieces = this.canSelectPieces,
        canSelectCategories = this.canSelectCategories,
        canEstimateKg = this.canEstimateKg,
        canUseExpress = this.canUseExpress,
        canUsePremium = this.canUsePremium,
        prenomAdmin = this.prenomAdmin,
        nomAdmin = this.nomAdmin,
        mailAdmin = this.mailAdmin,
        motDePasseAdmin = this.motDePasseAdmin,
        roleAdminSuper = this.roleAdminSuper,
        prenomUser = if (this.prenomUser.isNotBlank()) this.prenomUser else this.prenom,
        nomUser = if (this.nomUser.isNotBlank()) this.nomUser else this.nom,
        numeroUser = if (this.numeroUser.isNotBlank()) this.numeroUser else this.telephone,
        mailUser = if (this.mailUser.isNotBlank()) this.mailUser else this.email,
        adresseUser = if (this.adresseUser.isNotBlank()) this.adresseUser else this.coordonnees,
        motDePasseUser = if (this.motDePasseUser.isNotBlank()) this.motDePasseUser else this.motDePasse,
        cumulKiloFideliterUser = this.cumulKiloFideliterUser
    )
}

fun UserDto.toEntity(): UserEntity {
    return UserEntity(
        id = this.id,
        role = this.role,
        nom = if (this.nom.isNotBlank()) this.nom else this.nomUser,
        prenom = if (this.prenom.isNotBlank()) this.prenom else this.prenomUser,
        email = if (this.email.isNotBlank()) this.email else this.mailUser,
        telephone = if (this.telephone.isNotBlank()) this.telephone else this.numeroUser,
        motDePasse = if (this.motDePasse.isNotBlank()) this.motDePasse else this.motDePasseUser,
        coordonnees = if (this.coordonnees.isNotBlank()) this.coordonnees else this.adresseUser,
        token = this.token,
        fidelityPointsKg = this.fidelityPointsKg,
        myReferralCode = this.myReferralCode,
        referredBy = this.referredBy,
        isSuspended = this.isSuspended,
        canUsePromo = this.canUsePromo,
        canUsePromoUntil = this.canUsePromoUntil,
        canOrder = this.canOrder,
        suspensionReason = this.suspensionReason,
        suspensionUntil = this.suspensionUntil,
        isArchived = this.isArchived,
        permissions = this.permissions,
        isLoyaltyEnabled = this.isLoyaltyEnabled,
        loyaltyValidUntil = this.loyaltyValidUntil,
        accumulatedFreeKgs = this.accumulatedFreeKgs,
        currentKgTowardsReward = this.currentKgTowardsReward,
        lifetimeKgWashed = this.lifetimeKgWashed,
        customAutoBanEnabled = this.customAutoBanEnabled,
        customAutoBanThreshold = this.customAutoBanThreshold,
        customAutoBanDurationHours = this.customAutoBanDurationHours,
        canSelectPieces = this.canSelectPieces,
        canSelectCategories = this.canSelectCategories,
        canEstimateKg = this.canEstimateKg,
        canUseExpress = this.canUseExpress,
        canUsePremium = this.canUsePremium,
        prenomAdmin = this.prenomAdmin,
        nomAdmin = this.nomAdmin,
        mailAdmin = this.mailAdmin,
        motDePasseAdmin = this.motDePasseAdmin,
        roleAdminSuper = this.roleAdminSuper,
        prenomUser = this.prenomUser,
        nomUser = this.nomUser,
        numeroUser = this.numeroUser,
        mailUser = this.mailUser,
        adresseUser = this.adresseUser,
        motDePasseUser = this.motDePasseUser,
        cumulKiloFideliterUser = this.cumulKiloFideliterUser
    )
}
