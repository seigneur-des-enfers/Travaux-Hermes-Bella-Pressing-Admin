with open('app/src/main/java/com/example/data/FirestoreSyncManager.kt', 'r') as f:
    content = f.read()

import re

old_push_order = """    private suspend fun pushOrderToFirestore(order: OrderEntity) {
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
            db.collection("ordres").document(order.id).set(orderMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }"""

new_push_order = """    private suspend fun pushOrderToFirestore(order: OrderEntity) {
        val db = firestore ?: return
        try {
            // ONLY push fields that the admin is allowed to modify to prevent overwriting client data
            val orderMap = mutableMapOf<String, Any?>(
                "kg" to order.kg, 
                "status" to order.status, 
                "prixFinal" to order.prixFinal,
                "timelineStep" to order.timelineStep, 
                "adminMessage" to order.adminMessage,
                "isArchived" to order.isArchived
            )
            // Optionally update transportFee or other admin-managed fields if they change
            if (order.transportFee > 0) orderMap["transportFee"] = order.transportFee
            if (order.pickupDate.isNotEmpty()) orderMap["pickupDate"] = order.pickupDate
            if (order.deliveryDate.isNotEmpty()) orderMap["deliveryDate"] = order.deliveryDate
            
            // Critical: Never erase userId. Only push it if we have it locally just in case.
            if (order.userId.isNotEmpty()) {
                orderMap["userId"] = order.userId
            }
            
            db.collection("ordres").document(order.id).set(orderMap, SetOptions.merge()).await()
        } catch (e: Throwable) { e.printStackTrace() }
    }"""

if old_push_order in content:
    content = content.replace(old_push_order, new_push_order)
else:
    print("Could not find old_push_order!")

with open('app/src/main/java/com/example/data/FirestoreSyncManager.kt', 'w') as f:
    f.write(content)
