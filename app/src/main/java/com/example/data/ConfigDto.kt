package com.example.data

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data Transfer Object (DTO) pour la synchronisation Firestore et la sérialisation
 * Offline-First (PendingActionEntity) de la configuration générale et du programme de fidélité.
 */
@Keep
@IgnoreExtraProperties
data class ConfigDto(
    val id: String = "global_config",
    val baseTransportFee: Float = 1000f,
    val expressMultiplier: Float = 1.3f,
    val fidelityThresholdKg: Float = 10f,
    val fidelityRewardValue: Float = 350f,
    val pricePerKm: Float = 500f,
    val pressingLat: Double = 12.6392,
    val pressingLng: Double = -8.0029,
    val referralRewardKg: Float = 2f,
    val appDownloadLink: String = "",
    val autoBanEnabled: Boolean = false,
    val autoBanThreshold: Int = 3,
    val autoBanDurationHours: Int = 24,
    val isLoyaltyEnabled: Boolean = true,
    val loyaltyValidUntil: Long? = null,
    
    // --- MODE MAINTENANCE ET SECOURS ---
    val isMaintenance: Boolean = false,
    val acceptNewOrders: Boolean = true,

    val categories: String = "Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons",
    val businessName: String = "Bella Pressing",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val currencySymbol: String = "FCFA",
    val taxPercentage: Float = 0f,
    val openingHours: String = "",
    val closingHours: String = "",
    val openingDays: String = "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
    val closingDays: String = "Dimanche",
    val termsAndConditionsLink: String = "",

    val tarifBaseTransort: Double = 1000.0,
    val prixParKilo: Double = 350.0,

    val nomEntreprise: String = "BELLA — PRESSING",
    val numeroEntreprise: String = "",
    val emailEntreprise: String = "",
    val openDays: String = "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
    val openHours: String = "",
    val closeHours: String = "",
    val linkCGU: String = "",

    // --- PROGRAMME DE FIDÉLITÉ DYNAMIQUE: KILOS GRATUITS & EXPIRATION ---
    val isLoyaltyProgramEnabled: Boolean = true,
    val loyaltyKgThreshold: Double = 10.0,
    val loyaltyKgReward: Double = 1.0,
    val loyaltyMaxFreeKgs: Double = 30.0,
    val loyaltyExpirationDays: Int = 90
)

fun ConfigEntity.toDto(): ConfigDto {
    return ConfigDto(
        id = this.id,
        baseTransportFee = this.baseTransportFee,
        expressMultiplier = this.expressMultiplier,
        fidelityThresholdKg = this.fidelityThresholdKg,
        fidelityRewardValue = this.fidelityRewardValue,
        pricePerKm = this.pricePerKm,
        pressingLat = this.pressingLat,
        pressingLng = this.pressingLng,
        referralRewardKg = this.referralRewardKg,
        appDownloadLink = this.appDownloadLink,
        autoBanEnabled = this.autoBanEnabled,
        autoBanThreshold = this.autoBanThreshold,
        autoBanDurationHours = this.autoBanDurationHours,
        isLoyaltyEnabled = this.isLoyaltyEnabled,
        loyaltyValidUntil = this.loyaltyValidUntil,
        isMaintenance = this.isMaintenance,
        acceptNewOrders = this.acceptNewOrders,
        categories = this.categories,
        businessName = this.businessName,
        contactPhone = this.contactPhone,
        contactEmail = this.contactEmail,
        currencySymbol = this.currencySymbol,
        taxPercentage = this.taxPercentage,
        openingHours = this.openingHours,
        closingHours = this.closingHours,
        openingDays = this.openingDays,
        closingDays = this.closingDays,
        termsAndConditionsLink = this.termsAndConditionsLink,
        tarifBaseTransort = this.tarifBaseTransort,
        prixParKilo = this.prixParKilo,
        nomEntreprise = this.nomEntreprise,
        numeroEntreprise = this.numeroEntreprise,
        emailEntreprise = this.emailEntreprise,
        openDays = this.openDays,
        openHours = this.openHours,
        closeHours = this.closeHours,
        linkCGU = this.linkCGU,
        isLoyaltyProgramEnabled = this.isLoyaltyProgramEnabled,
        loyaltyKgThreshold = this.loyaltyKgThreshold,
        loyaltyKgReward = this.loyaltyKgReward,
        loyaltyMaxFreeKgs = this.loyaltyMaxFreeKgs,
        loyaltyExpirationDays = this.loyaltyExpirationDays
    )
}

fun ConfigDto.toEntity(): ConfigEntity {
    return ConfigEntity(
        id = this.id,
        baseTransportFee = this.baseTransportFee,
        expressMultiplier = this.expressMultiplier,
        fidelityThresholdKg = this.fidelityThresholdKg,
        fidelityRewardValue = this.fidelityRewardValue,
        pricePerKm = this.pricePerKm,
        pressingLat = this.pressingLat,
        pressingLng = this.pressingLng,
        referralRewardKg = this.referralRewardKg,
        appDownloadLink = this.appDownloadLink,
        autoBanEnabled = this.autoBanEnabled,
        autoBanThreshold = this.autoBanThreshold,
        autoBanDurationHours = this.autoBanDurationHours,
        isLoyaltyEnabled = this.isLoyaltyEnabled,
        loyaltyValidUntil = this.loyaltyValidUntil,
        isMaintenance = this.isMaintenance,
        acceptNewOrders = this.acceptNewOrders,
        categories = this.categories,
        businessName = this.businessName,
        contactPhone = this.contactPhone,
        contactEmail = this.contactEmail,
        currencySymbol = this.currencySymbol,
        taxPercentage = this.taxPercentage,
        openingHours = this.openingHours,
        closingHours = this.closingHours,
        openingDays = this.openingDays,
        closingDays = this.closingDays,
        termsAndConditionsLink = this.termsAndConditionsLink,
        tarifBaseTransort = this.tarifBaseTransort,
        prixParKilo = this.prixParKilo,
        nomEntreprise = this.nomEntreprise,
        numeroEntreprise = this.numeroEntreprise,
        emailEntreprise = this.emailEntreprise,
        openDays = this.openDays,
        openHours = this.openHours,
        closeHours = this.closeHours,
        linkCGU = this.linkCGU,
        isLoyaltyProgramEnabled = this.isLoyaltyProgramEnabled,
        loyaltyKgThreshold = this.loyaltyKgThreshold,
        loyaltyKgReward = this.loyaltyKgReward,
        loyaltyMaxFreeKgs = this.loyaltyMaxFreeKgs,
        loyaltyExpirationDays = this.loyaltyExpirationDays
    )
}
