const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/Models.kt', 'utf8');

code = code.replace(
/    val isArchived: Boolean = false\n\n@Entity\(tableName = "consumables"\)/,
`    val isArchived: Boolean = false
)

@Entity(tableName = "consumables")`
);

fs.writeFileSync('./app/src/main/java/com/example/data/Models.kt', code);
console.log("Models fixed closing brace");
