const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/fun TrashItemCard\(title: String, details: String = "", onRestore: \(\) -> Unit, onHardDelete: \(\) -> Unit\) \{[\s\S]*?    if \(showRestoreConfirm\) \{/m,
`fun TrashItemCard(title: String, details: String = "", onRestore: () -> Unit, onHardDelete: () -> Unit) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRestoreConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (expanded && details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(details, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(onClick = { showRestoreConfirm = true }) { Icon(Icons.Default.Refresh, contentDescription = "Restaurer", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer définitivement", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showRestoreConfirm) {`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("TrashItemCard properly updated!");
