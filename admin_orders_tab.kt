fun AdminOrdersTab(padding: PaddingValues, allOrders: List<OrderEntity>, viewModel: AppViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredOrders = allOrders.filter { order ->
        if (searchQuery.isBlank()) true
        else order.id.toString().contains(searchQuery) || order.userId.contains(searchQuery, ignoreCase = true)
    }
    
    val enAttente = filteredOrders.count { it.status == "EN_ATTENTE" || it.status == "PROPOSITION_ADMIN" }
    val acceptees = filteredOrders.count { it.status == "ACCEPTE" || it.status == "EN_COURS" }
    val rejetees = filteredOrders.count { it.status == "REJETE" }
    val terminees = filteredOrders.count { it.status == "TERMINE" }
    val activeOrders = filteredOrders.filter { it.status == "EN_ATTENTE" || it.status == "PROPOSITION_ADMIN" }
    val processedOrders = filteredOrders.filter { it.status != "EN_ATTENTE" && it.status != "PROPOSITION_ADMIN" }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher (ID Commande, Client ID)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistiques Générales", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            exportOrdersToCSV(context, allOrders, "Toutes_Commandes.csv")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Exporter tout (CSV)", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(enAttente.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Text("En cours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(acceptees.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Acceptées", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rejetees.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Rejetées", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(allOrders.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Commandes à traiter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        items(activeOrders) { order ->
            AdminOrderCard(order = order, viewModel = viewModel)
        }
        
        if (processedOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Commandes traitées", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(processedOrders) { order ->
                AdminOrderCard(order = order, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AdminOrderCard(order: OrderEntity, viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageOrders = hasPermission(currentUser, "GERER_COMMANDES")
    var isEditing by remember { mutableStateOf(false) }
    
    var piecesInput by remember { mutableStateOf(order.pieces.toString()) }
    var categorieInput by remember { mutableStateOf(order.categorie) }
    var lavageTypeInput by remember { mutableStateOf(order.lavageType) }
    var gommagePaireInput by remember { mutableStateOf(order.gommagePaire.toString()) }
    var gommageImpairInput by remember { mutableStateOf(order.gommageImpair.toString()) }
    var kgInput by remember { mutableStateOf(if (order.kg > 0) order.kg.toString() else "") }
    var adminMessageInput by remember { mutableStateOf(order.adminMessage) }
    
    var isExpressInput by remember { mutableStateOf(order.isExpress) }
    var deliveryTypeInput by remember { mutableStateOf(order.deliveryType) }
    var pickupDateInput by remember { mutableStateOf(order.pickupDate) }
    var deliveryDateInput by remember { mutableStateOf(order.deliveryDate) }
    var instructionsSpecialesInput by remember { mutableStateOf(order.instructionsSpeciales) }
    var hasPremiumGuaranteeInput by remember { mutableStateOf(order.hasPremiumGuarantee) }
    var promoCodeInput by remember { mutableStateOf(order.promoCode) }
    var adresseLivraisonInput by remember { mutableStateOf(order.adresseLivraison) }
    var photoUriInput by remember { mutableStateOf(order.photoUri) }

    val config by viewModel.config.collectAsStateWithLifecycle()
    
    val statusColor = when(order.status) {
        "EN_ATTENTE" -> MaterialTheme.colorScheme.tertiary
        "PROPOSITION_ADMIN" -> MaterialTheme.colorScheme.secondary
        "ACCEPTE" -> MaterialTheme.colorScheme.primary
        "TERMINE" -> Color(0xFF4CAF50)
        "REJETE" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Commande #${order.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (order.isExpress) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("URGENT", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Client ID: ${order.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(order.status.replace("_", " "), color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditing || order.status == "PROPOSITION_ADMIN") {
                // Editing form
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = piecesInput,
                        onValueChange = { piecesInput = it },
                        label = { Text("Pièces") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = categorieInput,
                        onValueChange = { categorieInput = it },
                        label = { Text("Catégorie") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Type de Lavage", style = MaterialTheme.typography.labelSmall)
                val lavageOptions = listOf(
                    "Simple" to "SIMPLE",
                    "Lavage + Séchage + Pliage" to "LAVAGE_SECHAGE_PLIAGE",
                    "Complet (avec repassage)" to "LAVAGE_SECHAGE_REPASSAGE_PLIAGE",
                    "Aucun (Gommage uniquement)" to "AUCUN"
                )
                Column {
                    lavageOptions.forEach { (label, type) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = lavageTypeInput == type,
                                onClick = { lavageTypeInput = type }
                            )
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gommagePaireInput,
                        onValueChange = { gommagePaireInput = it },
                        label = { Text("Gom. Paires") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = gommageImpairInput,
                        onValueChange = { gommageImpairInput = it },
                        label = { Text("Gom. Impairs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = kgInput,
                    onValueChange = { kgInput = it },
                    label = { Text("Poids (Kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isExpressInput, onCheckedChange = { isExpressInput = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Service Express (Urgent)", style = MaterialTheme.typography.bodyMedium)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hasPremiumGuaranteeInput, onCheckedChange = { hasPremiumGuaranteeInput = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Garantie Premium", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Type de Livraison", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = deliveryTypeInput == "AGENCE", onClick = { deliveryTypeInput = "AGENCE" })
                    Text("Dépôt Agence", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = deliveryTypeInput == "DOMICILE", onClick = { deliveryTypeInput = "DOMICILE" })
                    Text("Livraison Domicile", style = MaterialTheme.typography.bodySmall)
                }

                if (deliveryTypeInput == "DOMICILE") {
                    OutlinedTextField(
                        value = adresseLivraisonInput,
                        onValueChange = { adresseLivraisonInput = it },
                        label = { Text("Adresse de Livraison") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pickupDateInput,
                        onValueChange = { pickupDateInput = it },
                        label = { Text("Date/Heure Ramassage") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = deliveryDateInput,
                        onValueChange = { deliveryDateInput = it },
                        label = { Text("Date/Heure Livraison") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructionsSpecialesInput,
                    onValueChange = { instructionsSpecialesInput = it },
                    label = { Text("Instructions Spéciales") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = promoCodeInput,
                    onValueChange = { promoCodeInput = it },
                    label = { Text("Code Promo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { photoUriInput = "mock_admin_photo_${System.currentTimeMillis()}" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUriInput.isNotBlank()) "Photo des tâches ajoutée ✓" else "Prendre une photo des tâches")
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = adminMessageInput,
                    onValueChange = { adminMessageInput = it },
                    label = { Text("Message pour le client (Optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val kg = kgInput.toFloatOrNull() ?: 0f
                            val paires = gommagePaireInput.toIntOrNull() ?: 0
                            val impairs = gommageImpairInput.toIntOrNull() ?: 0
                            val pieces = piecesInput.toIntOrNull() ?: 0
                            
                            val updatedOrder = order.copy(
                                pieces = pieces,
                                categorie = categorieInput,
                                lavageType = lavageTypeInput,
                                gommagePaire = paires,
                                gommageImpair = impairs,
                                kg = kg,
                                isExpress = isExpressInput,
                                deliveryType = deliveryTypeInput,
                                pickupDate = pickupDateInput,
                                deliveryDate = deliveryDateInput,
                                instructionsSpeciales = instructionsSpecialesInput,
                                hasPremiumGuarantee = hasPremiumGuaranteeInput,
                                promoCode = promoCodeInput,
                                adresseLivraison = adresseLivraisonInput,
                                photoUri = photoUriInput,
                                status = "PROPOSITION_ADMIN",
                                adminMessage = adminMessageInput
                            )
                            val newPrice = calculatePrice(updatedOrder, kg, config)
                            val finalOrder = updatedOrder.copy(prixFinal = newPrice)
                            
                            viewModel.updateOrder(finalOrder)
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmer et envoyer la modification")
                    }
                    if (order.status != "PROPOSITION_ADMIN") {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Annuler")
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Catégorie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.categorie, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("Pièces", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.pieces.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Type de Lavage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.lavageType, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                if (order.gommagePaire > 0 || order.gommageImpair > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gommage : ${order.gommagePaire} paires, ${order.gommageImpair} impairs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                
                if (order.hasPremiumGuarantee) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🛡️ Garantie Premium Souscrite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }

                if (order.photoUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📸 Photo du linge jointe", style = MaterialTheme.typography.bodySmall)
                }

                if (order.pickupDate.isNotEmpty() || order.deliveryDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Horaires souhaités :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (order.pickupDate.isNotEmpty()) Text("Dépôt/Ramassage : ${order.pickupDate}", style = MaterialTheme.typography.bodySmall)
                    if (order.deliveryDate.isNotEmpty()) Text("Retrait/Livraison : ${order.deliveryDate}", style = MaterialTheme.typography.bodySmall)
                }

                if (order.deliveryType == "DOMICILE") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📍 Livraison à domicile: ${order.adresseLivraison} (${order.distanceKm} Km)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🏢 Dépôt en agence", style = MaterialTheme.typography.bodySmall)
                }
                
                if (order.specialItemsData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Articles Spéciaux: ${order.specialItemsData}", style = MaterialTheme.typography.bodySmall)
                }
                
                if (order.instructionsSpeciales.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Instructions: ${order.instructionsSpeciales}", style = MaterialTheme.typography.bodySmall)
                }
                
                if (order.promoCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (order.discountFixed > 0) {
                        Text("🎟️ Code Promo utilisé: ${order.promoCode} (-${order.discountFixed} FCFA)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Text("🎟️ Code Promo utilisé: ${order.promoCode} (-${order.discountPercentage * 100}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Poids final", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (order.kg > 0) {
                            Text("${order.kg} Kg", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Non pesé", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prix Calculé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${order.prixFinal} FCFA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (order.adminMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Message envoyé : ${order.adminMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (canManageOrders) {
                    if (order.status == "EN_ATTENTE") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // OutlinedTextField for Kg (mandatory for accepting directly)
                    OutlinedTextField(
                        value = kgInput,
                        onValueChange = { kgInput = it },
                        label = { Text("Saisir le poids (Kg) avant d'accepter") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val kg = kgInput.toFloatOrNull() ?: 0f
                                    val newPrice = calculatePrice(order, kg, config)
                                    viewModel.updateOrder(order.copy(status = "ACCEPTE", kg = kg, prixFinal = newPrice))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accepter")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateOrder(order.copy(status = "REJETE"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejeter")
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Modifier la commande")
                        }
                    }
                }
                } // End of canManageOrders
                
                if (canManageOrders && order.status == "ACCEPTE") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Avancement", style = MaterialTheme.typography.labelSmall)
                    val timelineOptions = listOf(
                        "Réceptionnée & Pesée" to 1,
                        "En plein lavage" to 2,
                        "Séchage & Repassage" to 3,
                        "Prête" to 4,
                        "Terminée (Livrée/Récupérée)" to 5
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        timelineOptions.forEach { (label, step) ->
                            val isSelected = order.timelineStep == step
                            val isPast = order.timelineStep > step
                            
                            Button(
                                onClick = {
                                    val newStatus = if (step == 5) "TERMINE" else "ACCEPTE"
                                    viewModel.updateOrder(order.copy(timelineStep = step, status = newStatus))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else if (isPast) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected || isPast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                
                if (order.status == "ACCEPTE" || order.status == "TERMINE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportInvoiceToPDF(context, order) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger Facture PDF")
                    }
                }
                
                if (order.status != "REJETE" && order.status != "TERMINE" && order.status != "EN_ATTENTE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.updateOrder(order.copy(status = "REJETE")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Forcer l'annulation de la commande")
                    }
                }
            }
        }
    }
}

fun calculatePrice(order: OrderEntity, kg: Float, config: com.example.data.ConfigEntity?): Float {
    val prixLavage = when (order.lavageType) {
        "SIMPLE" -> 350f * kg
        "LAVAGE_SECHAGE_PLIAGE" -> 400f * kg
        "LAVAGE_SECHAGE_REPASSAGE_PLIAGE" -> 500f * kg
        else -> 0f
    }
    
    val prixGommage = (order.gommagePaire * 1500f) + (order.gommageImpair * 2000f)
    
    var prixSpecial = 0f
    if (order.specialItemsData.isNotBlank()) {
        order.specialItemsData.split(";").forEach { item ->
            val parts = item.split("|")
            if (parts.size == 3) {
                val price = parts[1].toFloatOrNull() ?: 0f
                val qty = parts[2].toIntOrNull() ?: 0
                prixSpecial += price * qty
            }
        }
    }
    
    val sousTotal = prixLavage + prixGommage + prixSpecial
    val insuranceFee = if (order.hasPremiumGuarantee) sousTotal * 0.1f else 0f
    
    val multiplier = if (order.isExpress) (config?.expressMultiplier ?: 1.3f) else 1.0f
    val discount = 1f - order.discountPercentage
    val totalAvecExpress = maxOf(0f, ((sousTotal + insuranceFee) * multiplier * discount) - order.discountFixed)
    
    val transport = if (order.deliveryType == "DOMICILE") {
        (config?.baseTransportFee ?: 1000f) + (order.distanceKm * (config?.pricePerKm ?: 500f))
    } else {
        0f
    }
    
    return totalAvecExpress + transport
}

