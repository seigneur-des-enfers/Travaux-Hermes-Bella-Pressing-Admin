package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data Transfer Object (DTO) pour la synchronisation Firestore des commandes.
 * Représente la structure des documents dans les collections 'orders' et 'ordres'.
 */
@Keep
@IgnoreExtraProperties
data class OrderDto(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val status: String = "EN_ATTENTE", // EN_ATTENTE, ACCEPTE, EN_COURS, PRET, EN_LIVRAISON, LIVRE, TERMINE, ANNULE, REJETE
    val weight: Double = 0.0,
    val totalPrice: Double = 0.0,
    val adminNotes: String = "",
    val deliveryBoyId: String = "",
    val photoUri: String = "",
    val pieces: Int = 0,
    val categorie: String = "",
    val lavageType: String = "SIMPLE",
    val isExpress: Boolean = false,
    val instructionsSpeciales: String = "",
    val deliveryType: String = "AGENCE", // AGENCE ou DOMICILE
    val transportFee: Float = 0f,
    val timelineStep: Int = 0,
    val specialItemsData: String = "",
    val pickupDate: String = "",
    val deliveryDate: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceKm: Float = 0f,
    val promoCode: String = "",
    val discountPercentage: Float = 0f,
    val discountFixed: Float = 0f,
    val hasPremiumGuarantee: Boolean = false,
    val insuranceFee: Float = 0f,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // Données de traitement et fidélité
    val autoCompletionInfos: String = "",
    val nombrePieces: Double = 0.0,
    val choixCategory: String = "",
    val appareilPhoto: String = "",
    val lavageSimple: Boolean = false,
    val lavageSechagePliage: Boolean = false,
    val lavageSechagePliageRepassage: Boolean = false,
    val articlesUniquement: Boolean = false,
    val choixArticleSpeciaux: String = "",
    val choixArticleGommage: String = "",
    val estimationKilo: Double = 0.0,
    val depotAgence: Boolean = false,
    val dateHeureDepot: String = "",
    val dateHeureRetrait: String = "",
    val ramassage: Boolean = false,
    val coordonneesMaps: String = "",
    val instructionsSpecial: String = "",
    val serviceExpress: Boolean = false,
    val garantiVIP: Boolean = false,
    val saisiCodePromos: String = "",
    val prixEstimerAutoCalcul: Double = 0.0,
    val orderRecu: Boolean = false,
    val enPleinLavage: Boolean = false,
    val enSechage: Boolean = false,
    val orderPret: Boolean = false,
    val orderLivree: Boolean = false,
    val avisUsers: String = "",
    val fidelityKgUsed: Float = 0f,
    val fideliteKgUtilise: Double = 0.0,
    val fidelityCredited: Boolean = false,
    val fideliteCredite: Boolean = false,
    val usedFreeKgs: Double = 0.0,
    val isLoyaltyPointsGranted: Boolean = false
)

/**
 * Utilitaires de conversion et de mapping DTO <-> Entity.
 */
object OrderMapper {

    /**
     * Convertit de manière sécurisée tout type Firestore (Timestamp, Long, Number, String) en Long.
     */
    fun parseTimestampToLong(value: Any?): Long {
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }

    /**
     * Mappe un document Firestore vers une OrderEntity Room en gérant la nullabilité
     * et l'interopérabilité des clés (anglais/français, legacy/nouveau).
     */
    fun fromDocument(doc: DocumentSnapshot): OrderEntity {
        val safeWeight = (doc.get("weight") as? Number)?.toDouble() ?: 0.0
        val rawWeight = if (safeWeight > 0.0) safeWeight else {
            (doc.get("kg") as? Number)?.toDouble()
                ?: (doc.get("poids") as? Number)?.toDouble()
                ?: (doc.get("poidsApresPeser") as? Number)?.toDouble()
                ?: (doc.get("estimationKilo") as? Number)?.toDouble()
                ?: 0.0
        }

        val safeTotalPrice = (doc.get("totalPrice") as? Number)?.toDouble() ?: 0.0
        val rawPrice = if (safeTotalPrice > 0.0) safeTotalPrice else {
            (doc.get("prixFinal") as? Number)?.toDouble()
                ?: (doc.get("prix") as? Number)?.toDouble()
                ?: (doc.get("prixEstimerAutoCalcul") as? Number)?.toDouble()
                ?: 0.0
        }

        val rawCustName = doc.getString("customerName")
            ?: doc.getString("clientNom")
            ?: doc.getString("nomClient")
            ?: doc.getString("nomUser")
            ?: ""

        val rawCustPhone = doc.getString("customerPhone")
            ?: doc.getString("clientTelephone")
            ?: doc.getString("telephone")
            ?: doc.getString("numeroUser")
            ?: ""

        val rawAddress = doc.getString("deliveryAddress")
            ?: doc.getString("adresseLivraison")
            ?: doc.getString("adresseUser")
            ?: doc.getString("coordonnees")
            ?: ""

        val rawAdminNotes = doc.getString("adminNotes")
            ?: doc.getString("adminMessage")
            ?: doc.getString("notesAdmin")
            ?: ""

        val rawDeliveryBoy = doc.getString("deliveryBoyId")
            ?: doc.getString("livreurId")
            ?: ""

        val rawPhoto = doc.getString("photoUri")
            ?: doc.getString("imageUrl")
            ?: doc.getString("photoUrl")
            ?: doc.getString("appareilPhoto")
            ?: ""

        val createdAtMillis = parseTimestampToLong(doc.get("createdAt") ?: doc.get("dateCreation") ?: doc.get("timestamp"))

        val rawStatus = doc.getString("status") ?: doc.getString("statut") ?: "EN_ATTENTE"
        val clientAccepted = doc.getBoolean("clientAccepted") == true ||
            doc.getBoolean("accordClient") == true ||
            doc.getBoolean("clientValidation") == true ||
            doc.getBoolean("validationClient") == true ||
            doc.getBoolean("propositionAcceptee") == true

        val clientRefused = doc.getBoolean("clientRefused") == true ||
            doc.getBoolean("refusClient") == true ||
            doc.getBoolean("propositionRefusee") == true

        val statusVal = when {
            clientRefused -> "REJETE"
            clientAccepted && (rawStatus.equals("PROPOSITION_ADMIN", ignoreCase = true) || rawStatus.equals("EN_ATTENTE", ignoreCase = true)) -> "ACCEPTE"
            else -> when (rawStatus.uppercase().trim()) {
                "ACCEPTE", "ACCEPTEE", "ACCEPTÉ", "ACCEPTÉE", "ACCEPTED", "VALIDEE", "VALIDÉE", "CONFIRMEE", "CONFIRMÉE", "CLIENT_ACCEPTE", "PROPOSITION_ACCEPTEE" -> "ACCEPTE"
                "REJETE", "REJETEE", "REJETÉ", "REJETÉE", "REJECTED", "ANNULE", "ANNULÉE", "REFUSE", "REFUSEE", "REFUSÉE", "PROPOSITION_REFUSEE" -> "REJETE"
                "EN_COURS", "IN_PROGRESS", "LAVAGE" -> "EN_COURS"
                "PRET", "PRÊT", "PRETE", "PRÊTE", "READY" -> "PRET"
                "TERMINE", "TERMINEE", "TERMINÉ", "TERMINÉE", "LIVRE", "LIVREE", "LIVRÉ", "LIVRÉE", "FINISHED", "DONE" -> "TERMINE"
                "PROPOSITION_ADMIN", "DEVIS_ENVOYE", "ATTENTE_CLIENT" -> "PROPOSITION_ADMIN"
                else -> rawStatus
            }
        }

        return OrderEntity(
            id = doc.id,
            userId = doc.getString("userId") ?: doc.getString("idClient") ?: "",
            pieces = (doc.get("pieces") as? Number)?.toInt() ?: (doc.get("nombrePieces") as? Number)?.toInt() ?: 0,
            categorie = doc.getString("categorie") ?: doc.getString("choixCategory") ?: "",
            kg = rawWeight.toFloat(),
            lavageType = doc.getString("lavageType") ?: "SIMPLE",
            prixFinal = rawPrice.toFloat(),
            status = statusVal,
            createdAt = createdAtMillis,
            adminMessage = rawAdminNotes,
            rating = (doc.get("rating") as? Number)?.toInt() ?: 0,
            clientComment = doc.getString("clientComment") ?: doc.getString("avisUsers") ?: "",
            adresseLivraison = rawAddress,
            isExpress = doc.getBoolean("isExpress") ?: doc.getBoolean("serviceExpress") ?: false,
            instructionsSpeciales = doc.getString("instructionsSpeciales") ?: doc.getString("instructionsSpecial") ?: "",
            deliveryType = doc.getString("deliveryType") ?: if (doc.getBoolean("ramassage") == true) "DOMICILE" else "AGENCE",
            transportFee = (doc.get("transportFee") as? Number)?.toFloat() ?: 0f,
            timelineStep = (doc.get("timelineStep") as? Number)?.toInt() ?: 0,
            specialItemsData = doc.getString("specialItemsData") ?: "",
            pickupDate = doc.getString("pickupDate") ?: doc.getString("dateHeureDepot") ?: "",
            deliveryDate = doc.getString("deliveryDate") ?: doc.getString("dateHeureRetrait") ?: "",
            photoUri = rawPhoto,
            latitude = (doc.get("latitude") as? Number)?.toDouble() ?: 0.0,
            longitude = (doc.get("longitude") as? Number)?.toDouble() ?: 0.0,
            distanceKm = (doc.get("distanceKm") as? Number)?.toFloat() ?: 0f,
            promoCode = doc.getString("promoCode") ?: doc.getString("saisiCodePromos") ?: "",
            discountPercentage = (doc.get("discountPercentage") as? Number)?.toFloat() ?: 0f,
            discountFixed = (doc.get("discountFixed") as? Number)?.toFloat() ?: 0f,
            hasPremiumGuarantee = doc.getBoolean("hasPremiumGuarantee") ?: doc.getBoolean("garantiVIP") ?: false,
            insuranceFee = (doc.get("insuranceFee") as? Number)?.toFloat() ?: 0f,
            isArchived = doc.getBoolean("isArchived") ?: false,

            // Champs étendus
            autoCompletionInfos = doc.getString("autoCompletionInfos") ?: "",
            nombrePieces = (doc.get("nombrePieces") as? Number)?.toDouble() ?: 0.0,
            choixCategory = doc.getString("choixCategory") ?: "",
            appareilPhoto = rawPhoto,
            lavageSimple = doc.getBoolean("lavageSimple") ?: false,
            lavageSechagePliage = doc.getBoolean("lavageSechagePliage") ?: false,
            lavageSechagePliageRepassage = doc.getBoolean("lavageSechagePliageRepassage") ?: false,
            articlesUniquement = doc.getBoolean("articlesUniquement") ?: false,
            choixArticleSpeciaux = doc.getString("choixArticleSpeciaux") ?: "",
            choixArticleGommage = doc.getString("choixArticleGommage") ?: "",
            estimationKilo = (doc.get("estimationKilo") as? Number)?.toDouble() ?: 0.0,
            depotAgence = doc.getBoolean("depotAgence") ?: false,
            dateHeureDepot = doc.getString("dateHeureDepot") ?: "",
            dateHeureRetrait = doc.getString("dateHeureRetrait") ?: "",
            ramassage = doc.getBoolean("ramassage") ?: false,
            coordonneesMaps = doc.getString("coordonneesMaps") ?: "",
            instructionsSpecial = doc.getString("instructionsSpecial") ?: "",
            serviceExpress = doc.getBoolean("serviceExpress") ?: false,
            garantiVIP = doc.getBoolean("garantiVIP") ?: false,
            saisiCodePromos = doc.getString("saisiCodePromos") ?: "",
            prixEstimerAutoCalcul = (doc.get("prixEstimerAutoCalcul") as? Number)?.toDouble() ?: 0.0,
            orderRecu = doc.getBoolean("orderRecu") ?: false,
            enPleinLavage = doc.getBoolean("enPleinLavage") ?: false,
            enSechage = doc.getBoolean("enSechage") ?: false,
            orderPret = doc.getBoolean("orderPret") ?: false,
            orderLivree = doc.getBoolean("orderLivree") ?: (statusVal == "LIVRE" || statusVal == "TERMINE"),
            avisUsers = doc.getString("avisUsers") ?: "",
            fidelityKgUsed = (doc.get("fidelityKgUsed") as? Number)?.toFloat() ?: 0f,
            fideliteKgUtilise = (doc.get("fideliteKgUtilise") as? Number)?.toDouble() ?: 0.0,
            fidelityCredited = doc.getBoolean("fidelityCredited") ?: false,
            fideliteCredite = doc.getBoolean("fideliteCredite") ?: false,
            usedFreeKgs = (doc.get("usedFreeKgs") as? Number)?.toDouble()
                ?: (doc.get("fideliteKgUtilise") as? Number)?.toDouble()
                ?: ((doc.get("fidelityKgUsed") as? Number)?.toDouble() ?: 0.0),
            isLoyaltyPointsGranted = doc.getBoolean("isLoyaltyPointsGranted")
                ?: doc.getBoolean("fidelityCredited")
                ?: doc.getBoolean("fideliteCredite")
                ?: false,

            // Enrichissement Admin & Livreur
            weight = rawWeight,
            totalPrice = rawPrice,
            adminNotes = rawAdminNotes,
            deliveryBoyId = rawDeliveryBoy,
            customerName = rawCustName,
            customerPhone = rawCustPhone,
            deliveryAddress = rawAddress
        )
    }

    /**
     * Mappe une OrderEntity Room vers une HashMap prête à être écrite dans Firestore.
     */
    fun toFirestoreMap(entity: OrderEntity): Map<String, Any?> {
        return hashMapOf(
            "id" to entity.id,
            "userId" to entity.userId,
            "customerName" to entity.customerName,
            "nomUser" to entity.customerName,
            "customerPhone" to entity.customerPhone,
            "telephone" to entity.customerPhone,
            "deliveryAddress" to entity.deliveryAddress,
            "adresseLivraison" to entity.deliveryAddress,
            "status" to entity.status,
            "statut" to entity.status,
            "weight" to entity.weight,
            "kg" to entity.kg,
            "totalPrice" to entity.totalPrice,
            "prixFinal" to entity.prixFinal,
            "adminNotes" to entity.adminNotes,
            "adminMessage" to entity.adminNotes,
            "deliveryBoyId" to entity.deliveryBoyId,
            "photoUri" to entity.photoUri,
            "imageUrl" to entity.photoUri,
            "pieces" to entity.pieces,
            "categorie" to entity.categorie,
            "lavageType" to entity.lavageType,
            "isExpress" to entity.isExpress,
            "instructionsSpeciales" to entity.instructionsSpeciales,
            "deliveryType" to entity.deliveryType,
            "transportFee" to entity.transportFee,
            "timelineStep" to entity.timelineStep,
            "specialItemsData" to entity.specialItemsData,
            "pickupDate" to entity.pickupDate,
            "deliveryDate" to entity.deliveryDate,
            "latitude" to entity.latitude,
            "longitude" to entity.longitude,
            "distanceKm" to entity.distanceKm,
            "promoCode" to entity.promoCode,
            "discountPercentage" to entity.discountPercentage,
            "discountFixed" to entity.discountFixed,
            "hasPremiumGuarantee" to entity.hasPremiumGuarantee,
            "insuranceFee" to entity.insuranceFee,
            "isArchived" to entity.isArchived,
            "createdAt" to entity.createdAt,
            "orderRecu" to entity.orderRecu,
            "enPleinLavage" to entity.enPleinLavage,
            "enSechage" to entity.enSechage,
            "orderPret" to entity.orderPret,
            "orderLivree" to entity.orderLivree,
            "usedFreeKgs" to entity.usedFreeKgs,
            "isLoyaltyPointsGranted" to entity.isLoyaltyPointsGranted,
            "fideliteKgUtilise" to entity.usedFreeKgs,
            "fidelityKgUsed" to entity.usedFreeKgs.toFloat(),
            "fideliteCredite" to entity.isLoyaltyPointsGranted,
            "fidelityCredited" to entity.isLoyaltyPointsGranted
        )
    }

    /**
     * Conversion OrderDto -> OrderEntity
     */
    fun OrderDto.toEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            userId = userId,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            status = status,
            weight = weight,
            kg = weight.toFloat(),
            totalPrice = totalPrice,
            prixFinal = totalPrice.toFloat(),
            adminNotes = adminNotes,
            adminMessage = adminNotes,
            deliveryBoyId = deliveryBoyId,
            photoUri = photoUri,
            pieces = pieces,
            categorie = categorie,
            lavageType = lavageType,
            isExpress = isExpress,
            instructionsSpeciales = instructionsSpeciales,
            deliveryType = deliveryType,
            transportFee = transportFee,
            timelineStep = timelineStep,
            specialItemsData = specialItemsData,
            pickupDate = pickupDate,
            deliveryDate = deliveryDate,
            latitude = latitude,
            longitude = longitude,
            distanceKm = distanceKm,
            promoCode = promoCode,
            discountPercentage = discountPercentage,
            discountFixed = discountFixed,
            hasPremiumGuarantee = hasPremiumGuarantee,
            insuranceFee = insuranceFee,
            isArchived = isArchived,
            createdAt = createdAt,

            autoCompletionInfos = autoCompletionInfos,
            nombrePieces = nombrePieces,
            choixCategory = choixCategory,
            appareilPhoto = appareilPhoto,
            lavageSimple = lavageSimple,
            lavageSechagePliage = lavageSechagePliage,
            lavageSechagePliageRepassage = lavageSechagePliageRepassage,
            articlesUniquement = articlesUniquement,
            choixArticleSpeciaux = choixArticleSpeciaux,
            choixArticleGommage = choixArticleGommage,
            estimationKilo = estimationKilo,
            depotAgence = depotAgence,
            dateHeureDepot = dateHeureDepot,
            dateHeureRetrait = dateHeureRetrait,
            ramassage = ramassage,
            coordonneesMaps = coordonneesMaps,
            instructionsSpecial = instructionsSpecial,
            serviceExpress = serviceExpress,
            garantiVIP = garantiVIP,
            saisiCodePromos = saisiCodePromos,
            prixEstimerAutoCalcul = prixEstimerAutoCalcul,
            orderRecu = orderRecu,
            enPleinLavage = enPleinLavage,
            enSechage = enSechage,
            orderPret = orderPret,
            orderLivree = orderLivree,
            avisUsers = avisUsers,
            fidelityKgUsed = fidelityKgUsed,
            fideliteKgUtilise = fideliteKgUtilise,
            fidelityCredited = fidelityCredited,
            fideliteCredite = fideliteCredite,
            usedFreeKgs = if (usedFreeKgs > 0.0) usedFreeKgs else (if (fideliteKgUtilise > 0.0) fideliteKgUtilise else fidelityKgUsed.toDouble()),
            isLoyaltyPointsGranted = isLoyaltyPointsGranted || fidelityCredited || fideliteCredite
        )
    }

    /**
     * Conversion OrderEntity -> OrderDto
     */
    fun OrderEntity.toDto(): OrderDto {
        return OrderDto(
            id = id,
            userId = userId,
            customerName = if (customerName.isNotBlank()) customerName else autoCompletionInfos,
            customerPhone = customerPhone,
            deliveryAddress = if (deliveryAddress.isNotBlank()) deliveryAddress else (if (coordonneesMaps.isNotBlank()) coordonneesMaps else adresseLivraison),
            status = status,
            weight = if (weight > 0.0) weight else (if (kg > 0f) kg.toDouble() else estimationKilo),
            totalPrice = if (totalPrice > 0.0) totalPrice else (if (prixFinal > 0f) prixFinal.toDouble() else prixEstimerAutoCalcul),
            adminNotes = if (adminNotes.isNotBlank()) adminNotes else adminMessage,
            deliveryBoyId = deliveryBoyId,
            photoUri = if (photoUri.isNotBlank()) photoUri else appareilPhoto,
            pieces = if (pieces > 0) pieces else nombrePieces.toInt(),
            categorie = if (categorie.isNotBlank()) categorie else choixCategory,
            lavageType = lavageType,
            isExpress = isExpress || serviceExpress,
            instructionsSpeciales = instructionsSpeciales,
            deliveryType = deliveryType,
            transportFee = transportFee,
            timelineStep = timelineStep,
            specialItemsData = specialItemsData,
            pickupDate = pickupDate,
            deliveryDate = deliveryDate,
            latitude = latitude,
            longitude = longitude,
            distanceKm = distanceKm,
            promoCode = promoCode,
            discountPercentage = discountPercentage,
            discountFixed = discountFixed,
            hasPremiumGuarantee = hasPremiumGuarantee,
            insuranceFee = insuranceFee,
            isArchived = isArchived,
            createdAt = createdAt,
            autoCompletionInfos = autoCompletionInfos,
            nombrePieces = nombrePieces,
            choixCategory = choixCategory,
            appareilPhoto = appareilPhoto,
            lavageSimple = lavageSimple,
            lavageSechagePliage = lavageSechagePliage,
            lavageSechagePliageRepassage = lavageSechagePliageRepassage,
            articlesUniquement = articlesUniquement,
            choixArticleSpeciaux = choixArticleSpeciaux,
            choixArticleGommage = choixArticleGommage,
            estimationKilo = estimationKilo,
            depotAgence = depotAgence,
            dateHeureDepot = dateHeureDepot,
            dateHeureRetrait = dateHeureRetrait,
            ramassage = ramassage,
            coordonneesMaps = coordonneesMaps,
            instructionsSpecial = instructionsSpecial,
            serviceExpress = serviceExpress,
            garantiVIP = garantiVIP,
            saisiCodePromos = saisiCodePromos,
            prixEstimerAutoCalcul = prixEstimerAutoCalcul,
            orderRecu = orderRecu,
            enPleinLavage = enPleinLavage,
            enSechage = enSechage,
            orderPret = orderPret,
            orderLivree = orderLivree,
            avisUsers = avisUsers,
            fidelityKgUsed = fidelityKgUsed,
            fideliteKgUtilise = fideliteKgUtilise,
            fidelityCredited = fidelityCredited,
            fideliteCredite = fideliteCredite,
            usedFreeKgs = usedFreeKgs,
            isLoyaltyPointsGranted = isLoyaltyPointsGranted
        )
    }
}
