const fs = require('fs');
let content = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

// The existing AdminClientsTab spans line 230 to 657
// I will just use regex to replace the ClientBottomSheet function, and the AdminClientsTab body.

let newAdminClientsTab = `@Composable
fun AdminClientsTab(padding: PaddingValues, clients: List<UserEntity>, allOrders: List<OrderEntity>, viewModel: AppViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageClients = hasPermission(currentUser, "GERER_CLIENTS")
    var searchQuery by remember { mutableStateOf("") }
    
    var showTrash by remember { mutableStateOf(false) }

    val filteredClients = clients.filter { 
        (it.nom.contains(searchQuery, ignoreCase = true) || 
         it.prenom.contains(searchQuery, ignoreCase = true) || 
         it.email.contains(searchQuery, ignoreCase = true)) &&
         it.isArchived == showTrash
    }

    var selectedClient by remember { mutableStateOf<UserEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                placeholder = { Text("Rechercher un client...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                Button(onClick = { showTrash = !showTrash }, colors = ButtonDefaults.buttonColors(containerColor = if (showTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                    Text(if (showTrash) "Actifs" else "Corbeille")
                }
            }
        }

        if (filteredClients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if(showTrash) "La corbeille est vide." else "Aucun client trouvé.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredClients) { client ->
                val clientOrders = allOrders.filter { it.userId == client.id }
                val totalSpent = clientOrders.filter { it.status == "TERMINE" }.sumOf { it.prixFinal.toDouble() }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { if (canManageClients) { selectedClient = client; showBottomSheet = true } },
                    colors = CardDefaults.cardColors(
                        containerColor = if (client.isSuspended) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(client.prenom.first().uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("\${client.prenom} \${client.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(client.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(client.telephone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("\${totalSpent.toInt()} FCFA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("\${clientOrders.size} commandes", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (client.isSuspended) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("⚠️ Suspendu: \${client.suspensionReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedClient != null) {
        ClientBottomSheet(client = selectedClient!!, viewModel = viewModel, onDismiss = { showBottomSheet = false })
    }
}`;

let oldTabRegex = /@Composable\s+fun AdminClientsTab[\s\S]*?(?=@OptIn|\n@Composable\s+fun ClientBottomSheet)/g;
content = content.replace(oldTabRegex, newAdminClientsTab + "\n");

let clientBottomSheetRegex = /@Composable\s+fun ClientBottomSheet[\s\S]*?(?=@Composable\s+fun EditClientProfileDialog)/g;

let newClientBottomSheet = `@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBottomSheet(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showSuspendDialog by remember { mutableStateOf(false) }
    var suspensionReason by remember { mutableStateOf("") }
    var showAntiFraudDialog by remember { mutableStateOf(false) }
    var showBlockOptionsDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Actions pour \${client.prenom} \${client.nom}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            if (!client.isArchived) {
                Button(onClick = { showEditProfile = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Éditer le profil")
                }

                Button(onClick = { showBlockOptionsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Personnaliser Autorisations (Commandes)")
                }

                Button(onClick = { showAntiFraudDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Text("Configurer Anti-Fraude Client")
                }
                
                Button(onClick = { showPromoDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Text(if (client.canUsePromo) "Bloquer Utilisation Promos" else "Débloquer Utilisation Promos")
                }

                if (client.isSuspended) {
                    Button(onClick = {
                        viewModel.updateUser(client.copy(isSuspended = false, suspensionReason = "", suspensionUntil = null))
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Lever la suspension")
                    }
                } else {
                    Button(onClick = { showSuspendDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Suspendre le client")
                    }
                }
                
                Button(onClick = { showLogs = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Voir Activité")
                }

                OutlinedButton(onClick = {
                    viewModel.updateUser(client.copy(isArchived = true))
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer (Vers Corbeille)", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(onClick = {
                    viewModel.updateUser(client.copy(isArchived = false))
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Restaurer le compte")
                }
                
                OutlinedButton(onClick = {
                    viewModel.deleteUser(client.id)
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer Définitivement", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    if (showPromoDialog) {
        var isPermanentInput by remember { mutableStateOf(true) }
        var durationHours by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            title = { Text(if (client.canUsePromo) "Bloquer Promos" else "Débloquer Promos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (client.canUsePromo) "Voulez-vous empêcher ce client d'utiliser des codes promos ?" else "Voulez-vous autoriser ce client à utiliser des codes promos ?")
                    if (client.canUsePromo) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPermanentInput, onCheckedChange = { isPermanentInput = it })
                            Text("Blocage Permanent")
                        }
                        if (!isPermanentInput) {
                            OutlinedTextField(
                                value = durationHours,
                                onValueChange = { durationHours = it },
                                label = { Text("Durée du blocage (Heures)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val block = client.canUsePromo
                    val durHours = durationHours.toLongOrNull() ?: 0L
                    val until = if (block && !isPermanentInput && durHours > 0) System.currentTimeMillis() + (durHours * 60 * 60 * 1000L) else null
                    
                    viewModel.updateUser(client.copy(canUsePromo = !block, canUsePromoUntil = until))
                    showPromoDialog = false
                    onDismiss()
                }) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showSuspendDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text("Suspendre \${client.prenom}") },
            text = {
                OutlinedTextField(
                    value = suspensionReason,
                    onValueChange = { suspensionReason = it },
                    label = { Text("Raison (Visible par le client)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (suspensionReason.isNotBlank()) {
                        viewModel.updateUser(client.copy(isSuspended = true, suspensionReason = suspensionReason))
                        showSuspendDialog = false
                        onDismiss()
                    }
                }) {
                    Text("Suspendre", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showAntiFraudDialog) {
        var autoBanEnabled by remember { mutableStateOf(client.customAutoBanEnabled) }
        var autoBanThreshold by remember { mutableStateOf(client.customAutoBanThreshold.toString()) }
        var autoBanDuration by remember { mutableStateOf(client.customAutoBanDurationHours.toString()) }

        AlertDialog(
            onDismissRequest = { showAntiFraudDialog = false },
            title = { Text("Anti-Fraude Personnalisée") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoBanEnabled, onCheckedChange = { autoBanEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (autoBanEnabled) "Activé pour ce client" else "Utilise la config globale", fontWeight = FontWeight.Bold)
                    }
                    if (autoBanEnabled) {
                        OutlinedTextField(
                            value = autoBanThreshold,
                            onValueChange = { autoBanThreshold = it },
                            label = { Text("Seuil d'annulation toléré (Ex: 3)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = autoBanDuration,
                            onValueChange = { autoBanDuration = it },
                            label = { Text("Durée du ban (Heures)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val threshold = autoBanThreshold.toIntOrNull() ?: 0
                    val duration = autoBanDuration.toIntOrNull() ?: 0
                    
                    viewModel.updateUser(client.copy(
                        customAutoBanEnabled = autoBanEnabled,
                        customAutoBanThreshold = threshold,
                        customAutoBanDurationHours = duration
                    ))
                    showAntiFraudDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAntiFraudDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showBlockOptionsDialog) {
        var canOrder by remember { mutableStateOf(client.canOrder) }
        var canSelectPieces by remember { mutableStateOf(client.canSelectPieces) }
        var canSelectCategories by remember { mutableStateOf(client.canSelectCategories) }
        var canEstimateKg by remember { mutableStateOf(client.canEstimateKg) }
        var canUseExpress by remember { mutableStateOf(client.canUseExpress) }
        var canUsePremium by remember { mutableStateOf(client.canUsePremium) }

        AlertDialog(
            onDismissRequest = { showBlockOptionsDialog = false },
            title = { Text("Personnaliser Autorisations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = canOrder, onCheckedChange = { canOrder = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (canOrder) "Création de commande autorisée" else "Création de commande bloquée", fontWeight = FontWeight.Bold)
                    }
                    if (canOrder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectPieces, onCheckedChange = { canSelectPieces = it })
                            Text("Saisie du nombre de pièces", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectCategories, onCheckedChange = { canSelectCategories = it })
                            Text("Sélection de catégories", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canEstimateKg, onCheckedChange = { canEstimateKg = it })
                            Text("Estimation des kilos", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUseExpress, onCheckedChange = { canUseExpress = it })
                            Text("Service Express", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUsePremium, onCheckedChange = { canUsePremium = it })
                            Text("Garantie Premium", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUser(client.copy(
                        canOrder = canOrder,
                        canSelectPieces = canSelectPieces,
                        canSelectCategories = canSelectCategories,
                        canEstimateKg = canEstimateKg,
                        canUseExpress = canUseExpress,
                        canUsePremium = canUsePremium
                    ))
                    showBlockOptionsDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockOptionsDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showEditProfile) {
        EditClientProfileDialog(client = client, viewModel = viewModel, onDismiss = { showEditProfile = false; onDismiss() })
    }

    if (showLogs) {
        ClientLogsDialog(client = client, viewModel = viewModel, onDismiss = { showLogs = false })
    }
}
`;

content = content.replace(clientBottomSheetRegex, newClientBottomSheet + "\n");
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', content);
console.log("Updated AdminClientsTab and ClientBottomSheet");
