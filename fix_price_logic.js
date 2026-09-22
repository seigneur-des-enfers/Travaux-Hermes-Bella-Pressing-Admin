const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target1 = `fun calculateOrderPrice(
    pieces: Int,
    kg: Float,
    lavageType: String,
    gommagePaire: Int,
    gommageImpair: Int,
    config: com.example.data.ConfigEntity?,
    isExpress: Boolean,
    isPremium: Boolean
): Float {
    val prixLavageType = when (lavageType) {
        "SIMPLE", "Lavage simple" -> 350f
        "LAVAGE_SECHAGE_PLIAGE", "Lavage + séchage + pliage" -> 400f
        "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "Lavage + séchage + repassage + pliage" -> 500f
        else -> 0f
    }
    
    val prixKg = kg * prixLavageType
    val prixGommage = (gommagePaire * 1500f) + (gommageImpair * 2000f)
    
    val sousTotal = prixKg + prixGommage
    val insuranceFee = if (isPremium) sousTotal * 0.1f else 0f
    val multiplier = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
    
    val totalAvecExpress = (sousTotal + insuranceFee) * multiplier
    
    // The user requested that all results are integers (no decimal), so we round it or convert to Int and then Float.
    return Math.round(totalAvecExpress).toFloat()
}`;

const replacement1 = `fun calculateOrderPrice(
    pieces: Int,
    kg: Float,
    lavageType: String,
    gommagePaire: Int,
    gommageImpair: Int,
    config: com.example.data.ConfigEntity?,
    isExpress: Boolean,
    isPremium: Boolean,
    promo: com.example.data.PromoEntity? = null,
    specialItemsCostMap: Map<String, Float> = emptyMap(),
    transportFee: Float = 0f
): Float {
    val prixLavageType = when (lavageType) {
        "SIMPLE", "Lavage simple" -> 350f
        "LAVAGE_SECHAGE_PLIAGE", "Lavage + séchage + pliage" -> 400f
        "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "Lavage + séchage + repassage + pliage" -> 500f
        else -> 0f
    }
    val prixKg = kg * prixLavageType
    val prixGommage = (gommagePaire * 1500f) + (gommageImpair * 2000f)
    
    var totalTargetedForDiscount = 0f
    var totalNonTargeted = 0f
    
    val targetItems = promo?.targetItems?.split(",")?.map { it.trim() } ?: listOf()
    val targetsAll = targetItems.contains("ALL") || targetItems.contains("COMMANDE_COMPLETE") || targetItems.isEmpty()

    if (targetsAll) {
        totalTargetedForDiscount += prixKg + prixGommage
        specialItemsCostMap.forEach { totalTargetedForDiscount += it.value }
    } else {
        if (targetItems.contains(lavageType) || (targetItems.contains("LAVAGE_SIMPLE") && (lavageType == "SIMPLE" || lavageType == "Lavage simple"))) {
            totalTargetedForDiscount += prixKg
        } else {
            totalNonTargeted += prixKg
        }
        
        if (targetItems.contains("GOMMAGE")) {
            totalTargetedForDiscount += prixGommage
        } else {
            totalNonTargeted += prixGommage
        }
        
        specialItemsCostMap.forEach { (name, cost) ->
            if (targetItems.contains(name)) {
                totalTargetedForDiscount += cost
            } else {
                totalNonTargeted += cost
            }
        }
    }
    
    val totalDiscount = if (promo != null && promo.isActive) {
        if (promo.discountType == "PERCENTAGE") {
            totalTargetedForDiscount * promo.discountPercent
        } else {
            minOf(totalTargetedForDiscount, promo.discountPercent)
        }
    } else 0f
    
    val finalItemsPrice = (totalTargetedForDiscount - totalDiscount) + totalNonTargeted
    
    val insuranceFee = if (isPremium) finalItemsPrice * 0.1f else 0f
    val multiplier = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
    
    val totalAvecExpress = (finalItemsPrice + insuranceFee) * multiplier
    
    return Math.round(totalAvecExpress + transportFee).toFloat()
}`;

code = code.replace(target1, replacement1);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
