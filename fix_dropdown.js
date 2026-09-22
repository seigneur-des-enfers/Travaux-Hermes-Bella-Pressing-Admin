const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                        ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            listOf("ALL", "COMMANDE_COMPLETE", "LAVAGE_SIMPLE", "LAVAGE_SECHAGE_PLIAGE").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option else "$promoTargetItems,$option"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            specialItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Article: \${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,\${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                        }`;

const replacement = `                        ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            listOf("ALL", "COMMANDE_COMPLETE", "LAVAGE_SIMPLE", "LAVAGE_SECHAGE_PLIAGE", "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "GOMMAGE").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option else "$promoTargetItems,$option"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            specialItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Spécial: \${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,\${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            gommageItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Gommage: \${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,\${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                        }`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard dropdown fixed");
