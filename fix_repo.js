const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/data/AppRepository.kt', 'utf8');

code = code.replace(/val allAnnouncements: kotlinx\.coroutines\.flow\.Flow<List<AnnouncementEntity>> = dao\.getAllAnnouncements\(\)/g, '');
code = code.replace(/suspend fun insertAnnouncement\(announcement: AnnouncementEntity\) = dao\.insertAnnouncement\(announcement\)/g, '');
code = code.replace(/suspend fun deleteAnnouncement\(id: String\) = dao\.deleteAnnouncement\(id\)/g, '');

code = code.trim();
if (code.endsWith('}')) {
    code = code.substring(0, code.length - 1).trim();
}

const insert = `
    val allAnnouncements: kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>> = dao.getAllAnnouncements()
    
    suspend fun insertAnnouncement(announcement: AnnouncementEntity) = dao.insertAnnouncement(announcement)
    suspend fun deleteAnnouncement(id: String) = dao.deleteAnnouncement(id)
}
`;

fs.writeFileSync('./app/src/main/java/com/example/data/AppRepository.kt', code + insert);
console.log("AppRepo fixed");
