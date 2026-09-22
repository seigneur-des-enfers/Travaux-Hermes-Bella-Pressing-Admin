const fs = require('fs');

let code = fs.readFileSync('admin_order_card.kt', 'utf8');

// I need to add camera, special items, map and calendar
let newCode = `@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderCard(order: OrderEntity, viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
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
    
    var specialItemsMap = remember { mutableStateMapOf<String, Int>() }
    
    LaunchedEffect(order) {
        if (order.specialItemsData.isNotBlank()) {
            specialItemsMap.clear()
            order.specialItemsData.split(",").forEach {
                val parts = it.split(":")
                if (parts.size == 2) {
                    specialItemsMap[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            photoUriInput = "photo_\${System.currentTimeMillis()}.png"
        }
    }

    val statusColor = when(order.status) {
        "EN_ATTENTE" -> MaterialTheme.colorScheme.tertiary
        "PROPOSITION_ADMIN" -> MaterialTheme.colorScheme.secondary
        "ACCEPTE" -> MaterialTheme.colorScheme.primary
        "TERMINE" -> Color(0xFF4CAF50)
        "REJETE" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    val timeOptions = (0..23).flatMap { listOf(String.format("%02d:00", it), String.format("%02d:30", it)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Commande #\${order.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (order.isExpress) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("URGENT", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Client ID: \${order.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(order.status.replace("_", " "), color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditing || order.status == "PROPOSITION_ADMIN") {
                // Editing form
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = piecesInput, onValueChange = { piecesInput = it }, label = { Text("Pièces") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = categorieInput, onValueChange = { categorieInput = it }, label = { Text("Catégorie") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Type de Lavage", style = MaterialTheme.typography.labelSmall)
                val lavageOptions = listOf("Simple" to "SIMPLE", "Lavage + Séchage + Pliage" to "LAVAGE_SECHAGE_PLIAGE", "Complet (avec repassage)" to "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "Aucun (Gommage uniquement)" to "AUCUN")
                Column {
                    lavageOptions.forEach { (label, type) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = lavageTypeInput == type, onClick = { lavageTypeInput = type })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gommagePaireInput, onValueChange = { gommagePaireInput = it }, label = { Text("Gom. Paires") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = gommageImpairInput, onValueChange = { gommageImpairInput = it }, label = { Text("Gom. Impairs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = kgInput, onValueChange = { kgInput = it }, label = { Text("Poids (Kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isExpressInput, onCheckedChange = { isExpressInput = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Express (Multiplicateur x\${config?.expressMultiplier ?: 1.3f})")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Articles Spéciaux", style = MaterialTheme.typography.labelMedium)
                specialItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                if (current > 0) specialItemsMap[item.name] = current - 1
                            }) { Text("-") }
                            Text("\${specialItemsMap[item.name] ?: 0}")
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                specialItemsMap[item.name] = current + 1
                            }) { Text("+") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dates & Heures", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pickupDateInput, onValueChange = { pickupDateInput = it }, label = { Text("Jour Ramassage (Ex: 10/10)") }, modifier = Modifier.weight(1f))
                    var expandedPickTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedPickTime, onExpandedChange = { expandedPickTime = !expandedPickTime }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = pickupDateInput.substringAfter(" ", ""),
                            onValueChange = { },
                            label = { Text("Heure") },
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPickTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedPickTime, onDismissRequest = { expandedPickTime = false }) {
                            timeOptions.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { pickupDateInput = pickupDateInput.substringBefore(" ") + " " + t; expandedPickTime = false }) }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = deliveryDateInput, onValueChange = { deliveryDateInput = it }, label = { Text("Jour Livraison (Ex: 12/10)") }, modifier = Modifier.weight(1f))
                    var expandedDelTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedDelTime, onExpandedChange = { expandedDelTime = !expandedDelTime }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = deliveryDateInput.substringAfter(" ", ""),
                            onValueChange = { },
                            label = { Text("Heure") },
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDelTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedDelTime, onDismissRequest = { expandedDelTime = false }) {
                            timeOptions.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { deliveryDateInput = deliveryDateInput.substringBefore(" ") + " " + t; expandedDelTime = false }) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUriInput.isNotBlank()) "Photo Capturée" else "Prendre une photo des tâches")
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (order.latitude != 0.0) {
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Carte Maps: Lat \${order.latitude}, Lng \${order.longitude}")
                        }
                    }
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { 
                        isEditing = false 
                        piecesInput = order.pieces.toString()
                        categorieInput = order.categorie
                        kgInput = if(order.kg > 0) order.kg.toString() else ""
                    }) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val k = kgInput.toFloatOrNull() ?: 0f
                        var calcPrice = calculateOrderPrice(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            kg = k,
                            lavageType = lavageTypeInput,
                            gommagePaire = gommagePaireInput.toIntOrNull() ?: 0,
                            gommageImpair = gommageImpairInput.toIntOrNull() ?: 0,
                            config = config,
                            isExpress = isExpressInput,
                            isPremium = hasPremiumGuaranteeInput
                        )
                        
                        calcPrice += specialItemsMap.mapNotNull { entry ->
                            val p = specialItems.find { it.name == entry.key }?.price ?: 0f
                            p * entry.value
                        }.sum()
                        
                        val specialItemsStr = specialItemsMap.filter { it.value > 0 }.map { "\${it.key}:\${it.value}" }.joinToString(",")

                        val updated = order.copy(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            categorie = categorieInput,
                            lavageType = lavageTypeInput,
                            gommagePaire = gommagePaireInput.toIntOrNull() ?: 0,
                            gommageImpair = gommageImpairInput.toIntOrNull() ?: 0,
                            kg = k,
                            prixFinal = calcPrice,
                            adminMessage = adminMessageInput,
                            status = "PROPOSITION_ADMIN",
                            specialItemsData = specialItemsStr,
                            pickupDate = pickupDateInput,
                            deliveryDate = deliveryDateInput,
                            photoUri = photoUriInput
                        )
                        viewModel.updateOrder(updated)
                        isEditing = false
                    }) {
                        Text("Envoyer Proposition")
                    }
                }
            } else {
                // View mode
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("\${order.pieces} pièces • \${order.categorie}", style = MaterialTheme.typography.bodyMedium)
                        Text("\${order.prixFinal.toInt()} FCFA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (order.kg > 0) {
                        Text("Poids estimé: \${order.kg} Kg", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (order.specialItemsData.isNotBlank()) {
                        Text("Articles Spéciaux:", style = MaterialTheme.typography.labelMedium)
                        order.specialItemsData.split(",").forEach {
                            Text("- \$it", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (order.pickupDate.isNotBlank()) Text("Ramassage: \${order.pickupDate}", style = MaterialTheme.typography.bodySmall)
                    if (order.deliveryDate.isNotBlank()) Text("Livraison: \${order.deliveryDate}", style = MaterialTheme.typography.bodySmall)

                    if (order.status == "EN_ATTENTE" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Traiter (Peser/Proposer)")
                            }
                            OutlinedButton(
                                onClick = { 
                                    viewModel.updateOrder(order.copy(status = "REJETE"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rejeter", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (order.status == "ACCEPTE" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.updateOrder(order.copy(status = "EN_COURS", timelineStep = 2)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Démarrer le Lavage")
                        }
                    } else if (order.status == "EN_COURS" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.updateOrder(order.copy(timelineStep = 3)) }, modifier = Modifier.weight(1f)) {
                                Text("Séchage/Repassage")
                            }
                            Button(onClick = { viewModel.updateOrder(order.copy(status = "TERMINE", timelineStep = 5)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                                Text("Terminer")
                            }
                        }
                    }
                }
            }
        }
    }
}
`

let mainCode = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');
let regex = /@Composable\s+fun AdminOrderCard[\s\S]*?(?=\n\/\/|$)/g;
// actually there might not be a comment at the end. I will just replace the exact text extracted.
mainCode = mainCode.replace(code, newCode + "\n");
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', mainCode);
console.log("AdminOrderCard replaced.");
