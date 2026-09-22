const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

code = code.replace(
/                    val insuranceFee = if \(hasPremiumGuarantee\) basePrix \* 0\.1f else 0f\n                    \n                    val newOrder = OrderEntity\(/,
`                    val insuranceFee = if (hasPremiumGuarantee) basePrix * 0.1f else 0f
                    
                    if (promoCode.isNotBlank() && validPromo == null) {
                        // Prevent submission if promo code is invalid
                        return@Button
                    }
                    
                    val newOrder = OrderEntity(`
);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("Promo submit updated");
