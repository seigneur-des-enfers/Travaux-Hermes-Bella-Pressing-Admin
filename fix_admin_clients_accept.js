const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                                    var calcPrice = calculateOrderPrice(
                                        pieces = order.pieces,
                                        kg = k,
                                        lavageType = order.lavageType,
                                        gommagePaire = order.gommagePaire,
                                        gommageImpair = order.gommageImpair,
                                        config = config,
                                        isExpress = order.isExpress,
                                        isPremium = order.hasPremiumGuarantee
                                    )
                                    calcPrice += order.specialItemsData.split(",").filter { it.isNotBlank() }.mapNotNull { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val pSpec = specialItems.find { item -> item.name == parts[0] }?.price
                                            val pGom = gommageItems.find { item -> item.name == parts[0] }?.price
                                            val p = pSpec ?: (pGom ?: 0f)
                                            p * (parts[1].toIntOrNull() ?: 0)
                                        } else null
                                    }.sum()
                                    viewModel.updateOrder(order.copy(status = "ACCEPTE", timelineStep = 1, kg = k, prixFinal = calcPrice))`;

const replacement = `                                    val itemsCostMap = mutableMapOf<String, Float>()
                                    order.specialItemsData.split(",").filter { it.isNotBlank() }.forEach { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val pSpec = specialItems.find { item -> item.name == parts[0] }?.price
                                            val pGom = gommageItems.find { item -> item.name == parts[0] }?.price
                                            val p = pSpec ?: (pGom ?: 0f)
                                            itemsCostMap[parts[0]] = p * (parts[1].toIntOrNull() ?: 0)
                                        }
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
                                    )
                                    viewModel.updateOrder(order.copy(status = "ACCEPTE", timelineStep = 1, kg = k, prixFinal = calcPrice))`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminClientsTab accept patched");
