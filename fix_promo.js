const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

code = code.replace(
/            OutlinedTextField\(\n                value = promoCode,\n                onValueChange = \{ promoCode = it\.uppercase\(\) \},\n                label = \{ Text\("Code Promo \/ Parrainage"\) \},\n                modifier = Modifier\.fillMaxWidth\(\),\n                shape = RoundedCornerShape\(12\.dp\)\n            \)/,
`            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it.uppercase() },
                label = { Text("Code Promo / Parrainage") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = promoCode.isNotBlank() && validPromo == null,
                supportingText = {
                    if (promoCode.isNotBlank()) {
                        if (validPromo != null) {
                            Text("Code valide : -\${if (validPromo.discountType == "PERCENTAGE") "\${validPromo.discountPercent}%" else "\${validPromo.discountPercent} FCFA"}", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Code promo invalide ou expiré", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )`
);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("Promo code UI updated");
