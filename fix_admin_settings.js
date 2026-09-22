const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

// Insert State Variables
const stateTarget = `    var termsLink by remember { mutableStateOf(conf.termsAndConditionsLink) }`;
const stateReplacement = `    var termsLink by remember { mutableStateOf(conf.termsAndConditionsLink) }
    var isLoyaltyEnabled by remember { mutableStateOf(conf.isLoyaltyEnabled) }
    var loyaltyDurationDays by remember { mutableStateOf("") }
    var isLoyaltyPermanent by remember { mutableStateOf(conf.loyaltyValidUntil == null) }`;

code = code.replace(stateTarget, stateReplacement);

// Insert UI inside LazyColumn
const uiTarget = `        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catégories de Vêtements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)`;

const uiReplacement = `        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Programme de Fidélité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le programme de fidélité")
                    }
                    if (isLoyaltyEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isLoyaltyPermanent, onCheckedChange = { isLoyaltyPermanent = it })
                            Text("Permanent")
                        }
                        if (!isLoyaltyPermanent) {
                            OutlinedTextField(
                                value = loyaltyDurationDays,
                                onValueChange = { loyaltyDurationDays = it },
                                label = { Text("Durée d'activation (Jours)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catégories de Vêtements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)`;

code = code.replace(uiTarget, uiReplacement);

// Update config object
const saveTarget = `                    viewModel.updateConfig(
                        conf.copy(
                            baseTransportFee = baseTransportFee.toFloatOrNull() ?: conf.baseTransportFee,`;

const saveReplacement = `                    val updatedLoyaltyUntil = if (!isLoyaltyEnabled) null else if (isLoyaltyPermanent) null else (System.currentTimeMillis() + (loyaltyDurationDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000)
                    viewModel.updateConfig(
                        conf.copy(
                            isLoyaltyEnabled = isLoyaltyEnabled,
                            loyaltyValidUntil = updatedLoyaltyUntil,
                            baseTransportFee = baseTransportFee.toFloatOrNull() ?: conf.baseTransportFee,`;

code = code.replace(saveTarget, saveReplacement);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard Settings patched");
