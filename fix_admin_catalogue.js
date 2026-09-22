const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target1 = `        item {
            Button(onClick = { showSpecialDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un Article Spécial")
            }
        }`;
const replacement1 = `        item {
            Button(onClick = { 
                editingSpecialItem = null
                specialNameInput = ""
                specialPriceInput = ""
                showSpecialDialog = true 
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un Article Spécial")
            }
        }`;

code = code.replace(target1, replacement1);

const target2 = `            item {
                Button(onClick = { showGommageDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un Article de Gommage")
                }
            }`;
const replacement2 = `            item {
                Button(onClick = { 
                    editingGommageItem = null
                    gommageNameInput = ""
                    gommagePriceInput = ""
                    showGommageDialog = true 
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un Article de Gommage")
                }
            }`;

code = code.replace(target2, replacement2);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard catalogue creation fixed");
