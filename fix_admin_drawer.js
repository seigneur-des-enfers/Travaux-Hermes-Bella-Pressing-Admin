const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "CORBEILLE_GLOBALE",
                        onClick = { selectedTab = "CORBEILLE_GLOBALE"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Corbeille") },
                        label = { Text("Corbeille") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }`;

const replacement = `                if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "CORBEILLE_GLOBALE",
                        onClick = { selectedTab = "CORBEILLE_GLOBALE"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Corbeille") },
                        label = { Text("Corbeille") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_PUBLICITES") || currentUser?.permissions?.contains("ALL") == true) {
                    NavigationDrawerItem(
                        selected = selectedTab == "PUBLICITES",
                        onClick = { selectedTab = "PUBLICITES"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Publicités") },
                        label = { Text("Publicités") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard drawer fixed");
