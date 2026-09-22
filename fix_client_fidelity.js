const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', 'utf8');

const target = `        if (user != null && config != null) {
            item {
                val currentKg = user.fidelityPointsKg`;

const replacement = `        if (user != null && config != null) {
            val globalLoyaltyEnabled = config.isLoyaltyEnabled && (config.loyaltyValidUntil == null || config.loyaltyValidUntil > System.currentTimeMillis())
            val userLoyaltyEnabled = user.isLoyaltyEnabled && (user.loyaltyValidUntil == null || user.loyaltyValidUntil > System.currentTimeMillis())
            
            if (globalLoyaltyEnabled && userLoyaltyEnabled) {
            item {
                val currentKg = user.fidelityPointsKg`;

code = code.replace(target, replacement);

const target2 = `                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }`;

const replacement2 = `                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }`;

code = code.replace(target2, replacement2);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', code);
console.log("ClientDashboard fidelity patched");
