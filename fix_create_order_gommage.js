const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

const target = `            HorizontalDivider()
            
            Text(
                "Options supplémentaires (Gommage)", `;

const replacement = `            if (gommageItems.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Articles de Gommage", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sélectionnez les articles spécifiques pour le gommage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gommageItems.forEach { item ->
                        val qty = selectedGommageItems[item.name] ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("\${item.price} FCFA / unité", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 0) selectedGommageItems[item.name] = qty - 1 }) {
                                        Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$qty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { selectedGommageItems[item.name] = qty + 1 }) {
                                        Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            
            Text(
                "Options supplémentaires (Gommage)", `;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("CreateOrderScreen updated for Gommage items");
