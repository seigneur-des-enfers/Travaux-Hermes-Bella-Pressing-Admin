const fs = require('fs');
let content = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

let settingsTabOld = fs.readFileSync('admin_settings_tab.kt', 'utf8');

let settingsTabNew = `@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val allAntiFraudRules by viewModel.allAntiFraudRules.collectAsStateWithLifecycle()
    
    var showCreateAntiFraudDialog by remember { mutableStateOf(false) }
    var afNameInput by remember { mutableStateOf("") }
    var afTriggerInput by remember { mutableStateOf("") }
    var afThresholdInput by remember { mutableStateOf("3") }
    var afDurationInput by remember { mutableStateOf("24") }
    var afActionInput by remember { mutableStateOf("BAN") }

    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    
    val conf = config!!
    var baseTransportFee by remember { mutableStateOf(conf.baseTransportFee.toString()) }
    var pricePerKm by remember { mutableStateOf(conf.pricePerKm.toString()) }
    var categories by remember { mutableStateOf(conf.categories) }
    var newCategoryInput by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf(conf.businessName) }
    var contactPhone by remember { mutableStateOf(conf.contactPhone) }
    var contactEmail by remember { mutableStateOf(conf.contactEmail) }
    var openingHours by remember { mutableStateOf(conf.openingHours) }
    var closingHours by remember { mutableStateOf(conf.closingHours) }
    var openingDays by remember { mutableStateOf(conf.openingDays) }
    var termsLink by remember { mutableStateOf(conf.termsAndConditionsLink) }
    
    val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    val timeOptions = (0..23).flatMap { listOf(String.format("%02d:00", it), String.format("%02d:30", it)) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Paramètres Généraux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tarification & Livraison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = baseTransportFee,
                        onValueChange = { baseTransportFee = it },
                        label = { Text("Frais de transport de base (FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pricePerKm,
                        onValueChange = { pricePerKm = it },
                        label = { Text("Prix par Km supplémentaire (FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Système & Contacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nom de l'entreprise") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Téléphone de contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Email de contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var expandedDays by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedDays, onExpandedChange = { expandedDays = !expandedDays }) {
                        OutlinedTextField(
                            value = openingDays,
                            onValueChange = { openingDays = it },
                            label = { Text("Jours d'ouverture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDays) }
                        )
                        ExposedDropdownMenu(expanded = expandedDays, onDismissRequest = { expandedDays = false }) {
                            daysOfWeek.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = {
                                        openingDays = if (openingDays.isEmpty()) day else "$openingDays,$day"
                                        expandedDays = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedOpenTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedOpenTime, onExpandedChange = { expandedOpenTime = !expandedOpenTime }) {
                        OutlinedTextField(
                            value = openingHours,
                            onValueChange = { openingHours = it },
                            label = { Text("Heure d'ouverture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOpenTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedOpenTime, onDismissRequest = { expandedOpenTime = false }) {
                            timeOptions.forEach { time ->
                                DropdownMenuItem(
                                    text = { Text(time) },
                                    onClick = { openingHours = time; expandedOpenTime = false }
                                )
                            }
                        }
                    }

                    var expandedCloseTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCloseTime, onExpandedChange = { expandedCloseTime = !expandedCloseTime }) {
                        OutlinedTextField(
                            value = closingHours,
                            onValueChange = { closingHours = it },
                            label = { Text("Heure de fermeture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCloseTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedCloseTime, onDismissRequest = { expandedCloseTime = false }) {
                            timeOptions.forEach { time ->
                                DropdownMenuItem(
                                    text = { Text(time) },
                                    onClick = { closingHours = time; expandedCloseTime = false }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = termsLink,
                        onValueChange = { termsLink = it },
                        label = { Text("Lien des CGU") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catégories Pré-enregistrées", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    var expandedCats by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCats, onExpandedChange = { expandedCats = !expandedCats }) {
                        OutlinedTextField(
                            value = "Voir ou selectionner les catégories",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCats) }
                        )
                        ExposedDropdownMenu(expanded = expandedCats, onDismissRequest = { expandedCats = false }) {
                            categories.split(",").filter { it.isNotBlank() }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { expandedCats = false }
                                )
                            }
                        }
                    }
                    
                    Text("Catégories actuelles: $categories", style = MaterialTheme.typography.bodySmall)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            label = { Text("Nouvelle catégorie") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newCategoryInput.isNotBlank()) {
                                categories = if (categories.isEmpty()) newCategoryInput else "$categories,$newCategoryInput"
                                newCategoryInput = ""
                            }
                        }) {
                            Text("Ajouter")
                        }
                    }
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Anti-Fraude globale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(onClick = { showCreateAntiFraudDialog = true }) {
                            Text("Nouvelle Anti-Fraude")
                        }
                    }
                    
                    if (allAntiFraudRules.isEmpty()) {
                        Text("Aucune règle anti-fraude.", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    allAntiFraudRules.forEach { af ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(af.name, fontWeight = FontWeight.Bold)
                                    Text("\${af.triggerEvent} - Limite: \${af.threshold}", style = MaterialTheme.typography.bodySmall)
                                    Text("Action: \${af.action} pour \${af.durationHours}h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = af.isActive, onCheckedChange = { viewModel.toggleAntiFraudRuleStatus(af) })
                                    IconButton(onClick = { viewModel.deleteAntiFraudRule(af.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    viewModel.updateConfig(
                        conf.copy(
                            baseTransportFee = baseTransportFee.toFloatOrNull() ?: conf.baseTransportFee,
                            pricePerKm = pricePerKm.toFloatOrNull() ?: conf.pricePerKm,
                            categories = categories,
                            businessName = businessName,
                            contactPhone = contactPhone,
                            contactEmail = contactEmail,
                            openingHours = openingHours,
                            closingHours = closingHours,
                            openingDays = openingDays,
                            termsAndConditionsLink = termsLink
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer les modifications")
            }
        }
    }
    
    if (showCreateAntiFraudDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAntiFraudDialog = false },
            title = { Text("Nouvelle règle Anti-Fraude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = afNameInput,
                        onValueChange = { afNameInput = it },
                        label = { Text("Nom de la règle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    var expandedTrig by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTrig, onExpandedChange = { expandedTrig = !expandedTrig }) {
                        OutlinedTextField(
                            value = afTriggerInput,
                            onValueChange = { afTriggerInput = it },
                            label = { Text("Événement") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTrig) }
                        )
                        ExposedDropdownMenu(expanded = expandedTrig, onDismissRequest = { expandedTrig = false }) {
                            listOf("COMMANDES_ANNULEES", "PAIEMENTS_ECHOUES", "FAUSSES_COORDONNEES").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { afTriggerInput = option; expandedTrig = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = afThresholdInput,
                        onValueChange = { afThresholdInput = it },
                        label = { Text("Seuil") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = afDurationInput,
                        onValueChange = { afDurationInput = it },
                        label = { Text("Durée (Heures)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = afActionInput,
                        onValueChange = { afActionInput = it },
                        label = { Text("Action (BAN/SUSPEND/WARN)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addAntiFraudRule(
                        name = afNameInput,
                        triggerEvent = afTriggerInput,
                        threshold = afThresholdInput.toIntOrNull() ?: 3,
                        durationHours = afDurationInput.toIntOrNull() ?: 24,
                        action = afActionInput
                    )
                    showCreateAntiFraudDialog = false
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAntiFraudDialog = false }) { Text("Annuler") }
            }
        )
    }
}`;

content = content.replace(settingsTabOld, settingsTabNew);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', content);
console.log("Updated AdminSettingsTab");
