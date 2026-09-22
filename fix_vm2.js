const fs = require('fs');

let vm = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

vm = vm.replace(
/logAdminAction\(currentUser\.value\?\.id, null, "CREATE_GOMMAGE_ITEM", "Création de l'article de gommage \\\$\{item\.name\}"\)/,
`logAdminAction(currentUser.value?.id, "", "CREATE_GOMMAGE_ITEM", "Création de l'article de gommage \${item.name}")`
);

vm = vm.replace(
/logAdminAction\(currentUser\.value\?\.id, null, "UPDATE_GOMMAGE_ITEM", "Modification de l'article de gommage \\\$\{item\.name\}"\)/,
`logAdminAction(currentUser.value?.id, "", "UPDATE_GOMMAGE_ITEM", "Modification de l'article de gommage \${item.name}")`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', vm);
console.log("AppViewModel fixed logAdminAction");
