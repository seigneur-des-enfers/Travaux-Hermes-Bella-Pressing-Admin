with open('app/src/main/java/com/example/data/FirestoreSyncManager.kt', 'w') as f:
    f.write("""package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val repository: AppRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null

    fun startSync() {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e("FirestoreSyncManager", "Firebase not initialized", e)
            return
        }
        val db = firestore ?: return

        // --- 1. PULL FROM FIRESTORE (CLIENT CHANGES) ---
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            scope.launch {
                for (doc in snapshot.documents) {
                    try {
                        val user = UserEntity(
                            id = doc.id,
                            nom = doc.getString("nom") ?: "",
                            prenom = doc.getString("prenom") ?: "",
                            email = doc.getString("email") ?: "",
                            motDePasse = doc.getString("motDePasse") ?: "",
                            role = doc.getString("role") ?: "CLIENT",
                            telephone = doc.getString("telephone") ?: "",
                            coordonnees = doc.getString("coordonnees") ?: "",
                            token = doc.getString("token") ?: "",
                            fidelityPointsKg = doc.getDouble("fidelityPointsKg")?.toFloat() ?: 0f,
                            myReferralCode = doc.getString("myReferralCode") ?: "",
                            referredBy = doc.getString("referredBy") ?: "",
                            isSuspended = doc.getBoolean("isSuspended") ?: false,
                            canUsePromo = doc.getBoolean("canUsePromo") ?: true,
                            canOrder = doc.getBoolean("canOrder") ?: true,
                            suspensionReason = doc.getString("suspensionReason") ?: "",
                            isArchived = doc.getBoolean("isArchived") ?: false,
                            permissions = doc.getString("permissions") ?: "ALL"
                        )
                        repository.insertUser(user)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        db.collection("orders").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            scope.launch {
                for (doc in snapshot.documents) {
                    try {
                        val order = OrderEntity(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            pieces = doc.getLong("pieces")?.toInt() ?: 0,
                            categorie = doc.getString("categorie") ?: "",
                            kg = doc.getDouble("kg")?.toFloat() ?: 0f,
                            lavageType = doc.getString("lavageType") ?: "",
                            gommagePaire = doc.getLong("gommagePaire")?.toInt() ?: 0,
                            gommageImpair = doc.getLong("gommageImpair")?.toInt() ?: 0,
                            prixFinal = doc.getDouble("prixFinal")?.toFloat() ?: 0f,
                            status = doc.getString("status") ?: "EN_ATTENTE",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            adminMessage = doc.getString("adminMessage") ?: "",
                            rating = doc.getLong("rating")?.toInt() ?: 0,
                            clientComment = doc.getString("clientComment") ?: "",
                            adresseLivraison = doc.getString("adresseLivraison") ?: "",
                            isExpress = doc.getBoolean("isExpress") ?: false,
                            instructionsSpeciales = doc.getString("instructionsSpeciales") ?: "",
                            deliveryType = doc.getString("deliveryType") ?: "AGENCE",
                            transportFee = doc.getDouble("transportFee")?.toFloat() ?: 0f,
                            timelineStep = doc.getLong("timelineStep")?.toInt() ?: 0,
                            specialItemsData = doc.getString("specialItemsData") ?: "",
                            pickupDate = doc.getString("pickupDate") ?: "",
                            deliveryDate = doc.getString("deliveryDate") ?: "",
                            photoUri = doc.getString("photoUri") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            distanceKm = doc.getDouble("distanceKm")?.toFloat() ?: 0f,
                            promoCode = doc.getString("promoCode") ?: "",
                            discountPercentage = doc.getDouble("discountPercentage")?.toFloat() ?: 0f,
                            discountFixed = doc.getDouble("discountFixed")?.toFloat() ?: 0f,
                            hasPremiumGuarantee = doc.getBoolean("hasPremiumGuarantee") ?: false,
                            insuranceFee = doc.getDouble("insuranceFee")?.toFloat() ?: 0f,
                            isArchived = doc.getBoolean("isArchived") ?: false
                        )
                        repository.insertOrder(order)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        // --- 2. PUSH TO FIRESTORE (ADMIN CHANGES) ---
        scope.launch {
            repository.getAllOrders().collectLatest { orders ->
                for (order in orders) { pushOrderToFirestore(order) }
            }
        }
        
        scope.launch {
            repository.getAllClients().collectLatest { clients ->
                for (client in clients) { pushUserToFirestore(client) }
            }
        }

        scope.launch {
            repository.getConfig().collectLatest { config ->
                if (config != null) pushConfigToFirestore(config)
            }
        }

        scope.launch {
            repository.getAllPromos().collectLatest { promos ->
                for (promo in promos) { pushPromoToFirestore(promo) }
            }
        }

        scope.launch {
            repository.allAnnouncements.collectLatest { announcements ->
                for (ann in announcements) { pushAnnouncementToFirestore(ann) }
            }
        }

        scope.launch {
            repository.getAllGommageItems().collectLatest { items ->
                for (item in items) { pushGommageItemToFirestore(item) }
            }
        }
        
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

    private suspend fun pushOrderToFirestore(order: OrderEntity) {
        val db = firestore ?: return
        try {
            val orderMap = mapOf(
                "userId" to order.userId, "pieces" to order.pieces, "categorie" to order.categorie,
                "kg" to order.kg, "lavageType" to order.lavageType, "gommagePaire" to order.gommagePaire,
                "gommageImpair" to order.gommageImpair, "prixFinal" to order.prixFinal, "status" to order.status,
                "createdAt" to order.createdAt, "adminMessage" to order.adminMessage, "rating" to order.rating,
                "clientComment" to order.clientComment, "adresseLivraison" to order.adresseLivraison,
                "isExpress" to order.isExpress, "instructionsSpeciales" to order.instructionsSpeciales,
                "deliveryType" to order.deliveryType, "transportFee" to order.transportFee,
                "timelineStep" to order.timelineStep, "specialItemsData" to order.specialItemsData,
                "pickupDate" to order.pickupDate, "deliveryDate" to order.deliveryDate,
                "photoUri" to order.photoUri, "latitude" to order.latitude, "longitude" to order.longitude,
                "distanceKm" to order.distanceKm, "promoCode" to order.promoCode,
                "discountPercentage" to order.discountPercentage, "discountFixed" to order.discountFixed,
                "hasPremiumGuarantee" to order.hasPremiumGuarantee, "insuranceFee" to order.insuranceFee,
                "isArchived" to order.isArchived
            )
            db.collection("orders").document(order.id).set(orderMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushUserToFirestore(user: UserEntity) {
        val db = firestore ?: return
        try {
            val userMap = mutableMapOf<String, Any?>(
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
            )
            db.collection("users").document(user.id).set(userMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushConfigToFirestore(config: ConfigEntity) {
        val db = firestore ?: return
        try {
            val configMap = mutableMapOf<String, Any?>(
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
            )
            db.collection("config").document("main").set(configMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushPromoToFirestore(promo: PromoEntity) {
        val db = firestore ?: return
        try {
            val promoMap = mapOf(
                "code" to promo.code, "description" to promo.description, "discountPercent" to promo.discountPercent,
                "freeKg" to promo.freeKg, "minOrderValue" to promo.minOrderValue, "isActive" to promo.isActive,
                "validUntil" to promo.validUntil, "isPermanent" to promo.isPermanent,
                "targetItems" to promo.targetItems, "discountType" to promo.discountType, "isArchived" to promo.isArchived
            )
            db.collection("promos").document(promo.id).set(promoMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushAnnouncementToFirestore(ann: AnnouncementEntity) {
        val db = firestore ?: return
        try {
            val annMap = mapOf(
                "title" to ann.title, "message" to ann.message, "imageUrl" to ann.imageUrl,
                "isVisible" to ann.isVisible, "createdAt" to ann.createdAt
            )
            db.collection("announcements").document(ann.id).set(annMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

    private suspend fun pushGommageItemToFirestore(item: GommageItemEntity) {
        val db = firestore ?: return
        try {
            val itemMap = mapOf("name" to item.name, "price" to item.price, "isArchived" to item.isArchived)
            db.collection("gommage_items").document(item.id).set(itemMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }

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
""")
