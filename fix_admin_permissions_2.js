const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `    "VOIR_PUBLICITES", "GERER_PUBLICITES"
)`;

const replacement = `    "VOIR_PUBLICITES", "GERER_PUBLICITES", "GERER_FIDELITE"
)`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard permissions 2 fixed");
