                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = currentPerms.contains(perm),
                                                onCheckedChange = { isChecked ->
                                                    val newPerms = currentPerms.toMutableSet()
                                                    if (isChecked) newPerms.add(perm) else newPerms.remove(perm)
                                                    currentPerms = newPerms
                                                }
                                            )
                                            Text(perm.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                viewModel.updateUser(admin.copy(permissions = currentPerms.joinToString(",")))
                            }) {
                                Text("Sauvegarder")
                            }
                            TextButton(onClick = { viewModel.deleteUser(admin.id) }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAdminDialog) {
        var nom by remember { mutableStateOf("") }
        var prenom by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("ADMIN") }
        
        var currentPerms by remember { mutableStateOf(setOf("VOIR_COMMANDES", "VOIR_CLIENTS")) }

        AlertDialog(
            onDismissRequest = { showCreateAdminDialog = false },
            title = { Text("Nouveau membre du Staff") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }) }
                    item { OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }) }
                    item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }) }
                    item { OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe temporaire") }) }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Rôle: ", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ADMIN")
                            Switch(
                                checked = role == "SUPER_ADMIN",
                                onCheckedChange = { isSuper -> role = if (isSuper) "SUPER_ADMIN" else "ADMIN" }
                            )
                            Text("SUPER_ADMIN")
                        }
                    }

                    if (role == "ADMIN") {
                        item { Text("Permissions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                        items(AVAILABLE_PERMISSIONS.chunked(2)) { rowPerms ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowPerms.forEach { perm ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = currentPerms.contains(perm),
                                                onCheckedChange = { isChecked ->
                                                    val newPerms = currentPerms.toMutableSet()
                                                    if (isChecked) newPerms.add(perm) else newPerms.remove(perm)
                                                    currentPerms = newPerms
                                                }
                                            )
                                            Text(perm.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newUser = UserEntity(
                        role = role,
                        nom = nom,
                        prenom = prenom,
                        email = email,
                        telephone = "Non renseigné",
                        motDePasse = password,
                        permissions = if (role == "ADMIN") currentPerms.joinToString(",") else "ALL"
                    )
                    viewModel.insertUser(newUser)
                    showCreateAdminDialog = false
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDialog = false }) { Text("Annuler") }
            }
        )
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
