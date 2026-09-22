const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

code = code.replace(
/    val specialItems by viewModel\.allSpecialItems\.collectAsStateWithLifecycle\(\)/,
`    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()`
);

code = code.replace(
/    val selectedSpecialItems = remember \{ mutableStateMapOf<String, Int>\(\) \}/,
`    val selectedSpecialItems = remember { mutableStateMapOf<String, Int>() }
    val selectedGommageItems = remember { mutableStateMapOf<String, Int>() }`
);

code = code.replace(
/            if \(specialItems\.isNotEmpty\(\)\) \{[\s\S]*?                                \} \n                            \}\n                        \}\n                    \}\n                \}\n            \}/,
`            if (specialItems.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Articles Spéciaux (Hors Kg)", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    "Choisissez les articles spéciaux (facturés à l'unité)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    specialItems.forEach { item ->
                        val qty = selectedSpecialItems[item.name] ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("\${item.price.toInt()} FCFA / pièce", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 0) selectedSpecialItems[item.name] = qty - 1 }) {
                                        Text("-", style = MaterialTheme.typography.titleLarge)
                                    }
                                    Text(qty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { selectedSpecialItems[item.name] = qty + 1 }) {
                                        Text("+", style = MaterialTheme.typography.titleLarge)
                                    }
                                } 
                            }
                        }
                    }
                }
            }

            if (gommageItems.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Articles de Gommage", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    "Choisissez les articles de gommage (facturés à l'unité)",
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
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("\${item.price.toInt()} FCFA / pièce", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 0) selectedGommageItems[item.name] = qty - 1 }) {
                                        Text("-", style = MaterialTheme.typography.titleLarge)
                                    }
                                    Text(qty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { selectedGommageItems[item.name] = qty + 1 }) {
                                        Text("+", style = MaterialTheme.typography.titleLarge)
                                    }
                                } 
                            }
                        }
                    }
                }
            }`
);

code = code.replace(
/            var totalSpecial = 0f\n            selectedSpecialItems\.forEach \{ \(name, qty\) ->\n                val price = specialItems\.find \{ it\.name == name \}\?\.price \?: 0f\n                totalSpecial \+= price \* qty\n            \}/,
`            var totalSpecial = 0f
            selectedSpecialItems.forEach { (name, qty) ->
                val price = specialItems.find { it.name == name }?.price ?: 0f
                totalSpecial += price * qty
            }
            var totalGommage = 0f
            selectedGommageItems.forEach { (name, qty) ->
                val price = gommageItems.find { it.name == name }?.price ?: 0f
                totalGommage += price * qty
            }`
);

code = code.replace(
/            val sousTotal = baseGommagePrix \+ \(prixLavagePerKg \* kgVal\) \+ totalSpecial/,
`            val sousTotal = baseGommagePrix + (prixLavagePerKg * kgVal) + totalSpecial + totalGommage`
);

code = code.replace(
/                    val specialDataString = selectedSpecialItems\.filter \{ it\.value > 0 \}\.map \{ \(name, qty\) ->\n                        val price = specialItems\.find \{ it\.name == name \}\?\.price \?: 0f\n                        totalSpecial \+= price \* qty\n                        "\$name\|\$price\|\$qty"\n                    \}\.joinToString\(";"\)\n                    \n                    val basePrix = \(paires \* 1500f\) \+ \(impairs \* 2000f\) \+ totalSpecial/,
`                    val specialDataString = selectedSpecialItems.filter { it.value > 0 }.map { (name, qty) ->
                        val price = specialItems.find { it.name == name }?.price ?: 0f
                        totalSpecial += price * qty
                        "$name:$qty"
                    }.joinToString(",")

                    var totalGommageSubmit = 0f
                    val gommageDataString = selectedGommageItems.filter { it.value > 0 }.map { (name, qty) ->
                        val price = gommageItems.find { it.name == name }?.price ?: 0f
                        totalGommageSubmit += price * qty
                        "$name:$qty"
                    }.joinToString(",")
                    
                    val combinedSpecialData = listOf(specialDataString, gommageDataString).filter { it.isNotBlank() }.joinToString(",")
                    
                    val basePrix = (paires * 1500f) + (impairs * 2000f) + totalSpecial + totalGommageSubmit`
);

code = code.replace(
/                        specialItemsData = specialDataString,/,
`                        specialItemsData = combinedSpecialData,`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("CreateOrderScreen updated with GommageItems");
