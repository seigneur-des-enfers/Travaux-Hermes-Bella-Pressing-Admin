const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()
    val canManageOrders = hasPermission(currentUser, "GERER_COMMANDES")`;

const replacement = `    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()
    val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()
    val canManageOrders = hasPermission(currentUser, "GERER_COMMANDES")`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard allPromos fixed");
