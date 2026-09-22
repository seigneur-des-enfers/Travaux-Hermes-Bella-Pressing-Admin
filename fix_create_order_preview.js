const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

const target = `            val validPromoLive = allPromos.find { it.code == promoCode.trim().uppercase() && it.isActive && (it.isPermanent || (it.validUntil != null && it.validUntil > System.currentTimeMillis())) }
            val discountPreview = if (validPromoLive != null && validPromoLive.discountType == "PERCENTAGE" && user?.canUsePromo == true) validPromoLive.discountPercent else 0f
            val fixedDiscountPreview = if (validPromoLive != null && validPromoLive.discountType == "FIXED_AMOUNT" && user?.canUsePromo == true) validPromoLive.discountPercent else 0f

            val insuranceLive = if (hasPremiumGuarantee) currentTotal * 0.1f else 0f
            val multiplierLive = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
            
            val totalPreview = maxOf(0f, ((currentTotal + insuranceLive) * multiplierLive * (1f - discountPreview)) - fixedDiscountPreview) + if (deliveryType == "DOMICILE") (config?.baseTransportFee ?: 1000f) + ((distanceKm.toFloatOrNull() ?: 0f) * (config?.pricePerKm ?: 500f)) else 0f`;

const replacement = `            val validPromoLive = allPromos.find { it.code == promoCode.trim().uppercase() && it.isActive && (it.isPermanent || (it.validUntil != null && it.validUntil > System.currentTimeMillis())) }
            
            var totalTargetedPreview = 0f
            var totalNonTargetedPreview = 0f
            
            val targetItems = validPromoLive?.targetItems?.split(",")?.map { it.trim() } ?: listOf()
            val targetsAll = targetItems.contains("ALL") || targetItems.contains("COMMANDE_COMPLETE") || targetItems.isEmpty()

            if (targetsAll) {
                totalTargetedPreview = currentTotal
            } else {
                val pKg = kgVal * prixLavagePerKg
                if (targetItems.contains(lavageType) || (targetItems.contains("LAVAGE_SIMPLE") && (lavageType == "SIMPLE" || lavageType == "Lavage simple"))) {
                    totalTargetedPreview += pKg
                } else {
                    totalNonTargetedPreview += pKg
                }
                
                if (targetItems.contains("GOMMAGE")) {
                    totalTargetedPreview += baseGommagePrix
                } else {
                    totalNonTargetedPreview += baseGommagePrix
                }
                
                selectedSpecialItems.filter { it.value > 0 }.forEach { (name, qty) ->
                    val cost = (specialItems.find { it.name == name }?.price ?: 0f) * qty
                    if (targetItems.contains(name)) totalTargetedPreview += cost else totalNonTargetedPreview += cost
                }
                selectedGommageItems.filter { it.value > 0 }.forEach { (name, qty) ->
                    val cost = (gommageItems.find { it.name == name }?.price ?: 0f) * qty
                    if (targetItems.contains(name)) totalTargetedPreview += cost else totalNonTargetedPreview += cost
                }
            }
            
            val previewDiscount = if (validPromoLive != null && validPromoLive.isActive && user?.canUsePromo == true) {
                if (validPromoLive.discountType == "PERCENTAGE") {
                    totalTargetedPreview * validPromoLive.discountPercent
                } else {
                    minOf(totalTargetedPreview, validPromoLive.discountPercent)
                }
            } else 0f

            val finalItemsPreview = (totalTargetedPreview - previewDiscount) + totalNonTargetedPreview
            val insuranceLive = if (hasPremiumGuarantee) finalItemsPreview * 0.1f else 0f
            val multiplierLive = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
            
            val totalPreview = (finalItemsPreview + insuranceLive) * multiplierLive + if (deliveryType == "DOMICILE") (config?.baseTransportFee ?: 1000f) + ((distanceKm.toFloatOrNull() ?: 0f) * (config?.pricePerKm ?: 500f)) else 0f`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("CreateOrderScreen preview fixed");
