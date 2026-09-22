const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/Models.kt', 'utf8');

code = code.replace(
    'val isLoyaltyEnabled: Boolean = true,\n    val loyaltyValidUntil: Long? = null',
    'val isLoyaltyEnabled: Boolean = true,\n    val loyaltyValidUntil: Long? = null,'
);

fs.writeFileSync('./app/src/main/java/com/example/data/Models.kt', code);
console.log("Models fixed");
