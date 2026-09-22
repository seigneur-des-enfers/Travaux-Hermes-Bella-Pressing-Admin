const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/                            Button\(onClick = \{\n                                viewModel\.updateUser\(admin\.copy\(permissions = currentPerms\.joinToString\(","\)\)\)\n                            \}\) \{\n                                Text\("Sauvegarder"\)\n                            \}\n                            TextButton\(onClick = \{ viewModel\.deleteUser\(admin\.id\) \}\) \{\n                                Text\("Supprimer", color = MaterialTheme\.colorScheme\.error\)\n                            \}/,
`                            if (!showTrash) {
                                Button(onClick = {
                                    viewModel.updateUser(admin.copy(permissions = currentPerms.joinToString(",")))
                                }) {
                                    Text("Sauvegarder")
                                }
                                TextButton(onClick = { viewModel.archiveUser(admin.id) }) {
                                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Button(onClick = { viewModel.restoreUser(admin.id) }) {
                                    Text("Restaurer")
                                }
                                TextButton(onClick = { viewModel.hardDeleteUser(admin.id) }) {
                                    Text("Supprimer définitivement", color = MaterialTheme.colorScheme.error)
                                }
                            }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminStaffTab buttons fixed");
