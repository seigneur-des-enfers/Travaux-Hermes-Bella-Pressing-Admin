const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `        item {
            Button(
                onClick = {`;

const replacement = `        item {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration du Programme de Fidélité Global", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le programme de fidélité globalement")
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
                                label = { Text("Durée d'activation (jours à partir de maintenant)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard loyalty global UI added");
