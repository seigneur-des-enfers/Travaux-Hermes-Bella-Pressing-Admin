const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/data/Models.kt', 'utf8');

code = code.replace(
/data class GommageItemEntity\([\s\S]*?\)\n\)\n/,
`data class GommageItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Float,
    val isArchived: Boolean = false
)
`
);
fs.writeFileSync('./app/src/main/java/com/example/data/Models.kt', code);
console.log("Models.kt fixed");
