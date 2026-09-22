const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target1 = `                Text("Articles Spéciaux", style = MaterialTheme.typography.labelSmall)
                specialItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                if (current > 0) specialItemsMap[item.name] = current - 1
                            }) { Text("-") }
                            Text("\${specialItemsMap[item.name] ?: 0}")
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                specialItemsMap[item.name] = current + 1
                            }) { Text("+") }
                        }
                    }
                }`;

const replacement1 = `                Text("Articles Spéciaux", style = MaterialTheme.typography.labelSmall)
                specialItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                if (current > 0) specialItemsMap[item.name] = current - 1
                            }) { Text("-") }
                            Text("\${specialItemsMap[item.name] ?: 0}")
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                specialItemsMap[item.name] = current + 1
                            }) { Text("+") }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                if (gommageItems.isNotEmpty()) {
                    Text("Articles de Gommage", style = MaterialTheme.typography.labelSmall)
                    gommageItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    val current = specialItemsMap[item.name] ?: 0
                                    if (current > 0) specialItemsMap[item.name] = current - 1
                                }) { Text("-") }
                                Text("\${specialItemsMap[item.name] ?: 0}")
                                IconButton(onClick = { 
                                    val current = specialItemsMap[item.name] ?: 0
                                    specialItemsMap[item.name] = current + 1
                                }) { Text("+") }
                            }
                        }
                    }
                }`;

code = code.replace(target1, replacement1);


const target2 = `                        calcPrice += specialItemsMap.mapNotNull { entry ->
                            val p = specialItems.find { it.name == entry.key }?.price ?: 0f
                            p * entry.value
                        }.sum()`;
                        
const replacement2 = `                        calcPrice += specialItemsMap.mapNotNull { entry ->
                            val pSpec = specialItems.find { it.name == entry.key }?.price
                            val pGom = gommageItems.find { it.name == entry.key }?.price
                            val p = pSpec ?: (pGom ?: 0f)
                            p * entry.value
                        }.sum()`;
                        
code = code.replace(target2, replacement2);


const target3 = `                                    var calcPrice = calculateOrderPrice(
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
                                        val p = specialItems.find { it.name == entry.key }?.price ?: 0f
                                        p * entry.value
                                    }.sum()`;
                                    
const replacement3 = `                                    var calcPrice = calculateOrderPrice(
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
                                    
code = code.replace(target3, replacement3);

// wait we also need to pass gommageItems to AdminOrderCard
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard patched for order edit/save");
