fun AdminClientsTab(padding: PaddingValues, clients: List<UserEntity>, allOrders: List<OrderEntity>, viewModel: AppViewModel) {
    val context = LocalContext.current
    var selectedClient by remember { mutableStateOf<UserEntity?>(null) }

    val activeClients = clients.filter { !it.isArchived }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Clients Inscrits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (activeClients.isEmpty()) {
            item {
                Text("Aucun client n'est encore inscrit.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(activeClients) { client ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (client.isSuspended) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${client.prenom} ${client.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(client.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tél: ${client.telephone}", style = MaterialTheme.typography.bodyMedium)
                    if (client.coordonnees.isNotEmpty()) {
                        Text("Adresse: ${client.coordonnees}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (client.isSuspended) {
                        Text("COMPTE SUSPENDU", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val userOrders = allOrders.filter { it.userId == client.id }
                                exportOrdersToCSV(context, userOrders, "Commandes_${client.nom}.csv")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporter (CSV)")
                        }
                        Button(
                            onClick = { selectedClient = client },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gérer")
                        }
                    }
                }
            }
        }
    }

    if (selectedClient != null) {
        ClientManagementDialog(
            client = selectedClient!!,
            viewModel = viewModel,
            onDismiss = { selectedClient = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientManagementDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showEditProfile by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showAntiFraudDialog by remember { mutableStateOf(false) }
    var showBlockOptionsDialog by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canSuspend = hasPermission(currentUser, "SUSPENDRE_CLIENTS")
    val canEdit = hasPermission(currentUser, "GERER_CLIENTS")
    val canViewLogs = hasPermission(currentUser, "VOIR_LOGS")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gérer: ${client.prenom} ${client.nom}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Suspendre / Débloquer
                if (canSuspend) {
                    Button(
                        onClick = {
                            if (client.isSuspended) {
                                viewModel.updateUser(client.copy(isSuspended = false, suspensionReason = "", suspensionUntil = null))
                                viewModel.logAdminAction(viewModel.currentUser.value?.id, client.id, "UNBAN", "L'administrateur a débloqué le compte.")
                                onDismiss()
                            } else {
                                showSuspendDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (client.isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (client.isSuspended) "Débloquer l'utilisateur" else "Suspendre l'utilisateur")
                    }
                }
                
                if (canEdit) {
                    // Modifier Profil
                    Button(onClick = { showEditProfile = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Éditer le profil & mot de passe")
                    }
                }
                
                if (canViewLogs) {
                    // Voir les logs
                    Button(onClick = { showLogs = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voir le journal d'activité")
                    }
                }
                
                if (canEdit) {
                    // Autorisations Commandes
                    OutlinedButton(
                        onClick = {
                            showBlockOptionsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Personnaliser les autorisations")
                    }
                }
                
                if (canEdit) {
                    // Toggle Promos
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(canUsePromo = !client.canUsePromo))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.canUsePromo) "Bloquer l'utilisation de promos" else "Autoriser les promos")
                    }
                    
                    // Anti-fraud settings
                    OutlinedButton(onClick = { showAntiFraudDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Personnaliser Anti-Fraude")
                    }
                }
                
                if (canSuspend) {
                    // Supprimer le compte (Archive)
                    TextButton(
                        onClick = {
                            viewModel.updateUser(client.copy(isArchived = true))
                            viewModel.logAdminAction(viewModel.currentUser.value?.id, client.id, "ARCHIVE", "L'administrateur a archivé ce compte (suppression douce).")
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Archiver (Supprimer) le compte")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )

    if (showSuspendDialog) {
        var reason by remember { mutableStateOf("") }
        var durationHours by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text("Suspendre l'utilisateur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motif de suspension") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it },
                        label = { Text("Durée (en heures, vide = permanent)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val hours = durationHours.toLongOrNull()
                    val until = if (hours != null) System.currentTimeMillis() + (hours * 60 * 60 * 1000) else null
                    viewModel.updateUser(client.copy(
                        isSuspended = true,
                        suspensionReason = reason.ifBlank { "Non spécifié" },
                        suspensionUntil = until
                    ))
                    viewModel.logAdminAction(viewModel.currentUser.value?.id, client.id, "BAN", "Compte suspendu pour le motif: $reason")
                    showSuspendDialog = false
                    onDismiss()
                }) {
                    Text("Valider la suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showAntiFraudDialog) {
        var autoBanEnabled by remember { mutableStateOf(client.customAutoBanEnabled) }
        var autoBanThreshold by remember { mutableStateOf(if (client.customAutoBanThreshold > 0) client.customAutoBanThreshold.toString() else "") }
        var autoBanDuration by remember { mutableStateOf(if (client.customAutoBanDurationHours > 0) client.customAutoBanDurationHours.toString() else "") }
        
        AlertDialog(
            onDismissRequest = { showAntiFraudDialog = false },
            title = { Text("Anti-Fraude Personnalisé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Laissez les champs vides pour utiliser la configuration globale (Défaut).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoBanEnabled, onCheckedChange = { autoBanEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le banissement pour ce client")
                    }
                    
                    if (autoBanEnabled) {
                        OutlinedTextField(
                            value = autoBanThreshold,
                            onValueChange = { autoBanThreshold = it },
                            label = { Text("Seuil de rejets consécutifs (0 = global)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = autoBanDuration,
                            onValueChange = { autoBanDuration = it },
                            label = { Text("Durée de ban en heures (0 = global)") },
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

@Composable
fun EditClientProfileDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var nom by remember { mutableStateOf(client.nom) }
    var prenom by remember { mutableStateOf(client.prenom) }
    var telephone by remember { mutableStateOf(client.telephone) }
    var coordonnees by remember { mutableStateOf(client.coordonnees) }
    var password by remember { mutableStateOf(client.motDePasse) }
    var fidelityKg by remember { mutableStateOf(client.fidelityPointsKg.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Éditer le profil") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") })
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") })
                OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text("Téléphone") })
                OutlinedTextField(value = coordonnees, onValueChange = { coordonnees = it }, label = { Text("Adresse") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") })
                OutlinedTextField(
                    value = fidelityKg,
                    onValueChange = { fidelityKg = it },
                    label = { Text("Kg de fidélité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedClient = client.copy(
                    nom = nom,
                    prenom = prenom,
                    telephone = telephone,
                    coordonnees = coordonnees,
                    motDePasse = password,
                    fidelityPointsKg = fidelityKg.toFloatOrNull() ?: client.fidelityPointsKg
                )
                viewModel.updateUser(updatedClient)
                onDismiss()
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun ClientLogsDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val logs by viewModel.getActivityLogsForUser(client.id).collectAsStateWithLifecycle(initialValue = emptyList<ActivityLogEntity>())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activité de ${client.prenom}") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(logs) { log ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(log.description, style = MaterialTheme.typography.bodySmall)
                        val dateString = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(log.timestamp))
                        Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

