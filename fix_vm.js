const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

// The script added allAnnouncements twice
// Let's remove all declarations of allAnnouncements and add it once.
code = code.replace(/val allAnnouncements = repository\.allAnnouncements\.stateIn\(viewModelScope, SharingStarted\.Lazily, emptyList\(\)\)/g, '');
code = code.replace('val currentUser = _currentUser.asStateFlow()', 'val currentUser = _currentUser.asStateFlow()\n    val allAnnouncements = repository.allAnnouncements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())');

// Now we remove all occurrences of the announcements functions
const fun1 = `
    fun addAnnouncement(title: String, message: String, imageUrl: String) {
        viewModelScope.launch {
            val announcement = com.example.data.AnnouncementEntity(title = title, message = message, imageUrl = imageUrl)
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "CREATE_ANNOUNCEMENT", "Création de l'annonce $title")
        }
    }`;
const fun2 = `
    fun updateAnnouncement(announcement: com.example.data.AnnouncementEntity) {
        viewModelScope.launch {
            repository.insertAnnouncement(announcement)
            logAdminAction(currentUser.value?.id, "", "UPDATE_ANNOUNCEMENT", "Mise à jour de l'annonce \${announcement.title}")
        }
    }`;
const fun3 = `
    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            logAdminAction(currentUser.value?.id, "", "DELETE_ANNOUNCEMENT", "Suppression d'une annonce")
        }
    }`;

// Replace multiple times if they exist, or use regex
code = code.replace(/fun addAnnouncement[\s\S]*?\}[\s]*\}/g, '');
code = code.replace(/fun updateAnnouncement[\s\S]*?\}[\s]*\}/g, '');
code = code.replace(/fun deleteAnnouncement[\s\S]*?\}[\s]*\}/g, '');

code = code.trim();
if (code.endsWith('}')) {
    code = code.substring(0, code.length - 1).trim();
}

const insert = `
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
}
`;

fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', code + insert);
console.log("AppVM fixed");
