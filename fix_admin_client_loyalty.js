const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const targetState = `    var password by remember { mutableStateOf(client.motDePasse) }
    var fidelityKg by remember { mutableStateOf(client.fidelityPointsKg.toString()) }`;

const replacementState = `    var password by remember { mutableStateOf(client.motDePasse) }
    var fidelityKg by remember { mutableStateOf(client.fidelityPointsKg.toString()) }
    var isLoyaltyEnabled by remember { mutableStateOf(client.isLoyaltyEnabled) }
    var isLoyaltyPermanent by remember { mutableStateOf(client.loyaltyValidUntil == null) }
    var loyaltyDurationDays by remember { mutableStateOf("") }`;

code = code.replace(targetState, replacementState);

const targetUI = `                OutlinedTextField(
                    value = fidelityKg,
                    onValueChange = { fidelityKg = it },
                    label = { Text("Kg de fidélité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },`;

const replacementUI = `                OutlinedTextField(
                    value = fidelityKg,
                    onValueChange = { fidelityKg = it },
                    label = { Text("Kg de fidélité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Programme de Fidélité du Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activer")
                }
                if (isLoyaltyEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isLoyaltyPermanent, onCheckedChange = { isLoyaltyPermanent = it })
                        Text("Permanent")
                    }
                    if (!isLoyaltyPermanent) {
                        OutlinedTextField(
                            value = loyaltyDurationDays,
                            onValueChange = { loyaltyDurationDays = it },
                            label = { Text("Durée d'activation (jours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        },`;

code = code.replace(targetUI, replacementUI);

const targetSave = `                    coordonnees = coordonnees,
                    motDePasse = password,
                    fidelityPointsKg = fidelityKg.toFloatOrNull() ?: client.fidelityPointsKg
                )`;

const replacementSave = `                    coordonnees = coordonnees,
                    motDePasse = password,
                    fidelityPointsKg = fidelityKg.toFloatOrNull() ?: client.fidelityPointsKg,
                    isLoyaltyEnabled = isLoyaltyEnabled,
                    loyaltyValidUntil = if (!isLoyaltyEnabled) null else if (isLoyaltyPermanent) null else (System.currentTimeMillis() + (loyaltyDurationDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000)
                )`;

code = code.replace(targetSave, replacementSave);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard client loyalty UI added");
