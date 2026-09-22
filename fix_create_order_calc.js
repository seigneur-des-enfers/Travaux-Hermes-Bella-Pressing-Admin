const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

const target = `                    val basePrix = (paires * 1500f) + (impairs * 2000f) + totalSpecial + totalGommageSubmit
                    
                    val distanceFloat = distanceKm.toFloatOrNull() ?: 0f
                    val transport = if (deliveryType == "DOMICILE") (config?.baseTransportFee ?: 1000f) + (distanceFloat * (config?.pricePerKm ?: 500f)) else 0f
                    
                    val validPromo = allPromos.find { it.code == promoCode.trim().uppercase() && it.isActive && (it.isPermanent || (it.validUntil != null && it.validUntil > System.currentTimeMillis())) }
                    val finalDiscountPercentage = if (validPromo != null && validPromo.discountType == "PERCENTAGE" && user?.canUsePromo == true) validPromo.discountPercent else 0f
                    val finalFixedDiscount = if (validPromo != null && validPromo.discountType == "FIXED_AMOUNT" && user?.canUsePromo == true) validPromo.discountPercent else 0f
                    
                    val insuranceFee = if (hasPremiumGuarantee) basePrix * 0.1f else 0f
                    
                    val expressMultiplier = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
                    val rawPrice = maxOf(0f, ((basePrix + insuranceFee) * expressMultiplier * (1f - finalDiscountPercentage)) - finalFixedDiscount) + transport
                    val finalPriceComputed = Math.round(rawPrice).toFloat()`;

const replacement = `                    val distanceFloat = distanceKm.toFloatOrNull() ?: 0f
                    val transport = if (deliveryType == "DOMICILE") (config?.baseTransportFee ?: 1000f) + (distanceFloat * (config?.pricePerKm ?: 500f)) else 0f
                    
                    val validPromo = allPromos.find { it.code == promoCode.trim().uppercase() && it.isActive && (it.isPermanent || (it.validUntil != null && it.validUntil > System.currentTimeMillis())) }
                    
                    // Same logic as AdminDashboard calculateOrderPrice, but kg is 0 here
                    val prixKg = 0f 
                    val prixGommage = (paires * 1500f) + (impairs * 2000f)
                    
                    var totalTargetedForDiscount = 0f
                    var totalNonTargeted = 0f
                    
                    val targetItems = validPromo?.targetItems?.split(",")?.map { it.trim() } ?: listOf()
                    val targetsAll = targetItems.contains("ALL") || targetItems.contains("COMMANDE_COMPLETE") || targetItems.isEmpty()

                    if (targetsAll) {
                        totalTargetedForDiscount += prixKg + prixGommage + totalSpecial + totalGommageSubmit
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
                        
                        selectedSpecialItems.filter { it.value > 0 }.forEach { (name, qty) ->
                            val cost = (specialItems.find { it.name == name }?.price ?: 0f) * qty
                            if (targetItems.contains(name)) totalTargetedForDiscount += cost else totalNonTargeted += cost
                        }
                        selectedGommageItems.filter { it.value > 0 }.forEach { (name, qty) ->
                            val cost = (gommageItems.find { it.name == name }?.price ?: 0f) * qty
                            if (targetItems.contains(name)) totalTargetedForDiscount += cost else totalNonTargeted += cost
                        }
                    }
                    
                    val totalDiscount = if (validPromo != null && validPromo.isActive && user?.canUsePromo == true) {
                        if (validPromo.discountType == "PERCENTAGE") {
                            totalTargetedForDiscount * validPromo.discountPercent
                        } else {
                            minOf(totalTargetedForDiscount, validPromo.discountPercent)
                        }
                    } else 0f
                    
                    val finalItemsPrice = (totalTargetedForDiscount - totalDiscount) + totalNonTargeted
                    val insuranceFee = if (hasPremiumGuarantee) finalItemsPrice * 0.1f else 0f
                    val expressMultiplier = if (isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
                    
                    val rawPrice = (finalItemsPrice + insuranceFee) * expressMultiplier + transport
                    val finalPriceComputed = Math.round(rawPrice).toFloat()
                    
                    val finalDiscountPercentage = if (validPromo != null && validPromo.discountType == "PERCENTAGE" && user?.canUsePromo == true) validPromo.discountPercent else 0f
                    val finalFixedDiscount = if (validPromo != null && validPromo.discountType == "FIXED_AMOUNT" && user?.canUsePromo == true) validPromo.discountPercent else 0f`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
