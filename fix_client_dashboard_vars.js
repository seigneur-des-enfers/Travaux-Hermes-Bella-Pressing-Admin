const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', 'utf8');

const target = `    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()`;

const replacement = `    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val allAnnouncements by viewModel.allAnnouncements.collectAsStateWithLifecycle()`;

code = code.replace(target, replacement);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', code);
console.log("ClientDashboard announcements variable fixed");
