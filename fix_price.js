const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/fun calculateOrderPrice\([\s\S]*?\): Float \{[\s\S]*?    return totalAvecExpress\n\}/,
`fun calculateOrderPrice(
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
}`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("calculateOrderPrice fixed");
