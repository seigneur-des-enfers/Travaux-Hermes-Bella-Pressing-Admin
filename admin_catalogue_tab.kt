fun AdminCatalogueTab(padding: PaddingValues, viewModel: AppViewModel) {
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val promos by viewModel.allPromos.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageCatalogue = hasPermission(currentUser, "GERER_CATALOGUE")
    val canManagePromos = hasPermission(currentUser, "GERER_PROMOS")
    
    var showSpecialDialog by remember { mutableStateOf(false) }
    var specialNameInput by remember { mutableStateOf("") }
    var specialPriceInput by remember { mutableStateOf("") }
    
    var editingSpecialItem by remember { mutableStateOf<SpecialItemEntity?>(null) }
    
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCodeInput by remember { mutableStateOf("") }
    var promoDescInput by remember { mutableStateOf("") }
    var promoDiscountInput by remember { mutableStateOf("") }
    var promoFreeKgInput by remember { mutableStateOf("") }
    var promoIsPermanent by remember { mutableStateOf(true) }
    var promoDurationDays by remember { mutableStateOf("") }
    var promoDiscountType by remember { mutableStateOf("PERCENTAGE") }
    var promoTargetItemId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Articles Spéciaux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canManageCatalogue) {
                    Button(onClick = { showSpecialDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }
            }
        }

        if (specialItems.isEmpty()) {
            item {
                Text("Aucun article spécial enregistré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(specialItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${item.price} FCFA", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    if (canManageCatalogue) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editingSpecialItem = item
                                specialNameInput = item.name
                                specialPriceInput = item.price.toString()
                                showSpecialDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteSpecialItem(item.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Codes Promo & Événements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canManagePromos) {
                    Button(onClick = { showPromoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }
            }
        }
        
        if (promos.isEmpty()) {
            item {
                Text("Aucun code promo actif.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(promos) { promo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (promo.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(promo.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(promo.description, style = MaterialTheme.typography.bodySmall)
                        
                        if (promo.discountPercent > 0) {
                            if (promo.discountType == "PERCENTAGE") {
                                Text("-${promo.discountPercent * 100}% de réduction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("-${promo.discountPercent} FCFA de réduction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        if (promo.freeKg > 0) Text("+${promo.freeKg} Kg offerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        
                        if (!promo.isPermanent && promo.validUntil != null) {
                            Text("Expire le: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(promo.validUntil))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Permanent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    if (canManagePromos) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = promo.isActive, onCheckedChange = { viewModel.togglePromoStatus(promo) })
                            IconButton(onClick = { viewModel.deletePromo(promo.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text(if(promo.isActive) "Actif" else "Inactif", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showSpecialDialog) {
        AlertDialog(
            onDismissRequest = {
                showSpecialDialog = false
                editingSpecialItem = null
            },
            title = { Text(if (editingSpecialItem != null) "Modifier Article Spécial" else "Ajouter un Article Spécial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = specialNameInput,
                        onValueChange = { specialNameInput = it },
                        label = { Text("Nom (Ex: Tapis, Couette)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialPriceInput,
                        onValueChange = { specialPriceInput = it },
                        label = { Text("Prix Unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = specialPriceInput.toFloatOrNull() ?: 0f
                    if (specialNameInput.isNotBlank()) {
                        val currentItem = editingSpecialItem
                        if (currentItem != null) {
                            viewModel.updateSpecialItem(currentItem.copy(name = specialNameInput, price = p))
                        } else {
                            viewModel.addSpecialItem(specialNameInput, p)
                        }
                    }
                    showSpecialDialog = false
                    editingSpecialItem = null
                    specialNameInput = ""
                    specialPriceInput = ""
                }) {
                    Text(if (editingSpecialItem != null) "Sauvegarder" else "Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSpecialDialog = false
                    editingSpecialItem = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            title = { Text("Créer Code Promo / Événement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it.uppercase() },
                        label = { Text("Code (Ex: TABASKI20)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = promoDescInput,
                        onValueChange = { promoDescInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = promoIsPermanent, onCheckedChange = { promoIsPermanent = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (promoIsPermanent) "Promo Permanente" else "Promo Temporaire", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (!promoIsPermanent) {
                        OutlinedTextField(
                            value = promoDurationDays,
                            onValueChange = { promoDurationDays = it },
                            label = { Text("Durée de validité (en jours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Text("Type de réduction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = promoDiscountType == "PERCENTAGE", onClick = { promoDiscountType = "PERCENTAGE" })
                        Text("Pourcentage", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = promoDiscountType == "FIXED_AMOUNT", onClick = { promoDiscountType = "FIXED_AMOUNT" })
                        Text("Montant Fixe", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = promoDiscountInput,
                        onValueChange = { promoDiscountInput = it },
                        label = { Text(if (promoDiscountType == "PERCENTAGE") "Réduction en % (Ex: 20)" else "Réduction en FCFA (Ex: 1000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Offre spéciale", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = promoFreeKgInput,
                        onValueChange = { promoFreeKgInput = it },
                        label = { Text("Kg Offerts (Optionnel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val discount = (promoDiscountInput.toFloatOrNull() ?: 0f) / (if (promoDiscountType == "PERCENTAGE") 100f else 1f)
                    val freeKg = promoFreeKgInput.toFloatOrNull() ?: 0f
                    val durationMs = (promoDurationDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000L
                    val validUntil = if (promoIsPermanent || durationMs == 0L) null else System.currentTimeMillis() + durationMs
                    
                    if (promoCodeInput.isNotBlank()) {
                        viewModel.addPromo(
                            code = promoCodeInput, 
                            description = promoDescInput, 
                            discountPercent = discount, 
                            freeKg = freeKg, 
                            minOrderValue = 0f,
                            validUntil = validUntil,
                            isPermanent = promoIsPermanent,
                            targetItemId = promoTargetItemId,
                            discountType = promoDiscountType
                        )
                    }
                    showPromoDialog = false
                    promoCodeInput = ""
                    promoDescInput = ""
                    promoDiscountInput = ""
                    promoFreeKgInput = ""
                    promoDurationDays = ""
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

