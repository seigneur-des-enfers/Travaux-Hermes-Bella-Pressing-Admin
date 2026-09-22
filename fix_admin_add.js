const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/viewModel\.addSpecialItem\(com\.example\.data\.SpecialItemEntity\(name = specialNameInput, price = p\)\)/,
`viewModel.addSpecialItem(specialNameInput, p)`
);

dash = dash.replace(
/viewModel\.addGommageItem\(com\.example\.data\.GommageItemEntity\(name = gommageNameInput, price = p\)\)/,
`viewModel.addGommageItem(gommageNameInput, p)`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminDashboard fixed for add item calls");
