const fs = require('fs');
let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/TrashItemCard\(title = "\$\{user\.prenom\} \$\{user\.nom\} \(\$\{user\.role\}\)", onRestore = \{ viewModel\.restoreUser\(user\.id\) \}, onHardDelete = \{ viewModel\.hardDeleteUser\(user\.id\) \}\)/,
`TrashItemCard(title = "\${user.prenom} \${user.nom} (\${user.role})", details = "Email: \${user.email}\\nTéléphone: \${user.telephone}", onRestore = { viewModel.restoreUser(user.id) }, onHardDelete = { viewModel.hardDeleteUser(user.id) })`
);

dash = dash.replace(
/TrashItemCard\(title = "Commande #\$\{order\.id\} \(\$\{order\.status\}\)", onRestore = \{ viewModel\.restoreOrder\(order\.id\) \}, onHardDelete = \{ viewModel\.hardDeleteOrder\(order\.id\) \}\)/,
`TrashItemCard(title = "Commande #\${order.id} (\${order.status})", details = "Client ID: \${order.userId}\\nArticles: \${order.pieces} pièces, \${order.kg} Kg\\nPrix Final: \${order.prixFinal.toInt()} FCFA", onRestore = { viewModel.restoreOrder(order.id) }, onHardDelete = { viewModel.hardDeleteOrder(order.id) })`
);

dash = dash.replace(
/TrashItemCard\(title = item\.name, onRestore = \{ viewModel\.restoreSpecialItem\(item\.id\) \}, onHardDelete = \{ viewModel\.hardDeleteSpecialItem\(item\.id\) \}\)/,
`TrashItemCard(title = item.name, details = "Prix: \${item.price} FCFA", onRestore = { viewModel.restoreSpecialItem(item.id) }, onHardDelete = { viewModel.hardDeleteSpecialItem(item.id) })`
);

dash = dash.replace(
/TrashItemCard\(title = item\.code, onRestore = \{ viewModel\.restorePromo\(item\.id\) \}, onHardDelete = \{ viewModel\.hardDeletePromo\(item\.id\) \}\)/,
`TrashItemCard(title = item.code, details = "Réduction: \${item.discountPercent}\${if(item.discountType == "PERCENTAGE") "%" else " FCFA"}\\nValable: \${if(item.isPermanent) "Toujours" else "Temporaire"}", onRestore = { viewModel.restorePromo(item.id) }, onHardDelete = { viewModel.hardDeletePromo(item.id) })`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("Trash details injected");
