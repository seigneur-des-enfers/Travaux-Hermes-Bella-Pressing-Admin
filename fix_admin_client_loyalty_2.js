const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                if (canManageFidelity) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Programme de Fidélité du Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer")
                    }
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
                }`;

const replacement = `                if (canManageFidelity) {
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
                }`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard client loyalty 2 fixed");
