const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `    "VOIR_PARAMETRES", "GERER_PARAMETRES", "GERER_REGLES", "GERER_ANTI_FRAUDE"
)`;

const replacement = `    "VOIR_PARAMETRES", "GERER_PARAMETRES", "GERER_REGLES", "GERER_ANTI_FRAUDE",
    "VOIR_PUBLICITES", "GERER_PUBLICITES"
)`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard permissions fixed");
