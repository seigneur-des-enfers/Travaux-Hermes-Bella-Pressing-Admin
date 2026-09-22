const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/AppDatabase.kt', 'utf8');

code = code.replace(
    'entities = [UserEntity::class, OrderEntity::class, ActivityLogEntity::class, ConsumableEntity::class, ConfigEntity::class, SpecialItemEntity::class, GommageItemEntity::class, PromoEntity::class, RuleEntity::class, AntiFraudRuleEntity::class]',
    'entities = [UserEntity::class, OrderEntity::class, ActivityLogEntity::class, ConsumableEntity::class, ConfigEntity::class, SpecialItemEntity::class, GommageItemEntity::class, PromoEntity::class, RuleEntity::class, AntiFraudRuleEntity::class, AnnouncementEntity::class]'
);

code = code.replace('version = 17', 'version = 18');

fs.writeFileSync('./app/src/main/java/com/example/data/AppDatabase.kt', code);
console.log("AppDatabase updated");
