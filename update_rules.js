const fs = require('fs');
let content = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

// We will update AdminRulesTab
let ruleTabOld = fs.readFileSync('admin_rules_tab.kt', 'utf8');

let ruleTabNew = `@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRulesTab(padding: PaddingValues, viewModel: AppViewModel) {
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    
    var showCreateRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }
    
    var ruleNameInput by remember { mutableStateOf("") }
    var ruleTriggerInput by remember { mutableStateOf("") }
    var ruleActionInput by remember { mutableStateOf("") }
    var ruleActionValueInput by remember { mutableStateOf("") }
    var ruleConstraintsInput by remember { mutableStateOf("") }
    var isPermanentInput by remember { mutableStateOf(true) }
    var validUntilInput by remember { mutableStateOf<Long?>(null) }
    
    val triggersList = listOf("COMMANDE_CREEE", "UTILISATEUR_INSCRIT", "MONTANT_MIN_ATTEINT", "NOUVELLE_CATEGORIE", "MODIFICATION_PROMO", "STOCK_FAIBLE", "AVIS_AJOUTE", "PAIEMENT_ECHOUE", "OR", "AND", "XOR")
    val actionsList = listOf("APPLY_DISCOUNT", "ADD_FREE_KG", "NOTIFY_ADMIN", "BLOCK_USER", "SEND_EMAIL", "AUTO_ACCEPT_ORDER", "OR", "AND", "XOR")
    val constraintsList = listOf("ONLY_NEW_USERS", "MIN_AMOUNT=5000", "HAS_PREMIUM", "NO_PROMO", "OR", "AND", "XOR", "EQUAL", "NOT_EQUAL")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Règles Automatiques", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { 
                    editingRule = null
                    ruleNameInput = ""
                    ruleTriggerInput = ""
                    ruleActionInput = ""
                    ruleActionValueInput = ""
                    ruleConstraintsInput = ""
                    isPermanentInput = true
                    validUntilInput = null
                    showCreateRuleDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouvelle Règle")
                }
            }
        }
        
        if (allRules.isEmpty()) {
            item {
                Text("Aucune règle configurée.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(allRules) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    editingRule = rule
                    ruleNameInput = rule.name
                    ruleTriggerInput = rule.triggerCondition
                    ruleActionInput = rule.actionType
                    ruleActionValueInput = rule.actionValue
                    ruleConstraintsInput = rule.constraints
                    isPermanentInput = rule.isPermanent
                    validUntilInput = rule.validUntil
                    showCreateRuleDialog = true
                },
                colors = CardDefaults.cardColors(containerColor = if (rule.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Déclencheur: \${rule.triggerCondition}", style = MaterialTheme.typography.bodySmall)
                        Text("Action: \${rule.actionType} (\${rule.actionValue})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (rule.constraints.isNotBlank()) {
                            Text("Contraintes: \${rule.constraints}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Text("Durée: \${if (rule.isPermanent) "Permanent" else "Temporaire"}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = rule.isActive, onCheckedChange = { viewModel.toggleRuleStatus(rule) })
                        IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateRuleDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRuleDialog = false },
            title = { Text(if (editingRule == null) "Créer une nouvelle règle" else "Modifier la règle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text("Nom de la règle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var expandedTrigger by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTrigger, onExpandedChange = { expandedTrigger = !expandedTrigger }) {
                        OutlinedTextField(
                            value = ruleTriggerInput,
                            onValueChange = { ruleTriggerInput = it },
                            label = { Text("Déclencheur (OR/AND/XOR supporté)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTrigger) }
                        )
                        ExposedDropdownMenu(expanded = expandedTrigger, onDismissRequest = { expandedTrigger = false }) {
                            triggersList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleTriggerInput = if (ruleTriggerInput.isEmpty()) option else "$ruleTriggerInput $option "
                                        expandedTrigger = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedAction by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedAction, onExpandedChange = { expandedAction = !expandedAction }) {
                        OutlinedTextField(
                            value = ruleActionInput,
                            onValueChange = { ruleActionInput = it },
                            label = { Text("Action") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAction) }
                        )
                        ExposedDropdownMenu(expanded = expandedAction, onDismissRequest = { expandedAction = false }) {
                            actionsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleActionInput = if (ruleActionInput.isEmpty()) option else "$ruleActionInput $option "
                                        expandedAction = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ruleActionValueInput,
                        onValueChange = { ruleActionValueInput = it },
                        label = { Text("Valeur de l'action (Valeur de l'action: Montant, % ou ID ciblé par l'action choisie)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var expandedConstraint by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedConstraint, onExpandedChange = { expandedConstraint = !expandedConstraint }) {
                        OutlinedTextField(
                            value = ruleConstraintsInput,
                            onValueChange = { ruleConstraintsInput = it },
                            label = { Text("Contraintes (Optionnel)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConstraint) }
                        )
                        ExposedDropdownMenu(expanded = expandedConstraint, onDismissRequest = { expandedConstraint = false }) {
                            constraintsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleConstraintsInput = if (ruleConstraintsInput.isEmpty()) option else "$ruleConstraintsInput $option "
                                        expandedConstraint = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPermanentInput, onCheckedChange = { isPermanentInput = it })
                        Text("Règle Permanente")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ruleNameInput.isNotBlank() && ruleTriggerInput.isNotBlank() && ruleActionInput.isNotBlank()) {
                        if (editingRule != null) {
                            viewModel.updateRuleInfo(editingRule!!.copy(
                                name = ruleNameInput,
                                triggerCondition = ruleTriggerInput,
                                actionType = ruleActionInput,
                                actionValue = ruleActionValueInput,
                                constraints = ruleConstraintsInput,
                                isPermanent = isPermanentInput,
                                validUntil = validUntilInput
                            ))
                        } else {
                            viewModel.addRule(
                                name = ruleNameInput,
                                triggerCondition = ruleTriggerInput,
                                actionType = ruleActionInput,
                                actionValue = ruleActionValueInput,
                                constraints = ruleConstraintsInput,
                                isPermanent = isPermanentInput,
                                validUntil = validUntilInput
                            )
                        }
                    }
                    showCreateRuleDialog = false
                }) {
                    Text(if (editingRule == null) "Créer" else "Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRuleDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}`;

content = content.replace(ruleTabOld, ruleTabNew);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', content);
console.log("Updated AdminRulesTab");
