const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Codes Promo & Événements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)`;

const replacement = `        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (hasPermission(currentUser, "GERER_GOMMAGE")) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Catalogue: Articles de Gommage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (gommageItems.isNotEmpty()) {
                        Text("(Appuyez sur l'article pour le modifier)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            if (gommageItems.isEmpty()) {
                item {
                    Text("Aucun article de gommage configuré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            items(gommageItems) { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    editingGommageItem = item
                    gommageNameInput = item.name
                    gommagePriceInput = item.price.toString()
                    showGommageDialog = true
                }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("\${item.price} FCFA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deleteGommageItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            item {
                Button(onClick = { showGommageDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un Article de Gommage")
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Codes Promo & Événements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard updated for Gommage items");
