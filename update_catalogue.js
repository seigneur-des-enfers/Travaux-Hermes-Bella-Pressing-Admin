const fs = require('fs');
let content = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

let catalogueTabOld = fs.readFileSync('admin_catalogue_tab.kt', 'utf8');

let catalogueTabNew = `@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCatalogueTab(padding: PaddingValues, viewModel: AppViewModel) {
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()
    
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
    var promoDurationHours by remember { mutableStateOf("") }
    var promoDiscountType by remember { mutableStateOf("PERCENTAGE") } // PERCENTAGE or FIXED_AMOUNT
    var promoTargetItems by remember { mutableStateOf("ALL") }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManagePromos = currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("PROMOS_AVANCEES") == true
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Catalogue: Articles Spéciaux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { showSpecialDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Article")
                }
            }
        }
        
        if (specialItems.isEmpty()) {
            item {
                Text("Aucun article spécial configuré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(specialItems) { item ->
            Card(modifier = Modifier.fillMaxWidth().clickable {
                editingSpecialItem = item
                specialNameInput = item.name
                specialPriceInput = item.price.toString()
                showSpecialDialog = true
            }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text("\${item.price} FCFA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.deleteSpecialItem(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Codes Promo & Événements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canManagePromos) {
                    Button(onClick = { showPromoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Promo")
                    }
                }
            }
        }
        
        items(allPromos) { promo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (promo.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(promo.code, fontWeight = FontWeight.Bold)
                        Text(promo.description, style = MaterialTheme.typography.bodySmall)
                        val discountText = if (promo.discountType == "PERCENTAGE") {
                            "\${(promo.discountPercent * 100).toInt()}%"
                        } else {
                            "\${(promo.discountPercent).toInt()} FCFA"
                        }
                        Text("Réduction: \$discountText | Cible: \${promo.targetItems}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (promo.freeKg > 0) Text("Kg Offerts: \${promo.freeKg}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        Text("Type: \${if (promo.isPermanent) "Permanent" else "Temporaire (\${promo.validUntil})\"}", style = MaterialTheme.typography.bodySmall)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = promoDurationDays,
                                onValueChange = { promoDurationDays = it },
                                label = { Text("Jours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = promoDurationHours,
                                onValueChange = { promoDurationHours = it },
                                label = { Text("Heures") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    var expandedTarget by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTarget, onExpandedChange = { expandedTarget = !expandedTarget }) {
                        OutlinedTextField(
                            value = promoTargetItems,
                            onValueChange = { promoTargetItems = it },
                            label = { Text("Cibles (Toutes, Articles, Services...)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) }
                        )
                        ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            listOf("ALL", "COMMANDE_COMPLETE", "LAVAGE_SIMPLE", "LAVAGE_SECHAGE_PLIAGE").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option else "$promoTargetItems,$option"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            specialItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Article: \${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,\${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                        }
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
                    val durationDays = promoDurationDays.toLongOrNull() ?: 0L
                    val durationHoursAmount = promoDurationHours.toLongOrNull() ?: 0L
                    val durationMs = (durationDays * 24 * 60 * 60 * 1000L) + (durationHoursAmount * 60 * 60 * 1000L)
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
                            targetItems = promoTargetItems,
                            discountType = promoDiscountType
                        )
                    }
                    showPromoDialog = false
                    promoCodeInput = ""
                    promoDescInput = ""
                    promoDiscountInput = ""
                    promoFreeKgInput = ""
                    promoDurationDays = ""
                    promoDurationHours = ""
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
}`;

content = content.replace(catalogueTabOld, catalogueTabNew);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', content);
console.log("Updated AdminCatalogueTab");
