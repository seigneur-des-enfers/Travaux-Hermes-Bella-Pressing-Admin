const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                                    calcPrice += order.specialItemsData.split(",").filter { it.isNotBlank() }.mapNotNull { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val p = specialItems.find { item -> item.name == parts[0] }?.price ?: 0f
                                            p * (parts[1].toIntOrNull() ?: 0)
                                        } else null
                                    }.sum()`;

const replacement = `                                    calcPrice += order.specialItemsData.split(",").filter { it.isNotBlank() }.mapNotNull { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val pSpec = specialItems.find { item -> item.name == parts[0] }?.price
                                            val pGom = gommageItems.find { item -> item.name == parts[0] }?.price
                                            val p = pSpec ?: (pGom ?: 0f)
                                            p * (parts[1].toIntOrNull() ?: 0)
                                        } else null
                                    }.sum()`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
