const fs = require('fs');

let db = fs.readFileSync('./app/src/main/java/com/example/data/AppDatabase.kt', 'utf8');
db = db.replace(
/entities = \[UserEntity::class, OrderEntity::class, ActivityLogEntity::class, ConsumableEntity::class, ConfigEntity::class, SpecialItemEntity::class, PromoEntity::class, RuleEntity::class, AntiFraudRuleEntity::class\]/,
`entities = [UserEntity::class, OrderEntity::class, ActivityLogEntity::class, ConsumableEntity::class, ConfigEntity::class, SpecialItemEntity::class, GommageItemEntity::class, PromoEntity::class, RuleEntity::class, AntiFraudRuleEntity::class]`
);
// Bump version
db = db.replace(/version = \d+/, 'version = 17');
fs.writeFileSync('./app/src/main/java/com/example/data/AppDatabase.kt', db);
console.log("AppDatabase updated");
