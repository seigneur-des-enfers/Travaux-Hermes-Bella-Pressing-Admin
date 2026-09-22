const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/Models.kt', 'utf8');

// Update ConfigEntity
code = code.replace(
    'val autoBanDurationHours: Int = 24,',
    'val autoBanDurationHours: Int = 24,\n    val isLoyaltyEnabled: Boolean = true,\n    val loyaltyValidUntil: Long? = null,'
);

// Update UserEntity
code = code.replace(
    'val permissions: String = "ALL", // For ADMIN: "COMMANDES,CLIENTS,STOCKS,CATALOGUE,PARAMETRES,CATEGORIES,REGLES,PROMOS_AVANCEES"',
    'val permissions: String = "ALL", // For ADMIN: "COMMANDES,CLIENTS,STOCKS,CATALOGUE,PARAMETRES,CATEGORIES,REGLES,PROMOS_AVANCEES",\n    val isLoyaltyEnabled: Boolean = true,\n    val loyaltyValidUntil: Long? = null'
);

// Add AnnouncementEntity
const announcementStr = `
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val imageUrl: String = "",
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
`;
code += announcementStr;

fs.writeFileSync('./app/src/main/java/com/example/data/Models.kt', code);
console.log("Models updated");
