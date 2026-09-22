const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/    if \(showSpecialDialog\) \{[\s\S]*?        \)\n    \}/,
`    if (showSpecialDialog) {
        AlertDialog(
            onDismissRequest = {
                showSpecialDialog = false
                editingSpecialItem = null
            },
            title = { Text(if (editingSpecialItem == null) "Ajouter un Article Spécial" else "Modifier \${editingSpecialItem?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = specialNameInput,
                        onValueChange = { specialNameInput = it },
                        label = { Text("Nom de l'article") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialPriceInput,
                        onValueChange = { specialPriceInput = it },
                        label = { Text("Prix unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = specialPriceInput.toFloatOrNull() ?: 0f
                    if (specialNameInput.isNotBlank()) {
                        if (editingSpecialItem == null) {
                            viewModel.addSpecialItem(com.example.data.SpecialItemEntity(name = specialNameInput, price = p))
                        } else {
                            viewModel.updateSpecialItem(editingSpecialItem!!.copy(name = specialNameInput, price = p))
                        }
                    }
                    showSpecialDialog = false
                    editingSpecialItem = null
                }) {
                    Text("Enregistrer")
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

    if (showGommageDialog) {
        AlertDialog(
            onDismissRequest = {
                showGommageDialog = false
                editingGommageItem = null
            },
            title = { Text(if (editingGommageItem == null) "Ajouter un Article de Gommage" else "Modifier \${editingGommageItem?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gommageNameInput,
                        onValueChange = { gommageNameInput = it },
                        label = { Text("Nom de l'article de gommage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gommagePriceInput,
                        onValueChange = { gommagePriceInput = it },
                        label = { Text("Prix unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = gommagePriceInput.toFloatOrNull() ?: 0f
                    if (gommageNameInput.isNotBlank()) {
                        if (editingGommageItem == null) {
                            viewModel.addGommageItem(com.example.data.GommageItemEntity(name = gommageNameInput, price = p))
                        } else {
                            viewModel.updateGommageItem(editingGommageItem!!.copy(name = gommageNameInput, price = p))
                        }
                    }
                    showGommageDialog = false
                    editingGommageItem = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showGommageDialog = false 
                    editingGommageItem = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminCatalogueTab dialog added");
