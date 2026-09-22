const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const targetGlobal = `        item {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {`;

const replacementGlobal = `        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val canManageFidelity = currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true
        if (canManageFidelity) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {`;
                
code = code.replace(targetGlobal, replacementGlobal);

const targetGlobalEnd = `                }
            }
        }
        item {
            Button(`;

const replacementGlobalEnd = `                }
            }
        }
        } // end if canManageFidelity
        item {
            Button(`;
            
code = code.replace(targetGlobalEnd, replacementGlobalEnd);

const targetClient = `                Spacer(modifier = Modifier.height(16.dp))
                Text("Programme de Fidélité du Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activer")
                }`;
                
const replacementClient = `                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val canManageFidelity = currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true
                
                if (canManageFidelity) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Programme de Fidélité du Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer")
                    }
                }`;

code = code.replace(targetClient, replacementClient);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard fidelity permissions fixed");
