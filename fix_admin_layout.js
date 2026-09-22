const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/import androidx\.compose\.runtime\.\*/,
`import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Menu`
);

dash = dash.replace(
/fun AdminDashboard\([\s\S]*?    val currentUser by viewModel\.currentUser\.collectAsStateWithLifecycle\(\)\n    var selectedTab by remember \{ mutableStateOf\("COMMANDES"\) \}\n\n    Scaffold\(\n        topBar = \{[\s\S]*?            \)[\s\S]*?        \},\n        bottomBar = \{[\s\S]*?        \}\n    \) \{ padding ->/m,
`fun AdminDashboard(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("COMMANDES") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Menu Administration", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                
                if (hasPermission(currentUser, "VOIR_COMMANDES")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "COMMANDES",
                        onClick = { selectedTab = "COMMANDES"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Commandes") },
                        label = { Text("Commandes") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_CLIENTS")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "CLIENTS",
                        onClick = { selectedTab = "CLIENTS"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Clients") },
                        label = { Text("Clients") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_STOCKS")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "STOCKS",
                        onClick = { selectedTab = "STOCKS"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Stocks") },
                        label = { Text("Stocks") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_CATALOGUE")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "CATALOGUE",
                        onClick = { selectedTab = "CATALOGUE"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Catalogue") },
                        label = { Text("Catalogue") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_PARAMETRES")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "PARAMETRES",
                        onClick = { selectedTab = "PARAMETRES"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Config") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_STAFF")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "STAFF",
                        onClick = { selectedTab = "STAFF"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Staff") },
                        label = { Text("Staff") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "GERER_REGLES")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "REGLES",
                        onClick = { selectedTab = "REGLES"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Règles") },
                        label = { Text("Règles") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "CORBEILLE_GLOBALE",
                        onClick = { selectedTab = "CORBEILLE_GLOBALE"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Corbeille") },
                        label = { Text("Corbeille") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Espace Administration", style = MaterialTheme.typography.titleMedium)
                            Text("Bella Pressing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        ) { padding ->`
);

dash = dash.replace(
/        when \(selectedTab\) \{[\s\S]*?        \}\n    \}\n\}\n\nfun exportInvoiceToPDF/m,
`        when (selectedTab) {
            "COMMANDES" -> if (hasPermission(currentUser, "VOIR_COMMANDES")) AdminOrdersTab(padding, allOrders, viewModel)
            "CLIENTS" -> if (hasPermission(currentUser, "VOIR_CLIENTS")) AdminClientsTab(padding, allClients, allOrders, viewModel)
            "STOCKS" -> if (hasPermission(currentUser, "VOIR_STOCKS")) AdminStocksTab(padding, viewModel)
            "CATALOGUE" -> if (hasPermission(currentUser, "VOIR_CATALOGUE")) AdminCatalogueTab(padding, viewModel)
            "PARAMETRES" -> if (hasPermission(currentUser, "VOIR_PARAMETRES")) AdminSettingsTab(padding, viewModel)
            "STAFF" -> if (hasPermission(currentUser, "VOIR_STAFF")) AdminStaffTab(padding, viewModel)
            "REGLES" -> if (hasPermission(currentUser, "GERER_REGLES")) AdminRulesTab(padding, viewModel)
            "CORBEILLE_GLOBALE" -> if (hasPermission(currentUser, "VOIR_CORBEILLE")) AdminGlobalTrashTab(padding, viewModel)
        }
    }
}

fun exportInvoiceToPDF`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminDashboard drawer applied");
