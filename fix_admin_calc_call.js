const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target1 = `                                    var calcPrice = calculateOrderPrice(
                                        pieces = order.pieces,
                                        kg = k,
                                        lavageType = order.lavageType,
                                        gommagePaire = order.gommagePaire,
                                        gommageImpair = order.gommageImpair,
                                        config = config,
                                        isExpress = order.isExpress,
                                        isPremium = order.hasPremiumGuarantee
                                    )
                                    
                                    val currentSpecialMap = mutableMapOf<String, Int>()
                                    if (order.specialItemsData.isNotBlank()) {
                                        order.specialItemsData.split(",").forEach {
                                            val parts = it.split(":")
                                            if (parts.size == 2) currentSpecialMap[parts[0]] = parts[1].toIntOrNull() ?: 0
                                        }
                                    }
                                    calcPrice += currentSpecialMap.mapNotNull { entry ->
                                        val pSpec = specialItems.find { it.name == entry.key }?.price
                                        val pGom = gommageItems.find { it.name == entry.key }?.price
                                        val p = pSpec ?: (pGom ?: 0f)
                                        p * entry.value
                                    }.sum()`;
                                    
const replacement1 = `                                    
                                    val currentSpecialMap = mutableMapOf<String, Int>()
                                    if (order.specialItemsData.isNotBlank()) {
                                        order.specialItemsData.split(",").forEach {
                                            val parts = it.split(":")
                                            if (parts.size == 2) currentSpecialMap[parts[0]] = parts[1].toIntOrNull() ?: 0
                                        }
                                    }
                                    val itemsCostMap = currentSpecialMap.mapValues { entry ->
                                        val pSpec = specialItems.find { it.name == entry.key }?.price
                                        val pGom = gommageItems.find { it.name == entry.key }?.price
                                        val p = pSpec ?: (pGom ?: 0f)
                                        p * entry.value
                                    }
                                    val validPromo = allPromos.find { it.code == order.promoCode }
                                    var calcPrice = calculateOrderPrice(
                                        pieces = order.pieces,
                                        kg = k,
                                        lavageType = order.lavageType,
                                        gommagePaire = order.gommagePaire,
                                        gommageImpair = order.gommageImpair,
                                        config = config,
                                        isExpress = order.isExpress,
                                        isPremium = order.hasPremiumGuarantee,
                                        promo = validPromo,
                                        specialItemsCostMap = itemsCostMap,
                                        transportFee = order.transportFee
                                    )`;

code = code.replace(target1, replacement1);


const target2 = `                        var calcPrice = calculateOrderPrice(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            kg = k,
                            lavageType = lavageTypeInput,
                            gommagePaire = gommagePaireInput.toIntOrNull() ?: 0,
                            gommageImpair = gommageImpairInput.toIntOrNull() ?: 0,
                            config = config,
                            isExpress = isExpressInput,
                            isPremium = hasPremiumGuaranteeInput
                        )
                        
                        calcPrice += specialItemsMap.mapNotNull { entry ->
                            val pSpec = specialItems.find { it.name == entry.key }?.price
                            val pGom = gommageItems.find { it.name == entry.key }?.price
                            val p = pSpec ?: (pGom ?: 0f)
                            p * entry.value
                        }.sum()`;
                        
const replacement2 = `                        val itemsCostMap = specialItemsMap.mapValues { entry ->
                            val pSpec = specialItems.find { it.name == entry.key }?.price
                            val pGom = gommageItems.find { it.name == entry.key }?.price
                            val p = pSpec ?: (pGom ?: 0f)
                            p * entry.value
                        }
                        val validPromo = allPromos.find { it.code == order.promoCode }
                        var calcPrice = calculateOrderPrice(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            kg = k,
                            lavageType = lavageTypeInput,
                            gommagePaire = gommagePaireInput.toIntOrNull() ?: 0,
                            gommageImpair = gommageImpairInput.toIntOrNull() ?: 0,
                            config = config,
                            isExpress = isExpressInput,
                            isPremium = hasPremiumGuaranteeInput,
                            promo = validPromo,
                            specialItemsCostMap = itemsCostMap,
                            transportFee = order.transportFee
                        )`;

code = code.replace(target2, replacement2);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
