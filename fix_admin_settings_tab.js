const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `fun AdminSettingsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()`;

const replacement = `fun AdminSettingsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()`;

code = code.replace(target, replacement);

const target2 = `        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val canManageFidelity = currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true
        if (canManageFidelity) {`;

const replacement2 = `        val canManageFidelity = currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true
        if (canManageFidelity) {`;

code = code.replace(target2, replacement2);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminSettingsTab fixed");
