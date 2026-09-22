const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

if (!code.includes('import androidx.compose.ui.draw.clip')) {
    code = code.replace(/import androidx\.compose\.runtime\.\*/, 'import androidx.compose.runtime.*\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.foundation.background');
} else {
    code = code.replace(/import androidx\.compose\.ui\.draw\.clip/, 'import androidx.compose.ui.draw.clip\nimport androidx.compose.foundation.background');
}

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("Imports fixed");
