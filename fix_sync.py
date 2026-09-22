with open('app/src/main/java/com/example/data/FirestoreSyncManager.kt', 'r') as f:
    content = f.read()

import re

# Update pushUserToFirestore
user_map_old = """            val userMap = mapOf(
                "nom" to user.nom, "prenom" to user.prenom, "email" to user.email, "role" to user.role,
                "telephone" to user.telephone, "motDePasse" to user.motDePasse, "coordonnees" to user.coordonnees,
                "token" to user.token, "fidelityPointsKg" to user.fidelityPointsKg,
                "myReferralCode" to user.myReferralCode, "referredBy" to user.referredBy,
                "isSuspended" to user.isSuspended, "canUsePromo" to user.canUsePromo,
                "canOrder" to user.canOrder, "suspensionReason" to user.suspensionReason,
                "isArchived" to user.isArchived, "permissions" to user.permissions
            )"""
            
user_map_new = """            val userMap = mutableMapOf<String, Any?>(
                "nom" to user.nom, "prenom" to user.prenom, "email" to user.email, "role" to user.role,
                "telephone" to user.telephone, "motDePasse" to user.motDePasse, "coordonnees" to user.coordonnees,
                "token" to user.token, "fidelityPointsKg" to user.fidelityPointsKg,
                "myReferralCode" to user.myReferralCode, "referredBy" to user.referredBy,
                "isSuspended" to user.isSuspended, "canUsePromo" to user.canUsePromo,
                "canUsePromoUntil" to user.canUsePromoUntil, "suspensionUntil" to user.suspensionUntil,
                "canOrder" to user.canOrder, "suspensionReason" to user.suspensionReason,
                "isArchived" to user.isArchived, "permissions" to user.permissions,
                "isLoyaltyEnabled" to user.isLoyaltyEnabled, "loyaltyValidUntil" to user.loyaltyValidUntil,
                "customAutoBanEnabled" to user.customAutoBanEnabled,
                "customAutoBanThreshold" to user.customAutoBanThreshold,
                "customAutoBanDurationHours" to user.customAutoBanDurationHours,
                "canSelectPieces" to user.canSelectPieces,
                "canSelectCategories" to user.canSelectCategories,
                "canEstimateKg" to user.canEstimateKg,
                "canUseExpress" to user.canUseExpress,
                "canUsePremium" to user.canUsePremium
            )"""
content = content.replace(user_map_old, user_map_new)

# Update pushConfigToFirestore
config_map_old = """            val configMap = mapOf(
                "baseTransportFee" to config.baseTransportFee, "expressMultiplier" to config.expressMultiplier,
                "fidelityThresholdKg" to config.fidelityThresholdKg, "fidelityRewardValue" to config.fidelityRewardValue,
                "pricePerKm" to config.pricePerKm, "pressingLat" to config.pressingLat, "pressingLng" to config.pressingLng,
                "referralRewardKg" to config.referralRewardKg, "appDownloadLink" to config.appDownloadLink,
                "autoBanEnabled" to config.autoBanEnabled, "autoBanThreshold" to config.autoBanThreshold,
                "autoBanDurationHours" to config.autoBanDurationHours, "isLoyaltyEnabled" to config.isLoyaltyEnabled,
                "categories" to config.categories, "businessName" to config.businessName,
                "contactPhone" to config.contactPhone, "contactEmail" to config.contactEmail,
                "currencySymbol" to config.currencySymbol, "taxPercentage" to config.taxPercentage,
                "openingHours" to config.openingHours, "closingHours" to config.closingHours,
                "openingDays" to config.openingDays, "closingDays" to config.closingDays,
                "termsAndConditionsLink" to config.termsAndConditionsLink
            )"""
            
config_map_new = """            val configMap = mutableMapOf<String, Any?>(
                "baseTransportFee" to config.baseTransportFee, "expressMultiplier" to config.expressMultiplier,
                "fidelityThresholdKg" to config.fidelityThresholdKg, "fidelityRewardValue" to config.fidelityRewardValue,
                "pricePerKm" to config.pricePerKm, "pressingLat" to config.pressingLat, "pressingLng" to config.pressingLng,
                "referralRewardKg" to config.referralRewardKg, "appDownloadLink" to config.appDownloadLink,
                "autoBanEnabled" to config.autoBanEnabled, "autoBanThreshold" to config.autoBanThreshold,
                "autoBanDurationHours" to config.autoBanDurationHours, "isLoyaltyEnabled" to config.isLoyaltyEnabled,
                "loyaltyValidUntil" to config.loyaltyValidUntil,
                "categories" to config.categories, "businessName" to config.businessName,
                "contactPhone" to config.contactPhone, "contactEmail" to config.contactEmail,
                "currencySymbol" to config.currencySymbol, "taxPercentage" to config.taxPercentage,
                "openingHours" to config.openingHours, "closingHours" to config.closingHours,
                "openingDays" to config.openingDays, "closingDays" to config.closingDays,
                "termsAndConditionsLink" to config.termsAndConditionsLink
            )"""
content = content.replace(config_map_old, config_map_new)

# Add new push methods
new_methods = """
    private suspend fun pushSpecialItemToFirestore(item: SpecialItemEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf("name" to item.name, "price" to item.price, "isArchived" to item.isArchived)
            db.collection("special_items").document(item.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushConsumableToFirestore(c: ConsumableEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf("name" to c.name, "quantity" to c.quantity, "threshold" to c.threshold, "isPublic" to c.isPublic, "isArchived" to c.isArchived)
            db.collection("consumables").document(c.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushRuleToFirestore(r: RuleEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf("name" to r.name, "triggerCondition" to r.triggerCondition, "actionType" to r.actionType, "actionValue" to r.actionValue, "constraints" to r.constraints, "isActive" to r.isActive, "isPermanent" to r.isPermanent, "validUntil" to r.validUntil, "isArchived" to r.isArchived)
            db.collection("rules").document(r.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushAntiFraudRuleToFirestore(r: AntiFraudRuleEntity) {
        val db = firestore ?: return
        try {
            val map = mapOf("name" to r.name, "triggerEvent" to r.triggerEvent, "threshold" to r.threshold, "durationHours" to r.durationHours, "action" to r.action, "isActive" to r.isActive, "isArchived" to r.isArchived)
            db.collection("anti_fraud_rules").document(r.id).set(map, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }
}
"""

content = content.replace('}\n}', '}\n' + new_methods)


# Add the new listeners inside startSync
new_listeners = """
        scope.launch {
            repository.getAllSpecialItems().collectLatest { items ->
                for (item in items) { pushSpecialItemToFirestore(item) }
            }
        }
        scope.launch {
            repository.getAllConsumables().collectLatest { items ->
                for (item in items) { pushConsumableToFirestore(item) }
            }
        }
        scope.launch {
            repository.getAllRules().collectLatest { items ->
                for (item in items) { pushRuleToFirestore(item) }
            }
        }
        scope.launch {
            repository.getAllAntiFraudRules().collectLatest { items ->
                for (item in items) { pushAntiFraudRuleToFirestore(item) }
            }
        }
    }
"""

content = content.replace('        }\n    }\n\n    private suspend fun pushOrderToFirestore', new_listeners + '\n    private suspend fun pushOrderToFirestore')


with open('app/src/main/java/com/example/data/FirestoreSyncManager.kt', 'w') as f:
    f.write(content)
