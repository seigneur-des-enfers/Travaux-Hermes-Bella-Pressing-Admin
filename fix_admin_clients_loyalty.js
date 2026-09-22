const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(canUsePromo = !client.canUsePromo))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.canUsePromo) "Bloquer l'utilisation de promos" else "Autoriser les promos")
                    }`;

const replacement = `                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(canUsePromo = !client.canUsePromo))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.canUsePromo) "Bloquer l'utilisation de promos" else "Autoriser les promos")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(isLoyaltyEnabled = !client.isLoyaltyEnabled))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.isLoyaltyEnabled) "Désactiver le programme de fidélité" else "Activer le programme de fidélité")
                    }`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("Client loyalty toggle added");
