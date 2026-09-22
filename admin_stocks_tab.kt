fun AdminStocksTab(padding: PaddingValues, viewModel: AppViewModel) {
    val consumables by viewModel.allConsumables.collectAsStateWithLifecycle()
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageStocks = hasPermission(currentUser, "GERER_STOCKS")
    
    var showDialog by remember { mutableStateOf(false) }
    var showSpecialDialog by remember { mutableStateOf(false) }
    
    var nameInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf("") }
    
    var specialNameInput by remember { mutableStateOf("") }
    var specialPriceInput by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gestion des Stocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canManageStocks) {
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter Stock")
                    }
                }
            }
        }
        
        if (consumables.isEmpty()) {
            item {
                Text("Aucun stock enregistré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(consumables) { consumable ->
            val isLowStock = consumable.quantity <= consumable.threshold
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(consumable.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isLowStock) {
                            Text("Alerte: Stock bas !", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Seuil d'alerte: ${consumable.threshold}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (canManageStocks) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                if(consumable.quantity > 0) viewModel.updateConsumableQuantity(consumable, consumable.quantity - 1) 
                            }) {
                                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text("${consumable.quantity}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { 
                                viewModel.updateConsumableQuantity(consumable, consumable.quantity + 1) 
                            }) {
                                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteConsumable(consumable.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text("${consumable.quantity} restants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Ajouter un Produit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nom du produit (Ex: Savon, Eau de Javel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantité initiale") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it },
                        label = { Text("Seuil d'alerte (Ex: 5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = quantityInput.toIntOrNull() ?: 0
                    val t = thresholdInput.toIntOrNull() ?: 0
                    if (nameInput.isNotBlank()) {
                        viewModel.addConsumable(nameInput, q, t)
                    }
                    showDialog = false
                    nameInput = ""
                    quantityInput = ""
                    thresholdInput = ""
                }) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

