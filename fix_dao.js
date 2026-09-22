const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/AppDao.kt', 'utf8');

// Remove all occurrences of the announcements queries
code = code.replace(/@Query\("SELECT \* FROM announcements ORDER BY createdAt DESC"\)\s*fun getAllAnnouncements\(\): kotlinx\.coroutines\.flow\.Flow<List<AnnouncementEntity>>/g, '');
code = code.replace(/@Insert\(onConflict = OnConflictStrategy\.REPLACE\)\s*suspend fun insertAnnouncement\(announcement: AnnouncementEntity\)/g, '');
code = code.replace(/@Query\("DELETE FROM announcements WHERE id = :id"\)\s*suspend fun deleteAnnouncement\(id: String\)/g, '');

// Clean up empty lines at the end
code = code.trim();
if (code.endsWith('}')) {
    code = code.substring(0, code.length - 1).trim();
}

const insert = `
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
    
    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}
`;

fs.writeFileSync('./app/src/main/java/com/example/data/AppDao.kt', code + insert);
console.log("AppDao fixed");
