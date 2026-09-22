fun AdminRulesTab(padding: PaddingValues, viewModel: AppViewModel) {
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    
    var showCreateRuleDialog by remember { mutableStateOf(false) }
    var ruleNameInput by remember { mutableStateOf("") }
    var ruleTriggerInput by remember { mutableStateOf("ORDER_CREATED") }
    var ruleActionInput by remember { mutableStateOf("APPLY_DISCOUNT") }
    var ruleActionValueInput by remember { mutableStateOf("") }
    var ruleConstraintsInput by remember { mutableStateOf("") }
    
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
                Button(onClick = { showCreateRuleDialog = true }) {
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
                modifier = Modifier.fillMaxWidth(),
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
                        Text("Déclencheur: ${rule.triggerCondition}", style = MaterialTheme.typography.bodySmall)
                        Text("Action: ${rule.actionType} (${rule.actionValue})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (rule.constraints.isNotBlank()) {
                            Text("Contraintes: ${rule.constraints}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
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
            title = { Text("Créer une nouvelle règle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text("Nom de la règle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ruleTriggerInput,
                        onValueChange = { ruleTriggerInput = it },
                        label = { Text("Déclencheur (ex: ORDER_CREATED)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ruleActionInput,
                        onValueChange = { ruleActionInput = it },
                        label = { Text("Action (ex: APPLY_DISCOUNT)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ruleActionValueInput,
                        onValueChange = { ruleActionValueInput = it },
                        label = { Text("Valeur de l'action (ex: 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ruleConstraintsInput,
                        onValueChange = { ruleConstraintsInput = it },
                        label = { Text("Contraintes (Optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ruleNameInput.isNotBlank() && ruleTriggerInput.isNotBlank() && ruleActionInput.isNotBlank()) {
                        viewModel.addRule(
                            name = ruleNameInput,
                            triggerCondition = ruleTriggerInput,
                            actionType = ruleActionInput,
                            actionValue = ruleActionValueInput,
                            constraints = ruleConstraintsInput
                        )
                    }
                    showCreateRuleDialog = false
                    ruleNameInput = ""
                    ruleTriggerInput = "ORDER_CREATED"
                    ruleActionInput = "APPLY_DISCOUNT"
                    ruleActionValueInput = ""
                    ruleConstraintsInput = ""
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRuleDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
