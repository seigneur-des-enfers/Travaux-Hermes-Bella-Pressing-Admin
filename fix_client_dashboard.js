const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', 'utf8');

const target1 = `fun ClientDashboard(viewModel: AppViewModel, onNavigateToCreateOrder: () -> Unit, onNavigateToAuth: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()`;

const replacement1 = `fun ClientDashboard(viewModel: AppViewModel, onNavigateToCreateOrder: () -> Unit, onNavigateToAuth: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allAnnouncements by viewModel.allAnnouncements.collectAsStateWithLifecycle()`;

code = code.replace(target1, replacement1);

const target2 = `        when (selectedTab) {
            0 -> ClientHomeTab(padding, orders, user, config) { viewModel.updateOrder(it) }`;
            
const replacement2 = `        when (selectedTab) {
            0 -> ClientHomeTab(padding, orders, user, config, allAnnouncements) { viewModel.updateOrder(it) }`;

code = code.replace(target2, replacement2);

const target3 = `fun ClientHomeTab(padding: PaddingValues, orders: List<OrderEntity>, user: com.example.data.UserEntity?, config: com.example.data.ConfigEntity?, onUpdateOrder: (OrderEntity) -> Unit) {`;

const replacement3 = `fun ClientHomeTab(padding: PaddingValues, orders: List<OrderEntity>, user: com.example.data.UserEntity?, config: com.example.data.ConfigEntity?, announcements: List<com.example.data.AnnouncementEntity>, onUpdateOrder: (OrderEntity) -> Unit) {`;

code = code.replace(target3, replacement3);

const target4 = `        item {
            Text("Résumé de vos commandes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }`;
        
const replacement4 = `        if (announcements.any { it.isVisible }) {
            item {
                Text("Annonces & Publicités", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(announcements.filter { it.isVisible }) { ann ->
                        Card(
                            modifier = Modifier.width(300.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(ann.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
                                Text(ann.message, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                                // We could show image if imageUrl is valid
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Text("Résumé de vos commandes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }`;

code = code.replace(target4, replacement4);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', code);
console.log("ClientDashboard announcements added");
