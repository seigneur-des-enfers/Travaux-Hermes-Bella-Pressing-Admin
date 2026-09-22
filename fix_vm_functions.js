const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

const targetFuncs = `    fun addAnnouncement(title: String, message: String, imageUrl: String) {
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
    }`;

// Remove them from current location
code = code.replace(targetFuncs, '');

// Find where AppViewModel class ends. It's right before class AppViewModelFactory
const classEndIndex = code.lastIndexOf('class AppViewModelFactory');
let firstPart = code.substring(0, classEndIndex).trim();

// Remove the trailing brace of AppViewModel
if (firstPart.endsWith('}')) {
    firstPart = firstPart.substring(0, firstPart.length - 1).trim();
}

const newFirstPart = firstPart + '\n\n' + targetFuncs + '\n}\n\n';

code = newFirstPart + code.substring(classEndIndex);

fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', code);
console.log("AppViewModel functions moved");
