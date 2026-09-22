const fs = require('fs');

// DAO
let daoCode = fs.readFileSync('./app/src/main/java/com/example/data/AppDao.kt', 'utf8');
if (!daoCode.includes('getAllAnnouncements')) {
    const daoInsert = `
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
    
    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
`;
    const lastBraceIndex = daoCode.lastIndexOf('}');
    daoCode = daoCode.substring(0, lastBraceIndex) + daoInsert + '\n}';
    fs.writeFileSync('./app/src/main/java/com/example/data/AppDao.kt', daoCode);
}

// REPO
let repoCode = fs.readFileSync('./app/src/main/java/com/example/data/AppRepository.kt', 'utf8');
if (!repoCode.includes('allAnnouncements')) {
    const repoInsert = `
    val allAnnouncements: kotlinx.coroutines.flow.Flow<List<AnnouncementEntity>> = dao.getAllAnnouncements()
    
    suspend fun insertAnnouncement(announcement: AnnouncementEntity) = dao.insertAnnouncement(announcement)
    suspend fun deleteAnnouncement(id: String) = dao.deleteAnnouncement(id)
`;
    const lastBraceIndex = repoCode.lastIndexOf('}');
    repoCode = repoCode.substring(0, lastBraceIndex) + repoInsert + '\n}';
    fs.writeFileSync('./app/src/main/java/com/example/data/AppRepository.kt', repoCode);
}

// VM
let vmCode = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');
if (!vmCode.includes('allAnnouncements')) {
    const vmInsert1 = `    val allAnnouncements = repository.allAnnouncements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())`;
    vmCode = vmCode.replace('val currentUser = _currentUser.asStateFlow()', 'val currentUser = _currentUser.asStateFlow()\n' + vmInsert1);

    const vmInsert2 = `
    fun addAnnouncement(title: String, message: String, imageUrl: String) {
        viewModelScope.launch {
            val announcement = com.example.data.AnnouncementEntity(title = title, message = message, imageUrl = imageUrl)
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "CREATE_ANNOUNCEMENT", "Création de l'annonce $title")
        }
    }
    
    fun updateAnnouncement(announcement: com.example.data.AnnouncementEntity) {
        viewModelScope.launch {
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "UPDATE_ANNOUNCEMENT", "Mise à jour de l'annonce \${announcement.title}")
        }
    }
    
    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            logAdminAction(currentUser.value?.id, "", "DELETE_ANNOUNCEMENT", "Suppression d'une annonce")
        }
    }
`;
    const lastBraceIndex = vmCode.lastIndexOf('}');
    vmCode = vmCode.substring(0, lastBraceIndex) + vmInsert2 + '\n}';
    fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', vmCode);
}
console.log("DAO, Repo, VM updated");
