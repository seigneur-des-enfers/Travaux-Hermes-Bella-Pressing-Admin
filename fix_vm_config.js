const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

code = code.replace('val config = _config.value', 'val currentConfig = config.value');
code = code.replace('config?.isLoyaltyEnabled', 'currentConfig?.isLoyaltyEnabled');
code = code.replace(/config\.loyaltyValidUntil/g, 'currentConfig.loyaltyValidUntil');

fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', code);
console.log("AppViewModel config reference fixed");
