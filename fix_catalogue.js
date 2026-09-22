const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/fun AdminCatalogueTab\(padding: PaddingValues, viewModel: AppViewModel\) \{[\s\S]*?    val specialItems by viewModel\.allSpecialItems\.collectAsStateWithLifecycle\(\)/,
`fun AdminCatalogueTab(padding: PaddingValues, viewModel: AppViewModel) {
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()`
);

dash = dash.replace(
/    var editingSpecialItem by remember \{ mutableStateOf<SpecialItemEntity\?>\(null\) \}/,
`    var editingSpecialItem by remember { mutableStateOf<SpecialItemEntity?>(null) }
    var showGommageDialog by remember { mutableStateOf(false) }
    var gommageNameInput by remember { mutableStateOf("") }
    var gommagePriceInput by remember { mutableStateOf("") }
    var editingGommageItem by remember { mutableStateOf<com.example.data.GommageItemEntity?>(null) }`
);

dash = dash.replace(
/        item \{\n            Button\(onClick = \{ \n                editingSpecialItem = null\n                specialNameInput = ""\n                specialPriceInput = ""\n                showSpecialDialog = true \n            \}, modifier = Modifier\.fillMaxWidth\(\)\) \{\n                Text\("Ajouter un article spécial"\)\n            \}\n        \}/,
`        item {
            Button(onClick = { 
                editingSpecialItem = null
                specialNameInput = ""
                specialPriceInput = ""
                showSpecialDialog = true 
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter un article spécial")
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
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
            Button(onClick = { 
                editingGommageItem = null
                gommageNameInput = ""
                gommagePriceInput = ""
                showGommageDialog = true 
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter un article de gommage")
            }
        }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminCatalogueTab modified for GommageItems");
