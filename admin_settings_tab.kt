fun AdminSettingsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageSettings = hasPermission(currentUser, "GERER_PARAMETRES")

    var pricePerKmInput by remember(config) { mutableStateOf(config?.pricePerKm?.toString() ?: "500") }
    var baseTransportInput by remember(config) { mutableStateOf(config?.baseTransportFee?.toString() ?: "1000") }
    var referralRewardKgInput by remember(config) { mutableStateOf(config?.referralRewardKg?.toString() ?: "2") }
    var fidelityThresholdKgInput by remember(config) { mutableStateOf(config?.fidelityThresholdKg?.toString() ?: "10") }
    var appDownloadLinkInput by remember(config) { mutableStateOf(config?.appDownloadLink ?: "") }

    var autoBanEnabled by remember(config) { mutableStateOf(config?.autoBanEnabled ?: false) }
    var autoBanThresholdInput by remember(config) { mutableStateOf(config?.autoBanThreshold?.toString() ?: "3") }
    var autoBanDurationInput by remember(config) { mutableStateOf(config?.autoBanDurationHours?.toString() ?: "24") }

    // --- PROGRAMME DE FIDÉLITÉ DYNAMIQUE (Admin décide, Client exécute) ---
    var isLoyaltyProgramEnabled by remember(config) { mutableStateOf(config?.isLoyaltyProgramEnabled ?: true) }
    var loyaltyKgThresholdInput by remember(config) { mutableStateOf(config?.loyaltyKgThreshold?.toString() ?: "10") }
    var loyaltyKgRewardInput by remember(config) { mutableStateOf(config?.loyaltyKgReward?.toString() ?: "1") }
    var loyaltyMaxFreeKgsInput by remember(config) { mutableStateOf(config?.loyaltyMaxFreeKgs?.toString() ?: "30") }
    var loyaltyExpirationDaysInput by remember(config) { mutableStateOf(config?.loyaltyExpirationDays?.toString() ?: "90") }

    // Nouveaux paramètres système
    var businessNameInput by remember(config) { mutableStateOf(config?.businessName ?: "Bella Pressing") }
    var contactPhoneInput by remember(config) { mutableStateOf(config?.contactPhone ?: "") }
    var contactEmailInput by remember(config) { mutableStateOf(config?.contactEmail ?: "") }
    var openingHoursInput by remember(config) { mutableStateOf(config?.openingHours ?: "") }
    var closingHoursInput by remember(config) { mutableStateOf(config?.closingHours ?: "") }
    var openingDaysInput by remember(config) { mutableStateOf(config?.openingDays ?: "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi") }
    var closingDaysInput by remember(config) { mutableStateOf(config?.closingDays ?: "") }
    var currencySymbolInput by remember(config) { mutableStateOf(config?.currencySymbol ?: "FCFA") }
    var taxPercentageInput by remember(config) { mutableStateOf(config?.taxPercentage?.toString() ?: "0") }
    var categoriesInput by remember(config) { mutableStateOf(config?.categories ?: "Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons") }
    var termsLinkInput by remember(config) { mutableStateOf(config?.termsAndConditionsLink ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Configuration du Pressing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tarification Livraison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = baseTransportInput,
                        onValueChange = { baseTransportInput = it },
                        label = { Text("Frais de base (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pricePerKmInput,
                        onValueChange = { pricePerKmInput = it },
                        label = { Text("Prix par Kilomètre (FCFA/Km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Marketing & Fidélité (Hérité)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = referralRewardKgInput,
                        onValueChange = { referralRewardKgInput = it },
                        label = { Text("Récompense Parrainage (Kg offerts)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fidelityThresholdKgInput,
                        onValueChange = { fidelityThresholdKgInput = it },
                        label = { Text("Seuil Fidélité Hérité (Kg cumulés)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appDownloadLinkInput,
                        onValueChange = { appDownloadLinkInput = it },
                        label = { Text("Lien de téléchargement de l'Application") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🏆 Programme de Fidélité Dynamique (Kilos Gratuits)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isLoyaltyProgramEnabled, onCheckedChange = { isLoyaltyProgramEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le programme de fidélité (Kilos gratuits)")
                    }

                    if (isLoyaltyProgramEnabled) {
                        OutlinedTextField(
                            value = loyaltyKgThresholdInput,
                            onValueChange = { loyaltyKgThresholdInput = it },
                            label = { Text("Seuil déclencheur (Kg lavés pour gagner 1 Kg gratuit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = loyaltyKgRewardInput,
                            onValueChange = { loyaltyKgRewardInput = it },
                            label = { Text("Récompense par palier (Kg gratuits offerts)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = loyaltyMaxFreeKgsInput,
                            onValueChange = { loyaltyMaxFreeKgsInput = it },
                            label = { Text("Plafond max Kg gratuits cumulables") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = loyaltyExpirationDaysInput,
                            onValueChange = { loyaltyExpirationDaysInput = it },
                            label = { Text("Expiration des Kg gratuits (jours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            "💡 Ces valeurs sont synchronisées via Firestore vers l'application Client. Le Client ne peut pas les modifier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Anti-Fraude (Bannissement Auto)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoBanEnabled, onCheckedChange = { autoBanEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le bannissement automatique")
                    }

                    if (autoBanEnabled) {
                        OutlinedTextField(
                            value = autoBanThresholdInput,
                            onValueChange = { autoBanThresholdInput = it },
                            label = { Text("Nombre de commandes rejetées consécutives") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = autoBanDurationInput,
                            onValueChange = { autoBanDurationInput = it },
                            label = { Text("Durée de suspension (en heures)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Système & Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it },
                        label = { Text("Nom de l'entreprise") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhoneInput,
                        onValueChange = { contactPhoneInput = it },
                        label = { Text("Téléphone de contact") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = contactEmailInput,
                        onValueChange = { contactEmailInput = it },
                        label = { Text("Email de contact") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = openingHoursInput,
                        onValueChange = { openingHoursInput = it },
                        label = { Text("Heures d'ouverture (ex: 8h - 18h)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = closingHoursInput,
                        onValueChange = { closingHoursInput = it },
                        label = { Text("Heures de fermeture (ex: 18h)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openingDaysInput,
                        onValueChange = { openingDaysInput = it },
                        label = { Text("Jours d'ouverture (ex: Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = closingDaysInput,
                        onValueChange = { closingDaysInput = it },
                        label = { Text("Jours de fermeture (ex: Dimanche)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currencySymbolInput,
                        onValueChange = { currencySymbolInput = it },
                        label = { Text("Symbole monétaire (ex: FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = taxPercentageInput,
                        onValueChange = { taxPercentageInput = it },
                        label = { Text("Taxes (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = categoriesInput,
                        onValueChange = { categoriesInput = it },
                        label = { Text("Catégories (séparées par une virgule)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = termsLinkInput,
                        onValueChange = { termsLinkInput = it },
                        label = { Text("Lien des CGU") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (canManageSettings) {
            item {
                Button(
                    onClick = {
                        config?.let {
                            val newConfig = it.copy(
                                pricePerKm = pricePerKmInput.toFloatOrNull() ?: it.pricePerKm,
                                baseTransportFee = baseTransportInput.toFloatOrNull() ?: it.baseTransportFee,
                                referralRewardKg = referralRewardKgInput.toFloatOrNull() ?: it.referralRewardKg,
                                fidelityThresholdKg = fidelityThresholdKgInput.toFloatOrNull() ?: it.fidelityThresholdKg,
                                appDownloadLink = appDownloadLinkInput,
                                autoBanEnabled = autoBanEnabled,
                                autoBanThreshold = autoBanThresholdInput.toIntOrNull() ?: 3,
                                autoBanDurationHours = autoBanDurationInput.toIntOrNull() ?: 24,
                                businessName = businessNameInput,
                                contactPhone = contactPhoneInput,
                                contactEmail = contactEmailInput,
                                openingHours = openingHoursInput,
                                closingHours = closingHoursInput,
                                openingDays = openingDaysInput,
                                closingDays = closingDaysInput,
                                // Nouveaux schémas (duplication intentionnelle pour compatibilité)
                                openHours = openingHoursInput,
                                closeHours = closingHoursInput,
                                openDays = openingDaysInput,
                                currencySymbol = currencySymbolInput,
                                taxPercentage = taxPercentageInput.toFloatOrNull() ?: 0f,
                                categories = categoriesInput,
                                termsAndConditionsLink = termsLinkInput,
                                // Programme de fidélité dynamique
                                isLoyaltyProgramEnabled = isLoyaltyProgramEnabled,
                                loyaltyKgThreshold = loyaltyKgThresholdInput.toDoubleOrNull() ?: 10.0,
                                loyaltyKgReward = loyaltyKgRewardInput.toDoubleOrNull() ?: 1.0,
                                loyaltyMaxFreeKgs = loyaltyMaxFreeKgsInput.toDoubleOrNull() ?: 30.0,
                                loyaltyExpirationDays = loyaltyExpirationDaysInput.toIntOrNull() ?: 90
                            )
                            viewModel.updateConfig(newConfig)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer la configuration")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)