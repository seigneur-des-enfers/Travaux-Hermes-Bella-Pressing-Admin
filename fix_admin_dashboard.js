const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');
if (!code.includes('import androidx.compose.ui.text.style.TextOverflow')) {
    code = code.replace('import androidx.compose.ui.text.font.FontWeight', 'import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.style.TextOverflow');
}

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard imports fixed");
