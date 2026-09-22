const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const tabsTarget = `    val tabs = listOf(
        "COMMANDES" to Icons.Default.List,
        "CLIENTS" to Icons.Default.Person,
        "STOCKS" to Icons.Default.Inventory,
        "CATALOGUE" to Icons.Default.LocalOffer,
        "PARAMETRES" to Icons.Default.Settings,
        "STAFF" to Icons.Default.ManageAccounts,
        "REGLES" to Icons.Default.Gavel,
        "CORBEILLE_GLOBALE" to Icons.Default.Delete
    )`;

const tabsReplacement = `    val tabs = listOf(
        "COMMANDES" to Icons.Default.List,
        "CLIENTS" to Icons.Default.Person,
        "STOCKS" to Icons.Default.Inventory,
        "CATALOGUE" to Icons.Default.LocalOffer,
        "PARAMETRES" to Icons.Default.Settings,
        "PUBLICITES" to Icons.Default.Campaign,
        "STAFF" to Icons.Default.ManageAccounts,
        "REGLES" to Icons.Default.Gavel,
        "CORBEILLE_GLOBALE" to Icons.Default.Delete
    )`;

code = code.replace(tabsTarget, tabsReplacement);

const whenTarget = `        when (selectedTab) {
            "COMMANDES" -> if (hasPermission(currentUser, "VOIR_COMMANDES")) AdminOrdersTab(padding, allOrders, viewModel)
            "CLIENTS" -> if (hasPermission(currentUser, "VOIR_CLIENTS")) AdminClientsTab(padding, allClients, allOrders, viewModel)
            "STOCKS" -> if (hasPermission(currentUser, "VOIR_STOCKS")) AdminStocksTab(padding, viewModel)
            "CATALOGUE" -> if (hasPermission(currentUser, "VOIR_CATALOGUE")) AdminCatalogueTab(padding, viewModel)
            "PARAMETRES" -> if (hasPermission(currentUser, "VOIR_PARAMETRES")) AdminSettingsTab(padding, viewModel)
            "STAFF" -> if (hasPermission(currentUser, "VOIR_STAFF")) AdminStaffTab(padding, viewModel)
            "REGLES" -> if (hasPermission(currentUser, "GERER_REGLES")) AdminRulesTab(padding, viewModel)
            "CORBEILLE_GLOBALE" -> if (hasPermission(currentUser, "VOIR_CORBEILLE")) AdminGlobalTrashTab(padding, viewModel)
        }`;

const whenReplacement = `        when (selectedTab) {
            "COMMANDES" -> if (hasPermission(currentUser, "VOIR_COMMANDES")) AdminOrdersTab(padding, allOrders, viewModel)
            "CLIENTS" -> if (hasPermission(currentUser, "VOIR_CLIENTS")) AdminClientsTab(padding, allClients, allOrders, viewModel)
            "STOCKS" -> if (hasPermission(currentUser, "VOIR_STOCKS")) AdminStocksTab(padding, viewModel)
            "CATALOGUE" -> if (hasPermission(currentUser, "VOIR_CATALOGUE")) AdminCatalogueTab(padding, viewModel)
            "PARAMETRES" -> if (hasPermission(currentUser, "VOIR_PARAMETRES")) AdminSettingsTab(padding, viewModel)
            "PUBLICITES" -> if (hasPermission(currentUser, "VOIR_PUBLICITES") || currentUser?.permissions?.contains("ALL") == true) AdminPublicitesTab(padding, viewModel)
            "STAFF" -> if (hasPermission(currentUser, "VOIR_STAFF")) AdminStaffTab(padding, viewModel)
            "REGLES" -> if (hasPermission(currentUser, "GERER_REGLES")) AdminRulesTab(padding, viewModel)
            "CORBEILLE_GLOBALE" -> if (hasPermission(currentUser, "VOIR_CORBEILLE")) AdminGlobalTrashTab(padding, viewModel)
        }`;

code = code.replace(whenTarget, whenReplacement);

const tabCode = `
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPublicitesTab(padding: PaddingValues, viewModel: AppViewModel) {
    val announcements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.example.data.AnnouncementEntity?>(null) }
    
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var isVisibleInput by remember { mutableStateOf(true) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Gestion des Publicités & Annonces", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { 
                editingItem = null
                titleInput = ""
                messageInput = ""
                imageUrlInput = ""
                isVisibleInput = true
                showDialog = true 
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouvelle Publicité")
            }
        }
        
        items(announcements) { ann ->
            Card(modifier = Modifier.fillMaxWidth().clickable {
                editingItem = ann
                titleInput = ann.title
                messageInput = ann.message
                imageUrlInput = ann.imageUrl
                isVisibleInput = ann.isVisible
                showDialog = true
            }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ann.title, fontWeight = FontWeight.Bold)
                        Text(ann.message, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        if (!ann.isVisible) {
                            Text("Masqué", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteAnnouncement(ann.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingItem == null) "Nouvelle Publicité" else "Modifier Publicité") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Titre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("URL de l'image (Optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isVisibleInput, onCheckedChange = { isVisibleInput = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visible aux clients")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                        if (editingItem == null) {
                            viewModel.addAnnouncement(titleInput, messageInput, imageUrlInput)
                        } else {
                            viewModel.updateAnnouncement(editingItem!!.copy(title = titleInput, message = messageInput, imageUrl = imageUrlInput, isVisible = isVisibleInput))
                        }
                    }
                    showDialog = false
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }
}
`;

code += tabCode;

if (!code.includes('import androidx.compose.material.icons.filled.Campaign')) {
    code = code.replace('import androidx.compose.material.icons.filled.Settings', 'import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.Campaign');
}

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("Admin Publicites Tab added");
