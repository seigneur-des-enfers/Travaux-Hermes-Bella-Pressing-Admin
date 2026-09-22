const fs = require('fs');

let vm = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

vm = vm.replace(
/    fun deleteSpecialItem\(id: String\) \{\n        viewModelScope\.launch \{\n            repository\.deleteSpecialItem\(id\)\n        \}\n    \}/,
`    fun deleteSpecialItem(id: String) {
        viewModelScope.launch {
            repository.deleteSpecialItem(id)
        }
    }
    
    fun addGommageItem(name: String, price: Float) {
        viewModelScope.launch {
            val item = com.example.data.GommageItemEntity(name = name, price = price)
            repository.insertGommageItem(item)
            logAdminAction(currentUser.value?.id, null, "CREATE_GOMMAGE_ITEM", "Création de l'article de gommage \${item.name}")
        }
    }
    fun updateGommageItem(item: com.example.data.GommageItemEntity) {
        viewModelScope.launch {
            repository.updateGommageItem(item)
            logAdminAction(currentUser.value?.id, null, "UPDATE_GOMMAGE_ITEM", "Modification de l'article de gommage \${item.name}")
        }
    }
    fun deleteGommageItem(id: String) {
        viewModelScope.launch {
            repository.deleteGommageItem(id)
        }
    }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', vm);
console.log("AppViewModel fixed for Gommage items");
