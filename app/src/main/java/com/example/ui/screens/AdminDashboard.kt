package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*

import com.example.data.AntiFraudRuleEntity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

import com.example.data.UserEntity
import com.example.data.PromoEntity
import com.example.data.RuleEntity
import com.example.data.SpecialItemEntity
import com.example.data.ActivityLogEntity
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OrderEntity
import com.example.ui.AppViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R

import com.example.di.Graph
import com.example.data.PendingActionEntity
import androidx.compose.material.icons.filled.Schedule
import com.example.ui.components.QuotaExceededBanner
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.BorderStroke

fun hasPermission(user: UserEntity?, perm: String): Boolean {
    if (user == null) return false
    if (user.role == "SUPER_ADMIN" || user.roleAdminSuper) return true
    
    // For regular ADMIN users, check if permission is explicitly granted
    val permsList = user.permissions.split(",").map { it.trim() }.toSet()
    if (permsList.contains("ALL") || permsList.contains("*")) return true
    
    // Management actions require explicit approval from Super Admin or granted flag
    if (perm.startsWith("GERER_") || perm.startsWith("SUSPENDRE_") || perm.startsWith("RESTAURER_") || perm.startsWith("SUPPRIMER_")) {
        return permsList.contains(perm) || when (perm) {
            "GERER_COMMANDES" -> user.permGererCommandes
            "GERER_CLIENTS" -> user.permGererClients
            "SUSPENDRE_CLIENTS" -> user.permSuspendreClients
            "GERER_STOCKS" -> user.permGererStocks
            "GERER_CATALOGUE" -> user.permGererCatalogue
            "GERER_PROMOS" -> user.permGererPromos
            "GERER_PARAMETRES" -> user.permGererParametres
            "GERER_REGLES" -> user.permGererParametres
            "GERER_ANTI_FRAUDE" -> user.permGererAntiFraudPersonnel || user.permGererAntiFraudGlobal
            "GERER_GOMMAGE" -> user.permGererGommage
            "RESTAURER_COMPTE" -> user.permRestaurerCompte
            "SUPPRIMER_DEFINITIVEMENT" -> user.permSupprimerDefinitivement
            else -> false
        }
    }

    if (permsList.contains(perm)) return true

    // Read-only / view permissions for regular admins
    return when (perm) {
        "VOIR_COMMANDES" -> user.permVoirCommandes || permsList.contains("COMMANDES") || permsList.contains("VOIR_COMMANDES") || permsList.contains("ALL")
        "VOIR_CLIENTS" -> user.permVoirClients || permsList.contains("CLIENTS") || permsList.contains("VOIR_CLIENTS") || permsList.contains("ALL")
        "VOIR_STOCKS" -> user.permVoirStocks || permsList.contains("STOCKS") || permsList.contains("VOIR_STOCKS") || permsList.contains("ALL")
        "VOIR_CATALOGUE" -> user.permVoirCatalogue || permsList.contains("CATALOGUE") || permsList.contains("VOIR_CATALOGUE") || permsList.contains("ALL")
        "VOIR_PARAMETRES" -> user.permVoirParametres || permsList.contains("PARAMETRES") || permsList.contains("VOIR_PARAMETRES") || permsList.contains("ALL")
        "VOIR_LOGS" -> user.permVoirLog || permsList.contains("CLIENTS") || permsList.contains("VOIR_LOGS") || permsList.contains("ALL")
        "VOIR_CORBEILLE" -> user.permVoirCorbeille || permsList.contains("VOIR_CORBEILLE")
        "VOIR_PUBLICITES" -> permsList.contains("VOIR_PUBLICITES") || permsList.contains("ALL")
        else -> false
    }
}

val AVAILABLE_PERMISSIONS = listOf(
    "VOIR_COMMANDES", "GERER_COMMANDES", "MODIFIER_HEURES_RAMASSAGE",
    "VOIR_CLIENTS", "GERER_CLIENTS", "SUSPENDRE_CLIENTS", "VOIR_LOGS", "BLOQUER_PROMOS_CLIENT",
    "VOIR_CORBEILLE", "RESTAURER_COMPTE", "SUPPRIMER_DEFINITIVEMENT",
    "VOIR_STOCKS", "GERER_STOCKS", "GERER_STOCKS_VISIBILITE",
    "VOIR_CATALOGUE", "GERER_CATALOGUE", "GERER_CATEGORIES", "GERER_PROMOS", "GERER_PROMOS_AVANCEES", "GERER_GOMMAGE",
    "VOIR_PARAMETRES", "GERER_PARAMETRES", "GERER_REGLES", "GERER_ANTI_FRAUDE",
    "VOIR_PUBLICITES", "GERER_PUBLICITES", "GERER_FIDELITE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isFirestoreQuotaExhausted by viewModel.isFirestoreQuotaExhausted.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("COMMANDES") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bella_admin_icon),
                        contentDescription = "Logo Bella Pressing Admin",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Bella Pressing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Espace Administration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
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
                if (hasPermission(currentUser, "VOIR_LOGS")) {
                    NavigationDrawerItem(
                        selected = selectedTab == "LOGS",
                        onClick = { selectedTab = "LOGS"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Journal d'activité") },
                        label = { Text("Journal d'activité") },
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
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bella_admin_icon),
                                contentDescription = "Logo Bella Pressing",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Espace Administration", style = MaterialTheme.typography.titleMedium)
                                Text("Bella Pressing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
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
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isFirestoreQuotaExhausted) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "⚠️ ALERTE CRITIQUE : QUOTA FIRESTORE ÉPUISÉ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "L'application a dépassé sa limite quotidienne gratuite de lectures/écritures sur Firebase. Les données ne sont plus synchronisées. Veuillez patienter jusqu'à demain ou augmenter votre plan Firebase (Blaze).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    QuotaExceededBanner()
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val zeroPadding = PaddingValues(0.dp)
                    when (selectedTab) {
                        "COMMANDES" -> if (hasPermission(currentUser, "VOIR_COMMANDES")) AdminOrdersTab(zeroPadding, allOrders, viewModel)
                        "CLIENTS" -> if (hasPermission(currentUser, "VOIR_CLIENTS")) AdminClientsTab(zeroPadding, allClients, allOrders, viewModel)
                        "STOCKS" -> if (hasPermission(currentUser, "VOIR_STOCKS")) AdminStocksTab(zeroPadding, viewModel)
                        "CATALOGUE" -> if (hasPermission(currentUser, "VOIR_CATALOGUE")) AdminCatalogueTab(zeroPadding, viewModel)
                        "PARAMETRES" -> if (hasPermission(currentUser, "VOIR_PARAMETRES")) AdminSettingsTab(zeroPadding, viewModel)
                        "PUBLICITES" -> if (hasPermission(currentUser, "VOIR_PUBLICITES") || currentUser?.permissions?.contains("ALL") == true) AdminPublicitesTab(zeroPadding, viewModel)
                        "STAFF" -> if (hasPermission(currentUser, "VOIR_STAFF")) AdminStaffTab(zeroPadding, viewModel)
                        "REGLES" -> if (hasPermission(currentUser, "GERER_REGLES")) AdminRulesTab(zeroPadding, viewModel)
                        "CORBEILLE_GLOBALE" -> if (hasPermission(currentUser, "VOIR_CORBEILLE")) AdminGlobalTrashTab(zeroPadding, viewModel)
                        "LOGS" -> if (hasPermission(currentUser, "VOIR_LOGS")) AdminLogsTab(zeroPadding, viewModel)
                    }
                }
            }
        }
    }
}

fun exportInvoiceToPDF(context: android.content.Context, order: OrderEntity) {
    val dateString = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(order.createdAt))
    val textContent = """
        FACTURE - BELLA PRESSING
        ----------------------------------------
        Commande N° : ${order.id}
        Date : $dateString
        Client ID : ${order.userId}
        
        Détails :
        Catégorie : ${order.categorie}
        Pièces : ${order.pieces}
        Lavage : ${order.lavageType}
        Gommage : ${order.choixArticleGommage.ifEmpty { "Aucun" }}
        Poids : ${order.kg} Kg
        
        Total à payer : ${order.prixFinal} FCFA
        ----------------------------------------
        Merci pour votre confiance !
    """.trimIndent()
    
    // Pour simplifier et assurer la portabilité dans l'environnement du test, 
    // on exporte la facture en fichier texte (.txt) lisible universellement 
    // et pouvant être imprimé comme un PDF.
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Facture Commande #${order.id}")
        putExtra(android.content.Intent.EXTRA_TEXT, textContent)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Télécharger la facture"))
}

fun exportOrdersToCSV(context: android.content.Context, orders: List<OrderEntity>, filename: String) {
    val csvHeader = "ID,User ID,Status,Categorie,Pieces,Lavage,Choix Gommage,Kg,Prix,Date\n"
    val csvRows = orders.joinToString("\n") { order ->
        val dateString = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(order.createdAt))
        "${order.id},${order.userId},${order.status},${order.categorie},${order.pieces},${order.lavageType},${order.choixArticleGommage},${order.kg},${order.prixFinal},${dateString}"
    }
    val csvContent = csvHeader + csvRows
    
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Export Commandes")
        putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Exporter CSV"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClientsTab(padding: PaddingValues, clients: List<UserEntity>, allOrders: List<OrderEntity>, viewModel: AppViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageClients = hasPermission(currentUser, "GERER_CLIENTS")
    var searchQuery by remember { mutableStateOf("") }
    
    var showTrash by remember { mutableStateOf(false) }
    val archivedClients by viewModel.archivedClients.collectAsStateWithLifecycle()

    val currentClients = if (showTrash) archivedClients else clients

    val filteredClients = currentClients.filter { 
        (it.nom.contains(searchQuery, ignoreCase = true) || 
         it.prenom.contains(searchQuery, ignoreCase = true) || 
         it.email.contains(searchQuery, ignoreCase = true))
    }

    var selectedClient by remember { mutableStateOf<UserEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                placeholder = { Text("Rechercher un client...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                Button(onClick = { showTrash = !showTrash }, colors = ButtonDefaults.buttonColors(containerColor = if (showTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                    Text(if (showTrash) "Actifs" else "Corbeille")
                }
            }
        }

        if (filteredClients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if(showTrash) "La corbeille est vide." else "Aucun client trouvé.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(filteredClients) { client ->
                val clientOrders = allOrders.filter { it.userId == client.id }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        selectedClient = client
                        showBottomSheet = true
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${client.prenom} ${client.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(client.telephone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                val isBanned = client.isBanned || client.estBanni
                                val isSuspended = client.isSuspended || client.estSuspendu
                                if (isBanned) {
                                    val banType = if (client.isBanPermanent || client.bannissementPermanent) "DÉFINITIF" else "TEMPORAIRE"
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            "⛔ BANNI ($banType)",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isSuspended) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            "⏸️ SUSPENDU",
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            "🎁 ${client.accumulatedFreeKgs} Kg gratuits",
                                            color = Color(0xFF2E7D32),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            "Jauge: ${client.currentKgTowardsReward} Kg",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (canManageClients) {
                                IconButton(onClick = {
                                    selectedClient = client
                                    showBottomSheet = true
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Gérer le client")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showBottomSheet && selectedClient != null) {
        ClientBottomSheet(client = selectedClient!!, viewModel = viewModel, onDismiss = { showBottomSheet = false })
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientManagementDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showEditProfile by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var showAntiFraudDialog by remember { mutableStateOf(false) }
    var showBlockOptionsDialog by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canSuspend = hasPermission(currentUser, "SUSPENDRE_CLIENTS")
    val canEdit = hasPermission(currentUser, "GERER_CLIENTS")
    val canViewLogs = hasPermission(currentUser, "VOIR_LOGS")

    val isBanned = client.isBanned || client.estBanni
    val isSuspended = client.isSuspended || client.estSuspendu
    val isSanctioned = isBanned || isSuspended

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gérer: ${client.prenom} ${client.nom}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // STATUT ACTUEL & SANCTIONS
                if (isSanctioned) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBanned) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                if (isBanned) {
                                    val banType = if (client.isBanPermanent || client.bannissementPermanent) "DÉFINITIF" else "TEMPORAIRE"
                                    "⛔ COMPTE BANNI ($banType)"
                                } else "⏸️ COMPTE SUSPENDU",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isBanned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            val reason = if (isBanned) client.banReason.ifBlank { client.raisonBannissement } else client.suspensionReason.ifBlank { client.raisonSuspension }
                            if (reason.isNotBlank()) {
                                Text("Motif: $reason", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (canSuspend) {
                        Button(
                            onClick = {
                                viewModel.liftSanctions(client)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("🟢 Lever la sanction / Réactiver le compte")
                        }
                    }
                } else {
                    if (canSuspend) {
                        // Bouton Suspendre
                        Button(
                            onClick = { showSuspendDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("⏸️ Suspendre le Compte (Mesure Temporaire)")
                        }

                        // Bouton Bannir
                        Button(
                            onClick = { showBanDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("⛔ Bannir le Compte (Sanction Disciplinaire)")
                        }
                    }
                }
                
                if (canEdit) {
                    // Modifier Profil
                    Button(onClick = { showEditProfile = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Éditer le profil & mot de passe")
                    }
                }
                
                if (canViewLogs) {
                    // Voir les logs
                    Button(onClick = { showLogs = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voir le journal d'activité")
                    }
                }
                
                if (canEdit) {
                    // Autorisations Commandes
                    OutlinedButton(
                        onClick = {
                            showBlockOptionsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Personnaliser les autorisations")
                    }
                }
                
                if (canEdit) {
                    // Toggle Promos
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(canUsePromo = !client.canUsePromo))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.canUsePromo) "Bloquer l'utilisation de promos" else "Autoriser les promos")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUser(client.copy(isLoyaltyEnabled = !client.isLoyaltyEnabled))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (client.isLoyaltyEnabled) "Désactiver le programme de fidélité" else "Activer le programme de fidélité")
                    }
                    
                    // Anti-fraud settings
                    OutlinedButton(onClick = { showAntiFraudDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Personnaliser Anti-Fraude")
                    }
                }
                
                if (canSuspend) {
                    // Supprimer le compte (Archive)
                    TextButton(
                        onClick = {
                            viewModel.updateUser(client.copy(isArchived = true))
                            viewModel.logAdminAction(viewModel.currentUser.value?.id, client.id, "ARCHIVE", "L'administrateur a archivé ce compte (suppression douce).")
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Archiver (Supprimer) le compte")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )

    if (showSuspendDialog) {
        SuspendClientDialog(client = client, viewModel = viewModel, onDismiss = { showSuspendDialog = false; onDismiss() })
    }

    if (showBanDialog) {
        BanClientDialog(client = client, viewModel = viewModel, onDismiss = { showBanDialog = false; onDismiss() })
    }

    if (showAntiFraudDialog) {
        var autoBanEnabled by remember { mutableStateOf(client.customAutoBanEnabled) }
        var autoBanThreshold by remember { mutableStateOf(if (client.customAutoBanThreshold > 0) client.customAutoBanThreshold.toString() else "") }
        var autoBanDuration by remember { mutableStateOf(if (client.customAutoBanDurationHours > 0) client.customAutoBanDurationHours.toString() else "") }
        
        AlertDialog(
            onDismissRequest = { showAntiFraudDialog = false },
            title = { Text("Anti-Fraude Personnalisé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Laissez les champs vides pour utiliser la configuration globale (Défaut).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoBanEnabled, onCheckedChange = { autoBanEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activer le banissement pour ce client")
                    }
                    
                    if (autoBanEnabled) {
                        OutlinedTextField(
                            value = autoBanThreshold,
                            onValueChange = { autoBanThreshold = it },
                            label = { Text("Seuil de rejets consécutifs (0 = global)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = autoBanDuration,
                            onValueChange = { autoBanDuration = it },
                            label = { Text("Durée de ban en heures (0 = global)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val threshold = autoBanThreshold.toIntOrNull() ?: 0
                    val duration = autoBanDuration.toIntOrNull() ?: 0
                    
                    viewModel.updateUser(client.copy(
                        customAutoBanEnabled = autoBanEnabled,
                        customAutoBanThreshold = threshold,
                        customAutoBanDurationHours = duration
                    ))
                    showAntiFraudDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAntiFraudDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showBlockOptionsDialog) {
        var canOrder by remember { mutableStateOf(client.canOrder) }
        var canSelectPieces by remember { mutableStateOf(client.canSelectPieces) }
        var canSelectCategories by remember { mutableStateOf(client.canSelectCategories) }
        var canEstimateKg by remember { mutableStateOf(client.canEstimateKg) }
        var canUseExpress by remember { mutableStateOf(client.canUseExpress) }
        var canUsePremium by remember { mutableStateOf(client.canUsePremium) }

        AlertDialog(
            onDismissRequest = { showBlockOptionsDialog = false },
            title = { Text("Personnaliser Autorisations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = canOrder, onCheckedChange = { canOrder = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (canOrder) "Création de commande autorisée" else "Création de commande bloquée", fontWeight = FontWeight.Bold)
                    }
                    if (canOrder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectPieces, onCheckedChange = { canSelectPieces = it })
                            Text("Saisie du nombre de pièces", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectCategories, onCheckedChange = { canSelectCategories = it })
                            Text("Sélection de catégories", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canEstimateKg, onCheckedChange = { canEstimateKg = it })
                            Text("Estimation des kilos", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUseExpress, onCheckedChange = { canUseExpress = it })
                            Text("Service Express", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUsePremium, onCheckedChange = { canUsePremium = it })
                            Text("Garantie Premium", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUser(client.copy(
                        canOrder = canOrder,
                        canSelectPieces = canSelectPieces,
                        canSelectCategories = canSelectCategories,
                        canEstimateKg = canEstimateKg,
                        canUseExpress = canUseExpress,
                        canUsePremium = canUsePremium
                    ))
                    showBlockOptionsDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockOptionsDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showEditProfile) {
        EditClientProfileDialog(client = client, viewModel = viewModel, onDismiss = { showEditProfile = false; onDismiss() })
    }

    if (showLogs) {
        ClientLogsDialog(client = client, viewModel = viewModel, onDismiss = { showLogs = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBottomSheet(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var showAntiFraudDialog by remember { mutableStateOf(false) }
    var showBlockOptionsDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }
    
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val canEdit = hasPermission(currentUser, "GERER_CLIENTS")
    val canSuspend = hasPermission(currentUser, "SUSPENDRE_CLIENTS")
    val canViewLogs = hasPermission(currentUser, "VOIR_LOGS")
    val canRestore = hasPermission(currentUser, "RESTAURER_COMPTE")
    val canHardDelete = hasPermission(currentUser, "SUPPRIMER_DEFINITIVEMENT")

    val isBanned = client.isBanned || client.estBanni
    val isSuspended = client.isSuspended || client.estSuspendu
    val isSanctioned = isBanned || isSuspended

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Actions pour ${client.prenom} ${client.nom}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            // RÉCAPITULATIF PROGRAMME FIDÉLITÉ (JAUGE VISUELLE & SOLDE KILOS GRATUITS)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎁 Solde Kilos Gratuits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${client.accumulatedFreeKgs} Kg offerts",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    
                    val threshold = (config?.loyaltyKgThreshold ?: 10.0).coerceAtLeast(1.0)
                    val progressFraction = (client.currentKgTowardsReward / threshold).toFloat().coerceIn(0f, 1f)
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Progression vers prochain palier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${String.format(java.util.Locale.US, "%.1f", client.currentKgTowardsReward)} / ${threshold} Kg (${(progressFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Text(
                        "Total lavé à vie (Statut VIP) : ${client.lifetimeKgWashed} Kg",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!client.isArchived) {
                // STATUT ACTUEL & SANCTIONS
                if (isSanctioned) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBanned) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (isBanned) {
                                    val banType = if (client.isBanPermanent || client.bannissementPermanent) "DÉFINITIF" else "TEMPORAIRE"
                                    "⛔ COMPTE BANNI ($banType)"
                                } else "⏸️ COMPTE SUSPENDU (Mesure conservatoire)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isBanned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            val reason = if (isBanned) client.banReason.ifBlank { client.raisonBannissement } else client.suspensionReason.ifBlank { client.raisonSuspension }
                            if (reason.isNotBlank()) {
                                Text("Motif: $reason", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            }
                            if (isBanned && !client.isBanPermanent && !client.bannissementPermanent && client.banUntil != null) {
                                val remainingMs = client.banUntil - System.currentTimeMillis()
                                if (remainingMs > 0) {
                                    val hours = remainingMs / (1000 * 60 * 60)
                                    val mins = (remainingMs / (1000 * 60)) % 60
                                    Text("Temps restant: ${hours}h ${mins}min", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (canSuspend) {
                        Button(
                            onClick = {
                                viewModel.liftSanctions(client)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("🟢 Lever la sanction / Réactiver le compte")
                        }
                    }
                } else {
                    if (canSuspend) {
                        Button(
                            onClick = { showSuspendDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("⏸️ Suspendre le Client (Mesure Temporaire)")
                        }

                        Button(
                            onClick = { showBanDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("⛔ Bannir le Client (Sanction Disciplinaire)")
                        }
                    }
                }

                if (canEdit) {
                    Button(onClick = { showEditProfile = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text("Éditer le profil & Points")
                    }

                    Button(onClick = { showBlockOptionsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Personnaliser Autorisations (Commandes)")
                    }

                    Button(onClick = { showAntiFraudDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text("Configurer Anti-Fraude Client")
                    }
                    
                    Button(onClick = { showPromoDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(if (client.canUsePromo) "Bloquer Utilisation Promos" else "Débloquer Utilisation Promos")
                    }
                }
                
                if (canViewLogs) {
                    Button(onClick = { showLogs = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text("Voir Journal d'Activité")
                    }
                }

                if (canEdit) {
                    OutlinedButton(onClick = {
                        viewModel.archiveUser(client.id)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Supprimer (Vers Corbeille)", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                if (canRestore) {
                    Button(onClick = {
                        viewModel.restoreUser(client.id)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Restaurer le compte")
                    }
                }
                
                if (canHardDelete) {
                    OutlinedButton(onClick = {
                        viewModel.hardDeleteUser(client.id)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Supprimer Définitivement", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    if (showPromoDialog) {
        var isPermanentInput by remember { mutableStateOf(true) }
        var durationHours by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            title = { Text(if (client.canUsePromo) "Bloquer Promos" else "Débloquer Promos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (client.canUsePromo) "Voulez-vous empêcher ce client d'utiliser des codes promos ?" else "Voulez-vous autoriser ce client à utiliser des codes promos ?")
                    if (client.canUsePromo) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPermanentInput, onCheckedChange = { isPermanentInput = it })
                            Text("Blocage Permanent")
                        }
                        if (!isPermanentInput) {
                            OutlinedTextField(
                                value = durationHours,
                                onValueChange = { durationHours = it },
                                label = { Text("Durée du blocage (Heures)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val block = client.canUsePromo
                    val durHours = durationHours.toLongOrNull() ?: 0L
                    val until = if (block && !isPermanentInput && durHours > 0) System.currentTimeMillis() + (durHours * 60 * 60 * 1000L) else null
                    
                    viewModel.updateUser(client.copy(canUsePromo = !block, canUsePromoUntil = until))
                    showPromoDialog = false
                    onDismiss()
                }) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showSuspendDialog) {
        SuspendClientDialog(client = client, viewModel = viewModel, onDismiss = { showSuspendDialog = false; onDismiss() })
    }

    if (showBanDialog) {
        BanClientDialog(client = client, viewModel = viewModel, onDismiss = { showBanDialog = false; onDismiss() })
    }

    if (showAntiFraudDialog) {
        var autoBanEnabled by remember { mutableStateOf(client.customAutoBanEnabled) }
        var autoBanThreshold by remember { mutableStateOf(client.customAutoBanThreshold.toString()) }
        var autoBanDuration by remember { mutableStateOf(client.customAutoBanDurationHours.toString()) }

        AlertDialog(
            onDismissRequest = { showAntiFraudDialog = false },
            title = { Text("Anti-Fraude Personnalisée") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoBanEnabled, onCheckedChange = { autoBanEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (autoBanEnabled) "Activé pour ce client" else "Utilise la config globale", fontWeight = FontWeight.Bold)
                    }
                    if (autoBanEnabled) {
                        OutlinedTextField(
                            value = autoBanThreshold,
                            onValueChange = { autoBanThreshold = it },
                            label = { Text("Seuil d'annulation toléré (Ex: 3)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = autoBanDuration,
                            onValueChange = { autoBanDuration = it },
                            label = { Text("Durée du ban (Heures)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val threshold = autoBanThreshold.toIntOrNull() ?: 0
                    val duration = autoBanDuration.toIntOrNull() ?: 0
                    
                    viewModel.updateUser(client.copy(
                        customAutoBanEnabled = autoBanEnabled,
                        customAutoBanThreshold = threshold,
                        customAutoBanDurationHours = duration
                    ))
                    showAntiFraudDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAntiFraudDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showBlockOptionsDialog) {
        var canOrder by remember { mutableStateOf(client.canOrder) }
        var canSelectPieces by remember { mutableStateOf(client.canSelectPieces) }
        var canSelectCategories by remember { mutableStateOf(client.canSelectCategories) }
        var canEstimateKg by remember { mutableStateOf(client.canEstimateKg) }
        var canUseExpress by remember { mutableStateOf(client.canUseExpress) }
        var canUsePremium by remember { mutableStateOf(client.canUsePremium) }

        AlertDialog(
            onDismissRequest = { showBlockOptionsDialog = false },
            title = { Text("Personnaliser Autorisations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = canOrder, onCheckedChange = { canOrder = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (canOrder) "Création de commande autorisée" else "Création de commande bloquée", fontWeight = FontWeight.Bold)
                    }
                    if (canOrder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectPieces, onCheckedChange = { canSelectPieces = it })
                            Text("Saisie du nombre de pièces", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canSelectCategories, onCheckedChange = { canSelectCategories = it })
                            Text("Sélection de catégories", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canEstimateKg, onCheckedChange = { canEstimateKg = it })
                            Text("Estimation des kilos", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUseExpress, onCheckedChange = { canUseExpress = it })
                            Text("Service Express", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = canUsePremium, onCheckedChange = { canUsePremium = it })
                            Text("Garantie Premium", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUser(client.copy(
                        canOrder = canOrder,
                        canSelectPieces = canSelectPieces,
                        canSelectCategories = canSelectCategories,
                        canEstimateKg = canEstimateKg,
                        canUseExpress = canUseExpress,
                        canUsePremium = canUsePremium
                    ))
                    showBlockOptionsDialog = false
                    onDismiss()
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockOptionsDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showEditProfile) {
        EditClientProfileDialog(client = client, viewModel = viewModel, onDismiss = { showEditProfile = false; onDismiss() })
    }

    if (showLogs) {
        ClientLogsDialog(client = client, viewModel = viewModel, onDismiss = { showLogs = false })
    }
}

@Composable
fun SuspendClientDialog(
    client: UserEntity,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var isPermanent by remember { mutableStateOf(false) }
    var durationHours by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏸️ Suspendre le Compte", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "La suspension est une mesure conservatoire temporaire qui bloque l'accès aux commandes et services pour ce client.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motif de la suspension") },
                    placeholder = { Text("Ex: Vérification d'identité, litige en cours...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPermanent, onCheckedChange = { isPermanent = it })
                    Text("Durée indéterminée", style = MaterialTheme.typography.bodyMedium)
                }

                if (!isPermanent) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it },
                        label = { Text("Durée de suspension (en heures)") },
                        placeholder = { Text("Ex: 24, 48, 72") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = if (isPermanent) null else durationHours.toLongOrNull()
                    viewModel.suspendUser(client, reason, hours)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Appliquer la Suspension")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun BanClientDialog(
    client: UserEntity,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var isPermanent by remember { mutableStateOf(false) }
    var durationHours by remember { mutableStateOf("48") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⛔ Bannir le Compte Client", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Le bannissement est une sanction disciplinaire ferme. L'application client affichera l'écran officiel de bannissement.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Text("Type de bannissement :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = !isPermanent,
                            onClick = { isPermanent = false }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isPermanent,
                        onClick = { isPermanent = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("⏳ Bannissement Temporaire", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Exclusion temporaire avec compte à rebours.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isPermanent,
                            onClick = { isPermanent = true }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isPermanent,
                        onClick = { isPermanent = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("🛑 Bannissement Permanent / Définitif", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Text("Radiation irréversible sans expiration.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isPermanent) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it },
                        label = { Text("Durée du bannissement (en heures) *") },
                        placeholder = { Text("Ex: 24, 48, 72, 168 (7 jours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (isPermanent) "Motif de radiation définitive *" else "Motif de la sanction disciplinaire *") },
                    placeholder = { Text(if (isPermanent) "Ex: Fraude répétée, atteinte aux CGU..." else "Ex: Comportement inapproprié, annulations abusives...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = if (isPermanent) null else durationHours.toLongOrNull() ?: 24L
                    viewModel.banUser(client, reason, isPermanent, hours)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isPermanent) "Bannir Définitivement" else "Appliquer le Bannissement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun EditClientProfileDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var nom by remember { mutableStateOf(client.nom.ifEmpty { client.nomUser }) }
    var prenom by remember { mutableStateOf(client.prenom.ifEmpty { client.prenomUser }) }
    var email by remember { mutableStateOf(client.email.ifEmpty { client.mailUser }) }
    var telephone by remember { mutableStateOf(client.telephone.ifEmpty { client.numeroUser }) }
    var coordonnees by remember { mutableStateOf(client.coordonnees.ifEmpty { client.adresseUser }) }
    var password by remember { mutableStateOf(client.motDePasse.ifEmpty { client.motDePasseUser }) }
    val initialFidelity = if (client.fidelityPointsKg > 0f) client.fidelityPointsKg else client.cumulKiloFideliterUser.toFloat()
    var fidelityKg by remember { mutableStateOf(initialFidelity.toString()) }
    var freeKgsInput by remember { mutableStateOf(client.accumulatedFreeKgs.toString()) }
    var towardsRewardInput by remember { mutableStateOf(client.currentKgTowardsReward.toString()) }
    var lifetimeKgInput by remember { mutableStateOf(client.lifetimeKgWashed.toString()) }
    var adjustmentReason by remember { mutableStateOf("") }
    var showErrorReason by remember { mutableStateOf(false) }

    var isLoyaltyEnabled by remember { mutableStateOf(client.isLoyaltyEnabled) }
    var isLoyaltyPermanent by remember { mutableStateOf(client.loyaltyValidUntil == null) }
    var loyaltyDurationDays by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Éditer le profil client") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val isEmailEditable = currentUser?.role == "SUPER_ADMIN" || currentUser?.role == "ADMIN"
                
                OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") })
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") })
                OutlinedTextField(
                    value = email,
                    onValueChange = { if (isEmailEditable) email = it },
                    label = { Text("Adresse email") },
                    readOnly = !isEmailEditable
                )
                OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text("Téléphone") })
                OutlinedTextField(value = coordonnees, onValueChange = { coordonnees = it }, label = { Text("Adresse") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") })
                
                val canManageFidelity = (currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true) && client.role == "CLIENT"
                
                if (canManageFidelity) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎁 Moteur de Fidélité (Kilos Gratuits)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            OutlinedTextField(
                                value = freeKgsInput,
                                onValueChange = { freeKgsInput = it; showErrorReason = false },
                                label = { Text("Solde Kilos Gratuits Disponibles (Kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = towardsRewardInput,
                                onValueChange = { towardsRewardInput = it; showErrorReason = false },
                                label = { Text("Jauge en cours vers prochain palier (Kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = lifetimeKgInput,
                                onValueChange = { lifetimeKgInput = it; showErrorReason = false },
                                label = { Text("Cumul Historique Total Linge (Kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Motif obligatoire en cas de modification manuelle
                            val parsedFreeKgsCheck = freeKgsInput.toDoubleOrNull() ?: client.accumulatedFreeKgs
                            val parsedTowardsCheck = towardsRewardInput.toDoubleOrNull() ?: client.currentKgTowardsReward
                            val parsedLifetimeCheck = lifetimeKgInput.toDoubleOrNull() ?: client.lifetimeKgWashed
                            val hasFidelityChanged = parsedFreeKgsCheck != client.accumulatedFreeKgs ||
                                    parsedTowardsCheck != client.currentKgTowardsReward ||
                                    parsedLifetimeCheck != client.lifetimeKgWashed

                            if (hasFidelityChanged) {
                                OutlinedTextField(
                                    value = adjustmentReason,
                                    onValueChange = { adjustmentReason = it; showErrorReason = false },
                                    label = { Text("Motif de l'ajustement (OBLIGATOIRE) *") },
                                    isError = showErrorReason,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        if (showErrorReason) {
                                            Text("Veuillez renseigner un motif pour la traçabilité de l'audit.", color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("Requis pour la conformité et l'historique d'audit.")
                                        }
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = isLoyaltyEnabled, onCheckedChange = { isLoyaltyEnabled = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Programme activé pour ce client")
                            }
                            if (isLoyaltyEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isLoyaltyPermanent, onCheckedChange = { isLoyaltyPermanent = it })
                                    Text("Permanent")
                                }
                                if (!isLoyaltyPermanent) {
                                    OutlinedTextField(
                                        value = loyaltyDurationDays,
                                        onValueChange = { loyaltyDurationDays = it },
                                        label = { Text("Durée d'activation (jours)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            Button(onClick = {
                val parsedFreeKgs = freeKgsInput.toDoubleOrNull() ?: client.accumulatedFreeKgs
                val parsedTowards = towardsRewardInput.toDoubleOrNull() ?: client.currentKgTowardsReward
                val parsedLifetime = lifetimeKgInput.toDoubleOrNull() ?: client.lifetimeKgWashed
                val hasFidelityChanged = parsedFreeKgs != client.accumulatedFreeKgs ||
                        parsedTowards != client.currentKgTowardsReward ||
                        parsedLifetime != client.lifetimeKgWashed

                if (hasFidelityChanged && adjustmentReason.trim().isEmpty()) {
                    showErrorReason = true
                    return@Button
                }

                val parsedFidelity = fidelityKg.toFloatOrNull() ?: initialFidelity
                val updatedClient = client.copy(
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    telephone = telephone,
                    coordonnees = coordonnees,
                    motDePasse = password,
                    fidelityPointsKg = parsedFidelity,
                    cumulKiloFideliterUser = parsedFidelity.toDouble(),
                    accumulatedFreeKgs = parsedFreeKgs,
                    currentKgTowardsReward = parsedTowards,
                    lifetimeKgWashed = parsedLifetime,
                    nomUser = nom,
                    prenomUser = prenom,
                    mailUser = email,
                    numeroUser = telephone,
                    adresseUser = coordonnees,
                    motDePasseUser = password,
                    isLoyaltyEnabled = isLoyaltyEnabled,
                    loyaltyValidUntil = if (!isLoyaltyEnabled) null else if (isLoyaltyPermanent) null else (System.currentTimeMillis() + (loyaltyDurationDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000)
                )
                viewModel.updateUser(updatedClient)

                if (hasFidelityChanged) {
                    val diff = parsedFreeKgs - client.accumulatedFreeKgs
                    val diffStr = if (diff > 0) "+$diff" else "$diff"
                    viewModel.logAdminAction(
                        adminId = currentUser?.id ?: "ADMIN",
                        targetUserId = client.id,
                        action = "AJUSTEMENT_FIDELITE_MANUEL",
                        description = "Ajustement Fidélité : $diffStr Kg pour le client ${client.prenom} ${client.nom} (Nouveau solde: $parsedFreeKgs Kg, Jauge: $parsedTowards Kg, VIP: $parsedLifetime Kg). Motif : ${adjustmentReason.trim()}"
                    )
                }

                onDismiss()
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun ClientLogsDialog(client: UserEntity, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val logs by viewModel.getActivityLogsForUser(client.id).collectAsStateWithLifecycle(initialValue = emptyList<ActivityLogEntity>())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activité de ${client.prenom}") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(logs) { log ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(log.description, style = MaterialTheme.typography.bodySmall)
                        val dateString = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(log.timestamp))
                        Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun AdminOrdersTab(padding: PaddingValues, allOrders: List<OrderEntity>, viewModel: AppViewModel) {
    val context = LocalContext.current
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredOrders = allOrders.filter { order -> !order.isArchived &&
        if (searchQuery.isBlank()) true
        else order.id.toString().contains(searchQuery) || order.userId.contains(searchQuery, ignoreCase = true)
    }
    
    val enAttente = filteredOrders.count { it.status == "EN_ATTENTE" || it.status == "PROPOSITION_ADMIN" }
    val acceptees = filteredOrders.count { it.status == "ACCEPTE" || it.status == "EN_COURS" }
    val terminees = filteredOrders.count { it.status == "TERMINE" }
    val rejetees = filteredOrders.count { it.status == "REJETE" }
    val totalOrders = filteredOrders.size

    // Group orders by client (userId)
    val clientOrderGroups = filteredOrders.groupBy { it.userId }.entries.sortedByDescending { entry ->
        val clientOrders = entry.value
        val newCount = clientOrders.count { it.status == "EN_ATTENTE" || it.status == "PROPOSITION_ADMIN" || it.status == "ACCEPTE" }
        val lastTimestamp = clientOrders.maxOfOrNull { it.createdAt } ?: 0L
        if (newCount > 0) (10000000000000L + newCount * 1000000000L + lastTimestamp) else lastTimestamp
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher (Nom Client, ID Commande, Téléphone)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistiques Générales", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            exportOrdersToCSV(context, allOrders, "Toutes_Commandes.csv")
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Exporter tout (CSV)", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(enAttente.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Text("En attente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(acceptees.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("En cours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(terminees.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Text("Terminée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rejetees.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Rejetée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(totalOrders.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Boîte de Réception des Clients (${clientOrderGroups.size} Clients)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (clientOrderGroups.isEmpty()) {
            item {
                Text("Aucune commande enregistrée.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(clientOrderGroups) { entry ->
                val userId = entry.key
                val clientOrders = entry.value
                val clientUser = allClients.find { it.id == userId }
                
                ClientOrderInboxCard(
                    userId = userId,
                    clientOrders = clientOrders,
                    clientUser = clientUser,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ClientOrderInboxCard(
    userId: String,
    clientOrders: List<OrderEntity>,
    clientUser: UserEntity?,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var activeMenuSection by remember { mutableStateOf<String?>(null) } // "NEW", "OLD", "HISTORY", "LOGS", "ACTIONS"

    val newOrders = clientOrders.filter { it.status == "EN_ATTENTE" || it.status == "PROPOSITION_ADMIN" || it.status == "ACCEPTE" }
    val oldOrders = clientOrders.filter { it.status != "EN_ATTENTE" && it.status != "PROPOSITION_ADMIN" && it.status != "ACCEPTE" }
    val newCount = newOrders.size
    val lastOrder = clientOrders.maxByOrNull { it.createdAt }
    val logs by viewModel.getActivityLogsForUser(userId).collectAsStateWithLifecycle(initialValue = emptyList<ActivityLogEntity>())

    val clientName = clientUser?.let { "${it.prenom} ${it.nom}".trim() }.takeIf { !it.isNullOrBlank() } ?: "Client #${userId.take(8)}"
    val clientPhone = clientUser?.telephone.orEmpty().ifEmpty { clientUser?.numeroUser.orEmpty() }
    val clientEmail = clientUser?.email.orEmpty().ifEmpty { clientUser?.mailUser.orEmpty() }
    val clientAddress = clientUser?.coordonnees.orEmpty().ifEmpty { clientUser?.adresseUser.orEmpty() }

    val initials = clientName.split(" ").filter { it.isNotBlank() }.map { it.first().uppercase() }.take(2).joinToString("")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (newCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (newCount > 0) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // WHATSAPP / INBOX HEADER ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Photo Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (newCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "C" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (clientUser != null) {
                            val isBanned = clientUser.isBanned || clientUser.estBanni
                            val isSuspended = clientUser.isSuspended || clientUser.estSuspendu
                            if (isBanned) {
                                val banType = if (clientUser.isBanPermanent || clientUser.bannissementPermanent) "DÉFINITIF" else "TEMPORAIRE"
                                Text(
                                    text = "⛔ BANNI ($banType)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (isSuspended) {
                                Text(
                                    text = "⏸️ SUSPENDU",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        if (clientPhone.isNotBlank()) {
                            Text(
                                text = "📞 $clientPhone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lastOrder != null) {
                            Text(
                                text = "Dernière cmd: #${lastOrder.id} • ${lastOrder.status.replace("_", " ")} (${lastOrder.kg} Kg)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Notification Badge
                Column(horizontalAlignment = Alignment.End) {
                    if (newCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = "$newCount NOUVELLE${if (newCount > 1) "S" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "${clientOrders.size} cmd(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isExpanded) "▲ Masquer" else "▼ Ouvrir le profil",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // EXPANDABLE MAIN CLIENT CONTAINER WITH HIDDEN DRAWER MENU
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Full Client Profile Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Profil Client & Coordonnées Complètes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (clientEmail.isNotBlank()) Text("📧 Email: $clientEmail", style = MaterialTheme.typography.bodySmall)
                    if (clientAddress.isNotBlank()) Text("📍 Adresse: $clientAddress", style = MaterialTheme.typography.bodySmall)
                    Text("📦 Total Commandes: ${clientOrders.size} (${newCount} en attente, ${oldOrders.size} traitées)", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // HIDDEN COLLAPSIBLE DRAWER / BUTTONS MENU
                Text("Menu des Actions & Historique:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeMenuSection == "NEW",
                        onClick = { activeMenuSection = if (activeMenuSection == "NEW") null else "NEW" },
                        label = { Text("Nouvelles (${newCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                    FilterChip(
                        selected = activeMenuSection == "OLD",
                        onClick = { activeMenuSection = if (activeMenuSection == "OLD") null else "OLD" },
                        label = { Text("Anciennes (${oldOrders.size})") }
                    )
                    FilterChip(
                        selected = activeMenuSection == "HISTORY",
                        onClick = { activeMenuSection = if (activeMenuSection == "HISTORY") null else "HISTORY" },
                        label = { Text("Historique (${clientOrders.size})") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeMenuSection == "LOGS",
                        onClick = { activeMenuSection = if (activeMenuSection == "LOGS") null else "LOGS" },
                        label = { Text("Activités (${logs.size})") }
                    )
                    FilterChip(
                        selected = activeMenuSection == "ACTIONS",
                        onClick = { activeMenuSection = if (activeMenuSection == "ACTIONS") null else "ACTIONS" },
                        label = { Text("Actions Rapides") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SECTION CONTENT DISPLAY BASED ON MENU CHOICE
                when (activeMenuSection) {
                    "NEW" -> {
                        if (newOrders.isEmpty()) {
                            Text("Aucune nouvelle commande en attente pour ce client.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                newOrders.forEach { order ->
                                    AdminOrderCard(order = order, viewModel = viewModel)
                                }
                            }
                        }
                    }
                    "OLD" -> {
                        if (oldOrders.isEmpty()) {
                            Text("Aucune ancienne commande pour ce client.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                oldOrders.forEach { order ->
                                    AdminOrderCard(order = order, viewModel = viewModel)
                                }
                            }
                        }
                    }
                    "HISTORY" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Historique chronologique des commandes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            clientOrders.sortedByDescending { it.createdAt }.forEach { order ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Commande #${order.id} • ${order.categorie}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Pièces: ${order.pieces} | Kg: ${order.kg} Kg | Statut: ${order.status}", style = MaterialTheme.typography.bodySmall)
                                            Text("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${order.prixFinal.toInt()} FCFA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    "LOGS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Journal des Activités du Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (logs.isEmpty()) {
                                Text("Aucun journal d'activité enregistré pour le moment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                logs.take(15).forEach { log ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            Text(log.description, style = MaterialTheme.typography.labelSmall)
                                            Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "ACTIONS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Actions Rapides sur le Client", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (clientPhone.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$clientPhone"))
                                                context.startActivity(intent)
                                            } catch (e: Throwable) { e.printStackTrace() }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📞 Appeler")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:$clientPhone"))
                                                context.startActivity(intent)
                                            } catch (e: Throwable) { e.printStackTrace() }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("💬 SMS")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // By default show active/new orders if any
                        if (newOrders.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Commandes à traiter (${newOrders.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                newOrders.forEach { order ->
                                    AdminOrderCard(order = order, viewModel = viewModel)
                                }
                            }
                        } else if (oldOrders.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Dernière commande traitée:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                AdminOrderCard(order = oldOrders.first(), viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderCard(order: OrderEntity, viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()
    val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val client = allClients.find { it.id == order.userId }
    val isFirestoreQuotaExhausted by viewModel.isFirestoreQuotaExhausted.collectAsStateWithLifecycle()
    val canManageOrders = hasPermission(currentUser, "GERER_COMMANDES") && !isFirestoreQuotaExhausted
    
    var isEditing by remember { mutableStateOf(false) }
    
    var piecesInput by remember { mutableStateOf(order.pieces.toString()) }
    var categorieInput by remember { mutableStateOf(order.categorie) }
    var lavageTypeInput by remember { mutableStateOf(order.lavageType) }
    var kgInput by remember { mutableStateOf(if (order.kg > 0) order.kg.toString() else "") }
    var usedFreeKgsInput by remember { mutableStateOf(if (order.usedFreeKgs > 0.0) order.usedFreeKgs.toString() else "") }
    var adminMessageInput by remember { mutableStateOf(order.adminMessage) }
    
    var isExpressInput by remember { mutableStateOf(order.isExpress) }
    var deliveryTypeInput by remember { mutableStateOf(order.deliveryType) }
    var pickupDateInput by remember { mutableStateOf(order.pickupDate) }
    var deliveryDateInput by remember { mutableStateOf(order.deliveryDate) }
    var instructionsSpecialesInput by remember { mutableStateOf(order.instructionsSpeciales) }
    var hasPremiumGuaranteeInput by remember { mutableStateOf(order.hasPremiumGuarantee) }
    var promoCodeInput by remember { mutableStateOf(order.promoCode) }
    var adresseLivraisonInput by remember { mutableStateOf(order.adresseLivraison) }
    var photoUriInput by remember { mutableStateOf(order.photoUri) }
    
    var specialItemsMap = remember { mutableStateMapOf<String, Int>() }
    
    LaunchedEffect(order) {
        if (order.specialItemsData.isNotBlank()) {
            specialItemsMap.clear()
            order.specialItemsData.split(",").forEach {
                val parts = it.split(":")
                if (parts.size == 2) {
                    specialItemsMap[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            photoUriInput = "photo_${System.currentTimeMillis()}.png"
        }
    }

    val statusColor = when(order.status) {
        "EN_ATTENTE" -> MaterialTheme.colorScheme.tertiary
        "PROPOSITION_ADMIN" -> MaterialTheme.colorScheme.secondary
        "ACCEPTE" -> MaterialTheme.colorScheme.primary
        "TERMINE" -> Color(0xFF4CAF50)
        "REJETE" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    val timeOptions = (0..23).flatMap { listOf(String.format("%02d:00", it), String.format("%02d:30", it)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Commande #${order.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (order.isExpress) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("URGENT", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Client ID: ${order.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val pendingActions by Graph.repository.getPendingActionsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
                    val isPendingSync = remember(pendingActions, order.id) { pendingActions.any { it.payloadJson.contains(order.id) } }
                    if (isPendingSync) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sync locale en attente",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(order.status.replace("_", " "), color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditing) {
                // Editing form
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = piecesInput, onValueChange = { piecesInput = it }, label = { Text("Pièces") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = categorieInput, onValueChange = { categorieInput = it }, label = { Text("Catégorie") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Type de Lavage", style = MaterialTheme.typography.labelSmall)
                val lavageOptions = listOf("Simple" to "SIMPLE", "Lavage + Séchage + Pliage" to "LAVAGE_SECHAGE_PLIAGE", "Complet (avec repassage)" to "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "Aucun (Gommage uniquement)" to "AUCUN")
                Column {
                    lavageOptions.forEach { (label, type) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = lavageTypeInput == type, onClick = { lavageTypeInput = type })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = kgInput, onValueChange = { kgInput = it }, label = { Text("Poids (Kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                // Double bouclier mathématique pour l'ajustement des Kilos Gratuits
                val clientAvailableFreeKgs = (client?.accumulatedFreeKgs ?: 0.0) + order.usedFreeKgs
                if (clientAvailableFreeKgs > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentWeightVal = (kgInput.toDoubleOrNull() ?: order.kg.toDouble()).coerceAtLeast(0.0)
                    val maxAllowed = minOf(currentWeightVal, clientAvailableFreeKgs)
                    val enteredUsed = usedFreeKgsInput.toDoubleOrNull() ?: 0.0
                    val isExcess = enteredUsed > maxAllowed

                    OutlinedTextField(
                        value = usedFreeKgsInput,
                        onValueChange = { usedFreeKgsInput = it },
                        label = { Text("Kilos Gratuits déduits (Fidélité)") },
                        supportingText = {
                            if (isExcess) {
                                Text("Dépassement interdit : max $maxAllowed Kg déductibles", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Solde client : ${client?.accumulatedFreeKgs ?: 0.0} Kg disponible (Max déductible pour ce lot : $maxAllowed Kg)")
                            }
                        },
                        isError = isExcess,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isExpressInput, onCheckedChange = { isExpressInput = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Express (Multiplicateur x${config?.expressMultiplier ?: 1.3f})")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Articles Spéciaux", style = MaterialTheme.typography.labelMedium)
                specialItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                if (current > 0) specialItemsMap[item.name] = current - 1
                            }) { Text("-") }
                            Text("${specialItemsMap[item.name] ?: 0}")
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                specialItemsMap[item.name] = current + 1
                            }) { Text("+") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Articles de Gommage", style = MaterialTheme.typography.labelMedium)
                gommageItems.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                if (current > 0) specialItemsMap[item.name] = current - 1
                            }) { Text("-") }
                            Text("${specialItemsMap[item.name] ?: 0}")
                            IconButton(onClick = { 
                                val current = specialItemsMap[item.name] ?: 0
                                specialItemsMap[item.name] = current + 1
                            }) { Text("+") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dates & Heures", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pickupDateInput, onValueChange = { pickupDateInput = it }, label = { Text("Jour Ramassage (Ex: 10/10)") }, modifier = Modifier.weight(1f))
                    var expandedPickTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedPickTime, onExpandedChange = { expandedPickTime = !expandedPickTime }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = pickupDateInput.substringAfter(" ", ""),
                            onValueChange = { },
                            label = { Text("Heure") },
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPickTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedPickTime, onDismissRequest = { expandedPickTime = false }) {
                            timeOptions.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { pickupDateInput = pickupDateInput.substringBefore(" ") + " " + t; expandedPickTime = false }) }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = deliveryDateInput, onValueChange = { deliveryDateInput = it }, label = { Text("Jour Livraison (Ex: 12/10)") }, modifier = Modifier.weight(1f))
                    var expandedDelTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedDelTime, onExpandedChange = { expandedDelTime = !expandedDelTime }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = deliveryDateInput.substringAfter(" ", ""),
                            onValueChange = { },
                            label = { Text("Heure") },
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDelTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedDelTime, onDismissRequest = { expandedDelTime = false }) {
                            timeOptions.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { deliveryDateInput = deliveryDateInput.substringBefore(" ") + " " + t; expandedDelTime = false }) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUriInput.isNotBlank()) "Photo Capturée" else "Prendre une photo des tâches")
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (order.latitude != 0.0) {
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Carte Maps: Lat ${order.latitude}, Lng ${order.longitude}")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = adminMessageInput,
                    onValueChange = { adminMessageInput = it },
                    label = { Text("Message pour le client (Optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { 
                        isEditing = false 
                        piecesInput = order.pieces.toString()
                        categorieInput = order.categorie
                        kgInput = if(order.kg > 0) order.kg.toString() else ""
                    }) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val k = kgInput.toFloatOrNull() ?: 0f
                        val clientAvailableFreeKgs = (client?.accumulatedFreeKgs ?: 0.0) + order.usedFreeKgs
                        val enteredUsed = usedFreeKgsInput.toDoubleOrNull() ?: 0.0
                        // Double bouclier mathématique : borné entre 0 et min(poidsTotal, soldeClientDispo)
                        val safeUsedFreeKgs = enteredUsed.coerceIn(0.0, minOf(k.toDouble(), clientAvailableFreeKgs))

                        val itemsCostMap = specialItemsMap.mapValues { entry ->
                            val pSpec = specialItems.find { it.name == entry.key }?.price
                            val pGom = gommageItems.find { it.name == entry.key }?.price
                            val p = pSpec ?: (pGom ?: 0f)
                            p * entry.value
                        }
                        val validPromo = allPromos.find { it.code == order.promoCode }
                        var calcPrice = calculateOrderPrice(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            kg = k,
                            lavageType = lavageTypeInput,
                            config = config,
                            isExpress = isExpressInput,
                            isPremium = hasPremiumGuaranteeInput,
                            promo = validPromo,
                            specialItemsCostMap = itemsCostMap,
                            transportFee = order.transportFee,
                            gommageItemsList = gommageItems,
                            usedFreeKgs = safeUsedFreeKgs
                        )
                        
                        val specialItemsStr = specialItemsMap.filter { it.value > 0 }.map { "${it.key}:${it.value}" }.joinToString(",")

                        val deltaUsed = safeUsedFreeKgs - order.usedFreeKgs
                        if (deltaUsed != 0.0 && client != null) {
                            val updatedClient = client.copy(
                                accumulatedFreeKgs = (client.accumulatedFreeKgs - deltaUsed).coerceAtLeast(0.0)
                            )
                            viewModel.updateUser(updatedClient)
                            viewModel.logAdminAction(
                                adminId = currentUser?.id ?: "ADMIN",
                                targetUserId = client.id,
                                action = "UTILISATION_KILO_GRATUIT",
                                description = "Ajustement Fidélité : -${safeUsedFreeKgs} Kg pour le client ${client.prenom} ${client.nom} via Commande #${order.id}. Motif : Déduction de $safeUsedFreeKgs Kg gratuits sur commande #${order.id} (Nouveau solde: ${updatedClient.accumulatedFreeKgs} Kg)"
                            )
                        }

                        val updated = order.copy(
                            pieces = piecesInput.toIntOrNull() ?: 0,
                            categorie = categorieInput,
                            lavageType = lavageTypeInput,
                            kg = k,
                            weight = k.toDouble(),
                            usedFreeKgs = safeUsedFreeKgs,
                            fideliteKgUtilise = safeUsedFreeKgs,
                            fidelityKgUsed = safeUsedFreeKgs.toFloat(),
                            prixFinal = calcPrice.toFloat(),
                            totalPrice = calcPrice,
                            adminMessage = adminMessageInput,
                            status = "PROPOSITION_ADMIN",
                            specialItemsData = specialItemsStr,
                            pickupDate = pickupDateInput,
                            deliveryDate = deliveryDateInput,
                            photoUri = photoUriInput
                        )
                        viewModel.updateOrder(updated)
                        isEditing = false
                    }) {
                        Text("Confirmer les modifications")
                    }
                }
            } else {
                // View mode
                Column {
                    if (client != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Client : ${client.prenom} ${client.nom}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Téléphone : ${client.telephone}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Email : ${client.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${order.pieces} pièces • ${order.categorie}", style = MaterialTheme.typography.bodyMedium)
                        Text("${order.prixFinal.toInt()} FCFA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    val freeKgsUsed = if (order.usedFreeKgs > 0.0) order.usedFreeKgs else (if (order.fidelityKgUsed > 0) order.fidelityKgUsed.toDouble() else order.fideliteKgUtilise)
                    if (freeKgsUsed > 0 || order.discountFixed > 0) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎁 Kilo Gratuit Appliqué : ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("${freeKgsUsed} Kg déduits", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20))
                            }
                        }
                    }

                    if (order.kg > 0) {
                        Text("Poids estimé: ${order.kg} Kg", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (order.specialItemsData.isNotBlank()) {
                        Text("Articles Spéciaux:", style = MaterialTheme.typography.labelMedium)
                        order.specialItemsData.split(",").forEach {
                            Text("- $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (order.pickupDate.isNotBlank()) Text("Ramassage: ${order.pickupDate}", style = MaterialTheme.typography.bodySmall)
                    if (order.deliveryDate.isNotBlank()) Text("Livraison: ${order.deliveryDate}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))
                    if (order.photoUri.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = order.photoUri,
                                contentDescription = "Photo des vêtements",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aucune photo des vêtements", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (order.status == "EN_ATTENTE" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = kgInput,
                            onValueChange = { kgInput = it },
                            label = { Text("Poids après pesée (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        val clientAvailableFreeKgs = (client?.accumulatedFreeKgs ?: 0.0) + order.usedFreeKgs
                        if (clientAvailableFreeKgs > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val currentWeightVal = (kgInput.toDoubleOrNull() ?: order.kg.toDouble()).coerceAtLeast(0.0)
                            val maxAllowed = minOf(currentWeightVal, clientAvailableFreeKgs)
                            val enteredUsed = usedFreeKgsInput.toDoubleOrNull() ?: 0.0
                            val isExcess = enteredUsed > maxAllowed

                            OutlinedTextField(
                                value = usedFreeKgsInput,
                                onValueChange = { usedFreeKgsInput = it },
                                label = { Text("Kilos Gratuits déduits (Fidélité)") },
                                supportingText = {
                                    if (isExcess) {
                                        Text("Dépassement interdit : max $maxAllowed Kg déductibles", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("Solde client : ${client?.accumulatedFreeKgs ?: 0.0} Kg disponible (Max: $maxAllowed Kg)")
                                    }
                                },
                                isError = isExcess,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    val k = kgInput.toFloatOrNull() ?: order.kg
                                    val clientAvailableFreeKgs = (client?.accumulatedFreeKgs ?: 0.0) + order.usedFreeKgs
                                    val enteredUsed = usedFreeKgsInput.toDoubleOrNull() ?: order.usedFreeKgs
                                    val safeUsedFreeKgs = enteredUsed.coerceIn(0.0, minOf(k.toDouble(), clientAvailableFreeKgs))

                                    val itemsCostMap = mutableMapOf<String, Float>()
                                    order.specialItemsData.split(",").filter { it.isNotBlank() }.forEach { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val pSpec = specialItems.find { item -> item.name == parts[0] }?.price
                                            val pGom = gommageItems.find { item -> item.name == parts[0] }?.price
                                            val p = pSpec ?: (pGom ?: 0f)
                                            itemsCostMap[parts[0]] = p * (parts[1].toIntOrNull() ?: 0)
                                        }
                                    }
                                    val validPromo = allPromos.find { it.code == order.promoCode }
                                    var calcPrice = calculateOrderPrice(
                                        pieces = order.pieces,
                                        kg = k,
                                        lavageType = order.lavageType,
                                        config = config,
                                        isExpress = order.isExpress,
                                        isPremium = order.hasPremiumGuarantee,
                                        promo = validPromo,
                                        specialItemsCostMap = itemsCostMap,
                                        transportFee = order.transportFee,
                                        gommageItemsList = gommageItems,
                                        usedFreeKgs = safeUsedFreeKgs
                                    )

                                    val deltaUsed = safeUsedFreeKgs - order.usedFreeKgs
                                    if (deltaUsed != 0.0 && client != null) {
                                        val updatedClient = client.copy(
                                            accumulatedFreeKgs = (client.accumulatedFreeKgs - deltaUsed).coerceAtLeast(0.0)
                                        )
                                        viewModel.updateUser(updatedClient)
                                        viewModel.logAdminAction(
                                            adminId = currentUser?.id ?: "ADMIN",
                                            targetUserId = client.id,
                                            action = "UTILISATION_KILO_GRATUIT",
                                            description = "Ajustement Fidélité : -${safeUsedFreeKgs} Kg pour le client ${client.prenom} ${client.nom} via Commande #${order.id}. Motif : Déduction de $safeUsedFreeKgs Kg gratuits sur commande #${order.id} (Nouveau solde: ${updatedClient.accumulatedFreeKgs} Kg)"
                                        )
                                    }

                                    viewModel.updateOrder(order.copy(
                                        status = "PROPOSITION_ADMIN",
                                        timelineStep = 1,
                                        kg = k,
                                        weight = k.toDouble(),
                                        usedFreeKgs = safeUsedFreeKgs,
                                        fideliteKgUtilise = safeUsedFreeKgs,
                                        fidelityKgUsed = safeUsedFreeKgs.toFloat(),
                                        prixFinal = calcPrice.toFloat(),
                                        totalPrice = calcPrice
                                    ))
                                },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Proposer le prix")
                            }
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Modifier")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.updateOrder(order.copy(status = "REJETE")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rejeter", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (order.status == "PROPOSITION_ADMIN" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "⏳ Proposition envoyée au client",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Poids proposé : ${order.kg} Kg\nPrix proposé : ${order.prixFinal.toInt()} FCFA\n\nEn attente de la validation du client. Vous pouvez également forcer l'acceptation si le client a validé verbalement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    viewModel.updateOrder(order.copy(status = "ACCEPTE", timelineStep = 1))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Forcer l'acceptation")
                            }
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Modifier")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.updateOrder(order.copy(status = "REJETE")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Annuler / Rejeter la commande", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (order.status == "ACCEPTE" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("✅ Le client a validé la commande !", color = Color(0xFF1B5E20), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Le devis proposé (${order.kg} Kg • ${order.prixFinal.toInt()} FCFA) a été accepté. Vous pouvez lancer le traitement.", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Button(
                            onClick = { viewModel.updateOrder(order.copy(status = "EN_COURS", timelineStep = 2)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Démarrer le lavage (En Plein lavage)")
                        }
                    } else if (order.status == "EN_COURS" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (order.timelineStep == 2) {
                            Button(onClick = { viewModel.updateOrder(order.copy(timelineStep = 3)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("En séchage")
                            }
                        } else if (order.timelineStep == 3) {
                            Button(onClick = { viewModel.updateOrder(order.copy(timelineStep = 4)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("En Repassage")
                            }
                        } else if (order.timelineStep == 4) {
                            Button(onClick = { viewModel.updateOrder(order.copy(status = "TERMINE", timelineStep = 5)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                                Text("Finalisation du Service")
                            }
                        } else {
                            Button(onClick = { viewModel.updateOrder(order.copy(timelineStep = 3)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("En séchage")
                            }
                        }
                    }

                    if (order.status == "ACCEPTE" || order.status == "EN_COURS" || order.status == "TERMINE") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { /* Export to PDF */ }, modifier = Modifier.fillMaxWidth()) {
                            Text("Télécharger la Facture PDF")
                        }
                    }
                    if (order.status == "ACCEPTE" || order.status == "EN_COURS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.updateOrder(order.copy(status = "REJETE")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text("⚠️ Forcer l'annulation de la commande")
                        }
                    }
                    
                    if (order.status == "TERMINE") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Avis du client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (order.rating > 0) {
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                imageVector = if (i < order.rating) androidx.compose.material.icons.Icons.Default.Star else androidx.compose.material.icons.Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (i < order.rating) Color(0xFFFFC107) else Color.Gray
                                            )
                                        }
                                    }
                                    if (order.clientComment.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("\"${order.clientComment}\"", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                } else {
                                    Text("Pas encore d'avis laissé.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


fun calculateOrderPrice(
    pieces: Int,
    kg: Double,
    lavageType: String,
    config: com.example.data.ConfigEntity?,
    isExpress: Boolean,
    isPremium: Boolean,
    promo: com.example.data.PromoEntity? = null,
    specialItemsCostMap: Map<String, Float> = emptyMap(),
    transportFee: Float = 0f,
    gommageItemsList: List<com.example.data.GommageItemEntity> = emptyList(),
    usedFreeKgs: Double = 0.0
): Double {
    val prixLavageType = when (lavageType) {
        "SIMPLE", "Lavage simple" -> 350.0
        "LAVAGE_SECHAGE_PLIAGE", "Lavage + séchage + pliage" -> 400.0
        "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "Lavage + séchage + repassage + pliage" -> 500.0
        else -> 0.0
    }

    // Moteur Kilo Gratuit (Fidélité Dynamique) :
    // - Soustraction stricte de usedFreeKgs sur le poids au kilo
    // - Sécurité anti-dépassement : usedFreeKgs plafonné au poids total
    // - Non négatif
    // - N'affecte JAMAIS les articles spéciaux, le gommage ou le transport
    val safeUsedFreeKgs = usedFreeKgs.coerceAtLeast(0.0)
    val effectiveFreeKgs = minOf(safeUsedFreeKgs, kg.coerceAtLeast(0.0))
    val effectiveKg = (kg - effectiveFreeKgs).coerceAtLeast(0.0)
    val prixKg = effectiveKg * prixLavageType
    
    var prixGommage = 0.0
    var prixSpecialItems = 0.0
    
    specialItemsCostMap.forEach { (name, cost) ->
        val isGommage = gommageItemsList.any { it.name == name }
        if (isGommage) {
            prixGommage += cost.toDouble()
        } else {
            prixSpecialItems += cost.toDouble()
        }
    }
    
    var totalTargetedForDiscount = 0.0
    var totalNonTargeted = 0.0
    
    val targetItems = promo?.targetItems?.split(",")?.map { it.trim() } ?: listOf()
    val targetsAll = targetItems.contains("ALL") || targetItems.contains("COMMANDE_COMPLETE") || targetItems.isEmpty()

    if (targetsAll) {
        totalTargetedForDiscount += prixKg + prixGommage + prixSpecialItems
    } else {
        if (targetItems.contains(lavageType) || (targetItems.contains("LAVAGE_SIMPLE") && (lavageType == "SIMPLE" || lavageType == "Lavage simple"))) {
            totalTargetedForDiscount += prixKg
        } else {
            totalNonTargeted += prixKg
        }
        
        if (targetItems.contains("GOMMAGE")) {
            totalTargetedForDiscount += prixGommage
        } else {
            totalNonTargeted += prixGommage
        }
        
        specialItemsCostMap.forEach { (name, cost) ->
            val isGommage = gommageItemsList.any { it.name == name }
            if (!isGommage) {
                if (targetItems.contains(name)) {
                    totalTargetedForDiscount += cost.toDouble()
                } else {
                    totalNonTargeted += cost.toDouble()
                }
            }
        }
    }
    
    val totalDiscount = if (promo != null && promo.isActive) {
        if (promo.discountType == "PERCENTAGE") {
            totalTargetedForDiscount * promo.discountPercent.toDouble()
        } else {
            minOf(totalTargetedForDiscount, promo.discountPercent.toDouble())
        }
    } else 0.0
    
    val finalItemsPrice = (totalTargetedForDiscount - totalDiscount) + totalNonTargeted
    
    val insuranceFee = if (isPremium) finalItemsPrice * 0.1 else 0.0
    val multiplier = if (isExpress) (config?.expressMultiplier?.toDouble() ?: 1.3) else 1.0
    
    val totalAvecExpress = (finalItemsPrice + insuranceFee) * multiplier
    
    return Math.round(totalAvecExpress + transportFee.toDouble()).toDouble()
}

// Surcharge de compatibilité Float
fun calculateOrderPrice(
    pieces: Int,
    kg: Float,
    lavageType: String,
    config: com.example.data.ConfigEntity?,
    isExpress: Boolean,
    isPremium: Boolean,
    promo: com.example.data.PromoEntity? = null,
    specialItemsCostMap: Map<String, Float> = emptyMap(),
    transportFee: Float = 0f,
    gommageItemsList: List<com.example.data.GommageItemEntity> = emptyList(),
    usedFreeKgs: Double = 0.0
): Double = calculateOrderPrice(
    pieces = pieces,
    kg = kg.toDouble(),
    lavageType = lavageType,
    config = config,
    isExpress = isExpress,
    isPremium = isPremium,
    promo = promo,
    specialItemsCostMap = specialItemsCostMap,
    transportFee = transportFee,
    gommageItemsList = gommageItemsList,
    usedFreeKgs = usedFreeKgs
)

@Composable
fun AdminStocksTab(padding: PaddingValues, viewModel: AppViewModel) {
    val consumables by viewModel.allConsumables.collectAsStateWithLifecycle()
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageStocks = hasPermission(currentUser, "GERER_STOCKS")
    
    var showDialog by remember { mutableStateOf(false) }
    
    var nameInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf("") }
    var isPublicInput by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gestion des Stocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canManageStocks) {
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter Stock")
                    }
                }
            }
        }
        
        if (consumables.isEmpty()) {
            item {
                Text("Aucun stock enregistré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(consumables) { consumable ->
            val isLowStock = consumable.quantity <= consumable.threshold
            val pendingActions by Graph.repository.getPendingActionsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
            val isPendingConsumableSync = remember(pendingActions, consumable.id) { pendingActions.any { it.payloadJson.contains(consumable.id) } }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(consumable.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isLowStock) {
                            Text("Alerte: Stock bas !", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Seuil d'alerte: ${consumable.threshold}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Visibilité: ${if(consumable.isPublic) "Public" else "Privée"}", style = MaterialTheme.typography.bodySmall)
                        
                        if (isPendingConsumableSync) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sync locale en attente",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    if (canManageStocks) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = consumable.isPublic, onCheckedChange = { viewModel.updateConsumableVisibility(consumable, it) })
                            IconButton(onClick = { 
                                 if(consumable.quantity > 0) viewModel.updateConsumableQuantity(consumable, consumable.quantity - 1) 
                             }) {
                                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text("${consumable.quantity}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { 
                                 viewModel.updateConsumableQuantity(consumable, consumable.quantity + 1) 
                             }) {
                                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteConsumable(consumable.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text("${consumable.quantity} restants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Ajouter un Produit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nom du produit (Ex: Savon, Eau de Javel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantité initiale") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it },
                        label = { Text("Seuil d'alerte (Ex: 5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPublicInput, onCheckedChange = { isPublicInput = it })
                        Text("Rendre visible au public")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = quantityInput.toIntOrNull() ?: 0
                    val t = thresholdInput.toIntOrNull() ?: 0
                    if (nameInput.isNotBlank()) {
                        viewModel.addConsumable(nameInput, q, t, isPublicInput) // We might need to update AppViewModel for isPublic, wait we don't have isPublic in addConsumable! I should just update it later if needed or modify addConsumable now
                        // Wait, addConsumable does not take isPublic. Let's fix that. Actually, I can just not pass it and update it immediately, or I can update addConsumable.
                    }
                    showDialog = false
                    nameInput = ""
                    quantityInput = ""
                    thresholdInput = ""
                }) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCatalogueTab(padding: PaddingValues, viewModel: AppViewModel) {
    val specialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val gommageItems by viewModel.allGommageItems.collectAsStateWithLifecycle()
    val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()
    val activePromos = allPromos.filter { !it.isArchived }
    
    var showSpecialDialog by remember { mutableStateOf(false) }
    var specialNameInput by remember { mutableStateOf("") }
    var specialPriceInput by remember { mutableStateOf("") }
    var editingSpecialItem by remember { mutableStateOf<SpecialItemEntity?>(null) }
    var showGommageDialog by remember { mutableStateOf(false) }
    var gommageNameInput by remember { mutableStateOf("") }
    var gommagePriceInput by remember { mutableStateOf("") }
    var editingGommageItem by remember { mutableStateOf<com.example.data.GommageItemEntity?>(null) }
    
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCodeInput by remember { mutableStateOf("") }
    var promoDescInput by remember { mutableStateOf("") }
    var promoDiscountInput by remember { mutableStateOf("") }
    var promoFreeKgInput by remember { mutableStateOf("") }
    var promoIsPermanent by remember { mutableStateOf(true) }
    var promoDurationDays by remember { mutableStateOf("") }
    var promoDurationHours by remember { mutableStateOf("") }
    var promoDiscountType by remember { mutableStateOf("PERCENTAGE") } // PERCENTAGE or FIXED_AMOUNT
    var promoTargetItems by remember { mutableStateOf("ALL") }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageCatalogue = currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN" || currentUser?.roleAdminSuper == true || hasPermission(currentUser, "GERER_CATALOGUE") || hasPermission(currentUser, "VOIR_CATALOGUE")
    val canManageGommage = currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN" || currentUser?.roleAdminSuper == true || hasPermission(currentUser, "GERER_GOMMAGE") || hasPermission(currentUser, "VOIR_CATALOGUE")
    val canManagePromos = currentUser?.role == "ADMIN" || currentUser?.role == "SUPER_ADMIN" || currentUser?.roleAdminSuper == true || hasPermission(currentUser, "GERER_PROMOS") || hasPermission(currentUser, "GERER_PROMOS_AVANCEES")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Catalogue: Articles Spéciaux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (specialItems.isNotEmpty() && canManageCatalogue) {
                    Text("(Appuyez sur l'article pour le modifier)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        if (specialItems.isEmpty()) {
            item {
                Text("Aucun article spécial configuré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(specialItems) { item ->
            Card(modifier = Modifier.fillMaxWidth().then(
                if (canManageCatalogue) {
                    Modifier.clickable {
                        editingSpecialItem = item
                        specialNameInput = item.name
                        specialPriceInput = item.price.toString()
                        showSpecialDialog = true
                    }
                } else Modifier
            ), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text("${item.price} FCFA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (canManageCatalogue) {
                        IconButton(onClick = { viewModel.deleteSpecialItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        if (canManageCatalogue) {
            item {
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
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (hasPermission(currentUser, "VOIR_CATALOGUE")) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Catalogue: Articles de Gommage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (gommageItems.isNotEmpty() && canManageGommage) {
                        Text("(Appuyez sur l'article pour le modifier)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            if (gommageItems.isEmpty()) {
                item {
                    Text("Aucun article de gommage configuré.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            items(gommageItems) { item ->
                Card(modifier = Modifier.fillMaxWidth().then(
                    if (canManageGommage) {
                        Modifier.clickable {
                            editingGommageItem = item
                            gommageNameInput = item.name
                            gommagePriceInput = item.price.toString()
                            showGommageDialog = true
                        }
                    } else Modifier
                ), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("${item.price} FCFA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (canManageGommage) {
                            IconButton(onClick = { viewModel.deleteGommageItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            if (canManageGommage) {
                item {
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
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Codes Promo & Événements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        items(activePromos) { promo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (promo.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(promo.code, fontWeight = FontWeight.Bold)
                        Text(promo.description, style = MaterialTheme.typography.bodySmall)
                        val discountText = if (promo.discountType == "PERCENTAGE") {
                            "${(promo.discountPercent * 100).toInt()}%"
                        } else {
                            "${(promo.discountPercent).toInt()} FCFA"
                        }
                        Text("Réduction: $discountText | Cible: ${promo.targetItems}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (promo.freeKg > 0) Text("Kg Offerts: ${promo.freeKg}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        Text("Type: ${if (promo.isPermanent) "Permanent" else "Temporaire (${promo.validUntil})"}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (canManagePromos) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = promo.isActive, onCheckedChange = { viewModel.togglePromoStatus(promo) })
                            IconButton(onClick = { viewModel.deletePromo(promo.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text(if(promo.isActive) "Actif" else "Inactif", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (canManagePromos) {
            item {
                Button(onClick = { showPromoDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un Code Promo / Événement")
                }
            }
        }
    }
    
    if (showSpecialDialog) {
        AlertDialog(
            onDismissRequest = {
                showSpecialDialog = false
                editingSpecialItem = null
            },
            title = { Text(if (editingSpecialItem == null) "Ajouter un Article Spécial" else "Modifier ${editingSpecialItem?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = specialNameInput,
                        onValueChange = { specialNameInput = it },
                        label = { Text("Nom de l'article") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialPriceInput,
                        onValueChange = { specialPriceInput = it },
                        label = { Text("Prix unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = specialPriceInput.toFloatOrNull() ?: 0f
                    if (specialNameInput.isNotBlank()) {
                        if (editingSpecialItem == null) {
                            viewModel.addSpecialItem(specialNameInput, p)
                        } else {
                            viewModel.updateSpecialItem(editingSpecialItem!!.copy(name = specialNameInput, price = p))
                        }
                    }
                    showSpecialDialog = false
                    editingSpecialItem = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSpecialDialog = false 
                    editingSpecialItem = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showGommageDialog) {
        AlertDialog(
            onDismissRequest = {
                showGommageDialog = false
                editingGommageItem = null
            },
            title = { Text(if (editingGommageItem == null) "Ajouter un Article de Gommage" else "Modifier ${editingGommageItem?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gommageNameInput,
                        onValueChange = { gommageNameInput = it },
                        label = { Text("Nom de l'article de gommage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gommagePriceInput,
                        onValueChange = { gommagePriceInput = it },
                        label = { Text("Prix unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = gommagePriceInput.toFloatOrNull() ?: 0f
                    if (gommageNameInput.isNotBlank()) {
                        if (editingGommageItem == null) {
                            viewModel.addGommageItem(gommageNameInput, p)
                        } else {
                            viewModel.updateGommageItem(editingGommageItem!!.copy(name = gommageNameInput, price = p))
                        }
                    }
                    showGommageDialog = false
                    editingGommageItem = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showGommageDialog = false 
                    editingGommageItem = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            title = { Text("Créer Code Promo / Événement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it.uppercase() },
                        label = { Text("Code (Ex: TABASKI20)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = promoDescInput,
                        onValueChange = { promoDescInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = promoIsPermanent, onCheckedChange = { promoIsPermanent = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (promoIsPermanent) "Promo Permanente" else "Promo Temporaire", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (!promoIsPermanent) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = promoDurationDays,
                                onValueChange = { promoDurationDays = it },
                                label = { Text("Jours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = promoDurationHours,
                                onValueChange = { promoDurationHours = it },
                                label = { Text("Heures") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    var expandedTarget by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTarget, onExpandedChange = { expandedTarget = !expandedTarget }) {
                        OutlinedTextField(
                            value = promoTargetItems,
                            onValueChange = { promoTargetItems = it },
                            label = { Text("Cibles (Toutes, Articles, Services...)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) }
                        )
                        ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            listOf("ALL", "COMMANDE_COMPLETE", "LAVAGE_SIMPLE", "LAVAGE_SECHAGE_PLIAGE", "LAVAGE_SECHAGE_REPASSAGE_PLIAGE", "GOMMAGE").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option else "$promoTargetItems,$option"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            specialItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Spécial: ${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                            gommageItems.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Gommage: ${option.name}") },
                                    onClick = { 
                                        promoTargetItems = if (promoTargetItems == "ALL" || promoTargetItems.isEmpty()) option.name else "$promoTargetItems,${option.name}"
                                        expandedTarget = false 
                                    }
                                )
                            }
                        }
                    }
                    
                    Text("Type de réduction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = promoDiscountType == "PERCENTAGE", onClick = { promoDiscountType = "PERCENTAGE" })
                        Text("Pourcentage", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = promoDiscountType == "FIXED_AMOUNT", onClick = { promoDiscountType = "FIXED_AMOUNT" })
                        Text("Montant Fixe", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = promoDiscountInput,
                        onValueChange = { promoDiscountInput = it },
                        label = { Text(if (promoDiscountType == "PERCENTAGE") "Réduction en % (Ex: 20)" else "Réduction en FCFA (Ex: 1000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Offre spéciale", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = promoFreeKgInput,
                        onValueChange = { promoFreeKgInput = it },
                        label = { Text("Kg Offerts (Optionnel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val discount = (promoDiscountInput.toFloatOrNull() ?: 0f) / (if (promoDiscountType == "PERCENTAGE") 100f else 1f)
                    val freeKg = promoFreeKgInput.toFloatOrNull() ?: 0f
                    val durationDays = promoDurationDays.toLongOrNull() ?: 0L
                    val durationHoursAmount = promoDurationHours.toLongOrNull() ?: 0L
                    val durationMs = (durationDays * 24 * 60 * 60 * 1000L) + (durationHoursAmount * 60 * 60 * 1000L)
                    val validUntil = if (promoIsPermanent || durationMs == 0L) null else System.currentTimeMillis() + durationMs
                    
                    if (promoCodeInput.isNotBlank()) {
                        viewModel.addPromo(
                            code = promoCodeInput, 
                            description = promoDescInput, 
                            discountPercent = discount, 
                            freeKg = freeKg, 
                            minOrderValue = 0f,
                            validUntil = validUntil,
                            isPermanent = promoIsPermanent,
                            targetItems = promoTargetItems,
                            discountType = promoDiscountType
                        )
                    }
                    showPromoDialog = false
                    promoCodeInput = ""
                    promoDescInput = ""
                    promoDiscountInput = ""
                    promoFreeKgInput = ""
                    promoDurationDays = ""
                    promoDurationHours = ""
                    promoTargetItems = "ALL"
                    promoDiscountType = "PERCENTAGE"
                    promoIsPermanent = true
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allAntiFraudRules by viewModel.allAntiFraudRules.collectAsStateWithLifecycle()
    val activeAntiFraudRules = allAntiFraudRules.filter { !it.isArchived }
    
    var showCreateAntiFraudDialog by remember { mutableStateOf(false) }
    var afNameInput by remember { mutableStateOf("") }
    var afTriggerInput by remember { mutableStateOf("") }
    var afThresholdInput by remember { mutableStateOf("3") }
    var afDurationInput by remember { mutableStateOf("24") }
    var afActionInput by remember { mutableStateOf("BAN") }

    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    
    val conf = config!!
    var baseTransportFee by remember { mutableStateOf(conf.baseTransportFee.toString()) }
    var pricePerKm by remember { mutableStateOf(conf.pricePerKm.toString()) }
    var categories by remember { mutableStateOf(conf.categories) }
    var newCategoryInput by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf(conf.businessName) }
    var contactPhone by remember { mutableStateOf(conf.contactPhone) }
    var contactEmail by remember { mutableStateOf(conf.contactEmail) }
    var openingHours by remember { mutableStateOf(conf.openingHours) }
    var closingHours by remember { mutableStateOf(conf.closingHours) }
    var openingDays by remember { mutableStateOf(conf.openingDays) }
    var termsLink by remember { mutableStateOf(conf.termsAndConditionsLink) }
    var isLoyaltyProgramEnabled by remember { mutableStateOf(conf.isLoyaltyProgramEnabled) }
    var loyaltyKgThreshold by remember { mutableStateOf(conf.loyaltyKgThreshold.toString()) }
    var loyaltyKgReward by remember { mutableStateOf(conf.loyaltyKgReward.toString()) }
    var loyaltyMaxFreeKgs by remember { mutableStateOf(conf.loyaltyMaxFreeKgs.toString()) }
    var loyaltyExpirationDays by remember { mutableStateOf(conf.loyaltyExpirationDays.toString()) }
    var isLoyaltyEnabled by remember { mutableStateOf(conf.isLoyaltyEnabled) }
    var loyaltyDurationDays by remember { mutableStateOf("") }
    var isLoyaltyPermanent by remember { mutableStateOf(conf.loyaltyValidUntil == null) }
    
    val pendingActions by Graph.repository.getPendingActionsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val isPendingConfigSync = remember(pendingActions) {
        pendingActions.any { it.actionType == "UPDATE_CONFIG" }
    }
    
    val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    val timeOptions = (0..23).flatMap { listOf(String.format("%02d:00", it), String.format("%02d:30", it)) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Paramètres Généraux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tarification & Livraison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = baseTransportFee,
                        onValueChange = { baseTransportFee = it },
                        label = { Text("Frais de transport de base (FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pricePerKm,
                        onValueChange = { pricePerKm = it },
                        label = { Text("Prix par Km supplémentaire (FCFA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Système & Contacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nom de l'entreprise") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Téléphone de contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Email de contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var expandedDays by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedDays, onExpandedChange = { expandedDays = !expandedDays }) {
                        OutlinedTextField(
                            value = openingDays,
                            onValueChange = { openingDays = it },
                            label = { Text("Jours d'ouverture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDays) }
                        )
                        ExposedDropdownMenu(expanded = expandedDays, onDismissRequest = { expandedDays = false }) {
                            daysOfWeek.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = {
                                        openingDays = if (openingDays.isEmpty()) day else "$openingDays,$day"
                                        expandedDays = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedOpenTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedOpenTime, onExpandedChange = { expandedOpenTime = !expandedOpenTime }) {
                        OutlinedTextField(
                            value = openingHours,
                            onValueChange = { openingHours = it },
                            label = { Text("Heure d'ouverture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOpenTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedOpenTime, onDismissRequest = { expandedOpenTime = false }) {
                            timeOptions.forEach { time ->
                                DropdownMenuItem(
                                    text = { Text(time) },
                                    onClick = { openingHours = time; expandedOpenTime = false }
                                )
                            }
                        }
                    }

                    var expandedCloseTime by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCloseTime, onExpandedChange = { expandedCloseTime = !expandedCloseTime }) {
                        OutlinedTextField(
                            value = closingHours,
                            onValueChange = { closingHours = it },
                            label = { Text("Heure de fermeture") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCloseTime) }
                        )
                        ExposedDropdownMenu(expanded = expandedCloseTime, onDismissRequest = { expandedCloseTime = false }) {
                            timeOptions.forEach { time ->
                                DropdownMenuItem(
                                    text = { Text(time) },
                                    onClick = { closingHours = time; expandedCloseTime = false }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = termsLink,
                        onValueChange = { termsLink = it },
                        label = { Text("Lien des CGU") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catégories Pré-enregistrées", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    var expandedCats by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedCats, onExpandedChange = { expandedCats = !expandedCats }) {
                        OutlinedTextField(
                            value = "Voir ou selectionner les catégories",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCats) }
                        )
                        ExposedDropdownMenu(expanded = expandedCats, onDismissRequest = { expandedCats = false }) {
                            categories.split(",").filter { it.isNotBlank() }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { expandedCats = false }
                                )
                            }
                        }
                    }
                    
                    Text("Catégories actuelles: $categories", style = MaterialTheme.typography.bodySmall)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            label = { Text("Nouvelle catégorie") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newCategoryInput.isNotBlank()) {
                                categories = if (categories.isEmpty()) newCategoryInput else "$categories,$newCategoryInput"
                                newCategoryInput = ""
                            }
                        }) {
                            Text("Ajouter")
                        }
                    }
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val canManageAntiFraud = hasPermission(currentUser, "GERER_ANTI_FRAUDE")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Anti-Fraude globale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (canManageAntiFraud) {
                            Button(onClick = { showCreateAntiFraudDialog = true }) {
                                Text("Nouvelle Anti-Fraude")
                            }
                        }
                    }
                    
                    if (allAntiFraudRules.isEmpty()) {
                        Text("Aucune règle anti-fraude.", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    allAntiFraudRules.forEach { af ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(af.name, fontWeight = FontWeight.Bold)
                                    Text("${af.triggerEvent} - Limite: ${af.threshold}", style = MaterialTheme.typography.bodySmall)
                                    Text("Action: ${af.action} pour ${af.durationHours}h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = af.isActive, 
                                        onCheckedChange = { viewModel.toggleAntiFraudRuleStatus(af) },
                                        enabled = canManageAntiFraud
                                    )
                                    if (canManageAntiFraud) {
                                        IconButton(onClick = { viewModel.deleteAntiFraudRule(af.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        val canManageParametres = hasPermission(currentUser, "GERER_PARAMETRES")
        val canManageFidelity = currentUser?.role == "SUPER_ADMIN" || currentUser?.permissions?.contains("ALL") == true || currentUser?.permissions?.contains("GERER_FIDELITE") == true || canManageParametres
        
        if (canManageFidelity) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Fidélité",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Programme de Fidélité",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isPendingConfigSync) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Sync locale en attente",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "Configurez les récompenses accordées aux clients fidèles en fonction du poids de linge déposé.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Activer le programme de fidélité",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Attribue automatiquement des points lors du traitement des commandes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isLoyaltyProgramEnabled,
                                onCheckedChange = {
                                    isLoyaltyProgramEnabled = it
                                    isLoyaltyEnabled = it
                                },
                                enabled = canManageParametres
                            )
                        }

                        if (isLoyaltyProgramEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            OutlinedTextField(
                                value = loyaltyKgThreshold,
                                onValueChange = { loyaltyKgThreshold = it },
                                label = { Text("Seuil en Kg requis pour récompense (Kilo Gratuit)") },
                                supportingText = { Text("Nombre de Kg de linge lavés nécessaires (Ex: 10.0)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canManageParametres,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = loyaltyKgReward,
                                onValueChange = { loyaltyKgReward = it },
                                label = { Text("Récompense offerte en Kilos Gratuits (Kg)") },
                                supportingText = { Text("Nombre de kilos gratuits offerts par palier (Ex: 1.0)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canManageParametres,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = loyaltyMaxFreeKgs,
                                onValueChange = { loyaltyMaxFreeKgs = it },
                                label = { Text("Plafond Max Kilos Gratuits accumulables (Kg)") },
                                supportingText = { Text("Limite maximale de solde gratuit par client (Ex: 10.0)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canManageParametres,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = loyaltyExpirationDays,
                                onValueChange = { loyaltyExpirationDays = it },
                                label = { Text("Validité des Kilos Gratuits (Jours)") },
                                supportingText = { Text("Durée de validité des récompenses en jours (Ex: 90)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canManageParametres,
                                singleLine = true
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isLoyaltyPermanent,
                                    onCheckedChange = { isLoyaltyPermanent = it },
                                    enabled = canManageParametres
                                )
                                Text("Programme permanent (sans date d'expiration)")
                            }

                            if (!isLoyaltyPermanent) {
                                OutlinedTextField(
                                    value = loyaltyDurationDays,
                                    onValueChange = { loyaltyDurationDays = it },
                                    label = { Text("Durée de validité (jours à partir de maintenant)") },
                                    supportingText = { Text("Ex: 30 pour une campagne fidélité de 30 jours") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = canManageParametres,
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        } // end if canManageFidelity
        
        if (canManageParametres) {
            item {
                Button(
                    onClick = {
                        val updatedLoyaltyUntil = if (!isLoyaltyProgramEnabled) null else if (isLoyaltyPermanent) null else (System.currentTimeMillis() + (loyaltyDurationDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000)
                        viewModel.updateConfig(
                            conf.copy(
                                isLoyaltyProgramEnabled = isLoyaltyProgramEnabled,
                                isLoyaltyEnabled = isLoyaltyProgramEnabled,
                                loyaltyKgThreshold = loyaltyKgThreshold.toDoubleOrNull() ?: conf.loyaltyKgThreshold,
                                loyaltyKgReward = loyaltyKgReward.toDoubleOrNull() ?: conf.loyaltyKgReward,
                                loyaltyMaxFreeKgs = loyaltyMaxFreeKgs.toDoubleOrNull() ?: conf.loyaltyMaxFreeKgs,
                                loyaltyExpirationDays = loyaltyExpirationDays.toIntOrNull() ?: conf.loyaltyExpirationDays,
                                loyaltyValidUntil = updatedLoyaltyUntil,
                                baseTransportFee = baseTransportFee.toFloatOrNull() ?: conf.baseTransportFee,
                                pricePerKm = pricePerKm.toFloatOrNull() ?: conf.pricePerKm,
                                categories = categories,
                                businessName = businessName,
                                contactPhone = contactPhone,
                                contactEmail = contactEmail,
                                openingHours = openingHours,
                                closingHours = closingHours,
                                openingDays = openingDays,
                                termsAndConditionsLink = termsLink,
                                tarifBaseTransort = (baseTransportFee.toDoubleOrNull() ?: conf.tarifBaseTransort),
                                prixParKilo = (pricePerKm.toDoubleOrNull() ?: conf.prixParKilo),
                                nomEntreprise = businessName,
                                numeroEntreprise = contactPhone,
                                emailEntreprise = contactEmail,
                                openDays = openingDays,
                                openHours = openingHours,
                                closeHours = closingHours,
                                linkCGU = termsLink
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer les modifications")
                }
            }
        }
    }
    
    if (showCreateAntiFraudDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAntiFraudDialog = false },
            title = { Text("Nouvelle règle Anti-Fraude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = afNameInput,
                        onValueChange = { afNameInput = it },
                        label = { Text("Nom de la règle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    var expandedTrig by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTrig, onExpandedChange = { expandedTrig = !expandedTrig }) {
                        OutlinedTextField(
                            value = afTriggerInput,
                            onValueChange = { afTriggerInput = it },
                            label = { Text("Événement") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTrig) }
                        )
                        ExposedDropdownMenu(expanded = expandedTrig, onDismissRequest = { expandedTrig = false }) {
                            listOf("COMMANDES_ANNULEES", "PAIEMENTS_ECHOUES", "FAUSSES_COORDONNEES").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { afTriggerInput = option; expandedTrig = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = afThresholdInput,
                        onValueChange = { afThresholdInput = it },
                        label = { Text("Seuil") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = afDurationInput,
                        onValueChange = { afDurationInput = it },
                        label = { Text("Durée (Heures)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = afActionInput,
                        onValueChange = { afActionInput = it },
                        label = { Text("Action (BAN/SUSPEND/WARN)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addAntiFraudRule(
                        name = afNameInput,
                        triggerEvent = afTriggerInput,
                        threshold = afThresholdInput.toIntOrNull() ?: 3,
                        durationHours = afDurationInput.toIntOrNull() ?: 24,
                        action = afActionInput
                    )
                    showCreateAntiFraudDialog = false
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAntiFraudDialog = false }) { Text("Annuler") }
            }
        )
    }
}@Composable
fun AdminStaffTab(padding: PaddingValues, viewModel: AppViewModel) {
    val allAdmins by viewModel.allAdmins.collectAsStateWithLifecycle()
    val archivedAdmins by viewModel.archivedAdmins.collectAsStateWithLifecycle()
    var showTrash by remember { mutableStateOf(false) }
    val activeAdmins = if (showTrash) archivedAdmins else allAdmins
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canManageStaff = hasPermission(currentUser, "GERER_STAFF")
    
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var editingAdminProfile by remember { mutableStateOf<UserEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gestion du Staff", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    if (hasPermission(currentUser, "VOIR_CORBEILLE")) {
                        Button(onClick = { showTrash = !showTrash }, colors = ButtonDefaults.buttonColors(containerColor = if (showTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                            Text(if (showTrash) "Actifs" else "Corbeille")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (canManageStaff) {
                        Button(onClick = { showCreateAdminDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouveau")
                        }
                    }
                }
            }
        }
        
        items(activeAdmins) { admin ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${admin.prenom} ${admin.nom} (${admin.role})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(admin.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Permissions: ${admin.permissions}", style = MaterialTheme.typography.labelSmall)
                    
                    if (canManageStaff && admin.role != "SUPER_ADMIN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Permissions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        var currentPerms by remember { mutableStateOf(if (admin.permissions == "ALL") AVAILABLE_PERMISSIONS.toSet() else admin.permissions.split(",").map { it.trim() }.toSet()) }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AVAILABLE_PERMISSIONS.chunked(2).forEach { rowPerms ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowPerms.forEach { perm ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = currentPerms.contains(perm),
                                                onCheckedChange = { isChecked ->
                                                    val newPerms = currentPerms.toMutableSet()
                                                    if (isChecked) newPerms.add(perm) else newPerms.remove(perm)
                                                    currentPerms = newPerms
                                                }
                                            )
                                            Text(perm.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!showTrash) {
                                Button(onClick = {
                                    viewModel.updateUser(admin.copy(permissions = currentPerms.joinToString(",")))
                                }) {
                                    Text("Sauvegarder")
                                }
                                Button(onClick = {
                                    editingAdminProfile = admin
                                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                    Text("Éditer Profil")
                                }
                                TextButton(onClick = { viewModel.archiveUser(admin.id) }) {
                                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Button(onClick = { viewModel.restoreUser(admin.id) }) {
                                    Text("Restaurer")
                                }
                                TextButton(onClick = { viewModel.hardDeleteUser(admin.id) }) {
                                    Text("Supprimer définitivement", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAdminDialog) {
        var nom by remember { mutableStateOf("") }
        var prenom by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("ADMIN") }
        
        var currentPerms by remember { mutableStateOf(setOf("VOIR_COMMANDES", "VOIR_CLIENTS")) }

        AlertDialog(
            onDismissRequest = { showCreateAdminDialog = false },
            title = { Text("Nouveau membre du Staff") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }) }
                    item { OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }) }
                    item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }) }
                    item { OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe temporaire") }) }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Rôle: ", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ADMIN")
                            Switch(
                                checked = role == "SUPER_ADMIN",
                                onCheckedChange = { isSuper -> role = if (isSuper) "SUPER_ADMIN" else "ADMIN" }
                            )
                            Text("SUPER_ADMIN")
                        }
                    }

                    if (role == "ADMIN") {
                        item { Text("Permissions:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                        items(AVAILABLE_PERMISSIONS.chunked(2)) { rowPerms ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowPerms.forEach { perm ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = currentPerms.contains(perm),
                                                onCheckedChange = { isChecked ->
                                                    val newPerms = currentPerms.toMutableSet()
                                                    if (isChecked) newPerms.add(perm) else newPerms.remove(perm)
                                                    currentPerms = newPerms
                                                }
                                            )
                                            Text(perm.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newUser = UserEntity(
                        role = role,
                        nom = nom,
                        prenom = prenom,
                        email = email,
                        telephone = "Non renseigné",
                        motDePasse = password,
                        permissions = if (role == "ADMIN") currentPerms.joinToString(",") else "ALL",
                        prenomAdmin = prenom,
                        nomAdmin = nom,
                        mailAdmin = email,
                        motDePasseAdmin = password,
                        roleAdminSuper = (role == "SUPER_ADMIN"),
                        prenomUser = prenom,
                        nomUser = nom,
                        mailUser = email,
                        motDePasseUser = password
                    )
                    viewModel.insertUser(newUser)
                    showCreateAdminDialog = false
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (editingAdminProfile != null) {
        EditClientProfileDialog(client = editingAdminProfile!!, viewModel = viewModel, onDismiss = { editingAdminProfile = null })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRulesTab(padding: PaddingValues, viewModel: AppViewModel) {
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val activeRules = allRules.filter { !it.isArchived }
    
    var showCreateRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }
    
    var ruleNameInput by remember { mutableStateOf("") }
    var ruleTriggerInput by remember { mutableStateOf("") }
    var ruleActionInput by remember { mutableStateOf("") }
    var ruleActionValueInput by remember { mutableStateOf("") }
    var ruleConstraintsInput by remember { mutableStateOf("") }
    var isPermanentInput by remember { mutableStateOf(true) }
    var validUntilInput by remember { mutableStateOf<Long?>(null) }
    
    val triggersList = listOf("COMMANDE_CRÉÉE", "UTILISATEUR_INSCRIT", "MONTANT_MIN_ATTEINT", "NOUVELLE_CATÉGORIE", "MODIFICATION_PROMO", "STOCK_FAIBLE", "AVIS_AJOUTÉ", "PAIEMENT_ÉCHOUÉ", "OR", "AND", "XOR")
    val actionsList = listOf("APPLIQUER_RÉDUCTION", "AJOUTER_KG_GRATUITS", "NOTIFIER_ADMIN", "BLOQUER_UTILISATEUR", "ENVOYER_EMAIL", "ACCEPTER_COMMANDE_AUTO", "OR", "AND", "XOR")
    val constraintsList = listOf("SEULEMENT_NOUVEAUX_UTILISATEURS", "MONTANT_MIN=5000", "A_PREMIUM", "SANS_PROMO", "OR", "AND", "XOR", "ÉGAL", "DIFFÉRENT")
    
    val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()
    val allSpecialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()
    val activeSpecialItems = allSpecialItems.filter { !it.isArchived }
    val dynamicTargets = (allPromos.map { it.code } + allSpecialItems.map { it.name } + listOf("Tous les Utilisateurs", "Nouvelles Commandes", "Catégorie: Vêtements", "Catégorie: Tapis", "Montant: 1000", "Pourcentage: 10%")).distinct()

    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Règles Automatiques", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { 
                    editingRule = null
                    ruleNameInput = ""
                    ruleTriggerInput = ""
                    ruleActionInput = ""
                    ruleActionValueInput = ""
                    ruleConstraintsInput = ""
                    isPermanentInput = true
                    validUntilInput = null
                    showCreateRuleDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouvelle Règle")
                }
            }
        }
        
        if (allRules.isEmpty()) {
            item {
                Text("Aucune règle configurée.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        items(activeRules) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    editingRule = rule
                    ruleNameInput = rule.name
                    ruleTriggerInput = rule.triggerCondition
                    ruleActionInput = rule.actionType
                    ruleActionValueInput = rule.actionValue
                    ruleConstraintsInput = rule.constraints
                    isPermanentInput = rule.isPermanent
                    validUntilInput = rule.validUntil
                    showCreateRuleDialog = true
                },
                colors = CardDefaults.cardColors(containerColor = if (rule.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Déclencheur: ${rule.triggerCondition}", style = MaterialTheme.typography.bodySmall)
                        Text("Action: ${rule.actionType} (${rule.actionValue})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (rule.constraints.isNotBlank()) {
                            Text("Contraintes: ${rule.constraints}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Text("Durée: ${if (rule.isPermanent) "Permanent" else "Temporaire"}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = rule.isActive, onCheckedChange = { viewModel.toggleRuleStatus(rule) })
                        IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateRuleDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRuleDialog = false },
            title = { Text(if (editingRule == null) "Créer une nouvelle règle" else "Modifier la règle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = ruleNameInput,
                        onValueChange = { ruleNameInput = it },
                        label = { Text("Nom de la règle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var expandedTrigger by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTrigger, onExpandedChange = { expandedTrigger = !expandedTrigger }) {
                        OutlinedTextField(
                            value = ruleTriggerInput,
                            onValueChange = { ruleTriggerInput = it },
                            label = { Text("Déclencheur (OR/AND/XOR supporté)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTrigger) }
                        )
                        ExposedDropdownMenu(expanded = expandedTrigger, onDismissRequest = { expandedTrigger = false }) {
                            triggersList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleTriggerInput = if (ruleTriggerInput.isEmpty()) option else "$ruleTriggerInput $option "
                                        expandedTrigger = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedAction by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedAction, onExpandedChange = { expandedAction = !expandedAction }) {
                        OutlinedTextField(
                            value = ruleActionInput,
                            onValueChange = { ruleActionInput = it },
                            label = { Text("Action") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAction) }
                        )
                        ExposedDropdownMenu(expanded = expandedAction, onDismissRequest = { expandedAction = false }) {
                            actionsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleActionInput = if (ruleActionInput.isEmpty()) option else "$ruleActionInput $option "
                                        expandedAction = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedActionValue by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedActionValue, onExpandedChange = { expandedActionValue = !expandedActionValue }) {
                        OutlinedTextField(
                            value = ruleActionValueInput,
                            onValueChange = { ruleActionValueInput = it },
                            label = { Text("Valeur de l'action (Valeurs/Cibles)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActionValue) }
                        )
                        ExposedDropdownMenu(expanded = expandedActionValue, onDismissRequest = { expandedActionValue = false }) {
                            dynamicTargets.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleActionValueInput = if (ruleActionValueInput.isEmpty()) option else "$ruleActionValueInput $option "
                                        expandedActionValue = false
                                    }
                                )
                            }
                        }
                    }
                    
                    var expandedConstraint by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedConstraint, onExpandedChange = { expandedConstraint = !expandedConstraint }) {
                        OutlinedTextField(
                            value = ruleConstraintsInput,
                            onValueChange = { ruleConstraintsInput = it },
                            label = { Text("Contraintes (Optionnel)") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConstraint) }
                        )
                        ExposedDropdownMenu(expanded = expandedConstraint, onDismissRequest = { expandedConstraint = false }) {
                            constraintsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        ruleConstraintsInput = if (ruleConstraintsInput.isEmpty()) option else "$ruleConstraintsInput $option "
                                        expandedConstraint = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPermanentInput, onCheckedChange = { isPermanentInput = it })
                        Text("Règle Permanente")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ruleNameInput.isNotBlank() && ruleTriggerInput.isNotBlank() && ruleActionInput.isNotBlank()) {
                        if (editingRule != null) {
                            viewModel.updateRuleInfo(editingRule!!.copy(
                                name = ruleNameInput,
                                triggerCondition = ruleTriggerInput,
                                actionType = ruleActionInput,
                                actionValue = ruleActionValueInput,
                                constraints = ruleConstraintsInput,
                                isPermanent = isPermanentInput,
                                validUntil = validUntilInput
                            ))
                        } else {
                            viewModel.addRule(
                                name = ruleNameInput,
                                triggerCondition = ruleTriggerInput,
                                actionType = ruleActionInput,
                                actionValue = ruleActionValueInput,
                                constraints = ruleConstraintsInput,
                                isPermanent = isPermanentInput,
                                validUntil = validUntilInput
                            )
                        }
                    }
                    showCreateRuleDialog = false
                }) {
                    Text(if (editingRule == null) "Créer" else "Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRuleDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
@Composable
fun AdminGlobalTrashTab(padding: PaddingValues, viewModel: AppViewModel) {
    val archivedOrders by viewModel.archivedOrders.collectAsStateWithLifecycle()
    val archivedPromos by viewModel.archivedPromos.collectAsStateWithLifecycle()
    val archivedSpecialItems by viewModel.archivedSpecialItems.collectAsStateWithLifecycle()
    val archivedGommageItems by viewModel.archivedGommageItems.collectAsStateWithLifecycle()
    val archivedConsumables by viewModel.archivedConsumables.collectAsStateWithLifecycle()
    val archivedRules by viewModel.archivedRules.collectAsStateWithLifecycle()
    val archivedAntiFraudRules by viewModel.archivedAntiFraudRules.collectAsStateWithLifecycle()
    val archivedClients by viewModel.archivedClients.collectAsStateWithLifecycle()
    val archivedAdmins by viewModel.archivedAdmins.collectAsStateWithLifecycle()
    val allGlobalTrash by viewModel.allGlobalTrash.collectAsStateWithLifecycle()
    
    val archivedUsers = archivedClients + archivedAdmins
    
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Corbeille Globale", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Utilisateurs & Administrateurs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedUsers) { user ->
                TrashItemCard(title = "${user.prenom} ${user.nom} (${user.role})", details = "Email: ${user.email}\nTéléphone: ${user.telephone}", onRestore = { viewModel.restoreUser(user.id) }, onHardDelete = { viewModel.hardDeleteUser(user.id) })
            }
            item { Text("Commandes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedOrders) { order ->
                TrashItemCard(title = "Commande #${order.id} (${order.status})", details = "Client ID: ${order.userId}\nArticles: ${order.pieces} pièces, ${order.kg} Kg\nPrix Final: ${order.prixFinal.toInt()} FCFA", onRestore = { viewModel.restoreOrder(order.id) }, onHardDelete = { viewModel.hardDeleteOrder(order.id) })
            }
            item { Text("Articles du Catalogue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedSpecialItems) { item ->
                TrashItemCard(title = "${item.name} (Spécial)", details = "Prix: ${item.price} FCFA", onRestore = { viewModel.restoreSpecialItem(item.id) }, onHardDelete = { viewModel.hardDeleteSpecialItem(item.id) })
            }
            items(archivedGommageItems) { item ->
                TrashItemCard(title = "${item.name} (Gommage)", details = "Prix: ${item.price} FCFA", onRestore = { viewModel.restoreGommageItem(item.id) }, onHardDelete = { viewModel.hardDeleteGommageItem(item.id) })
            }
            item { Text("Consommables (Stocks)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedConsumables) { item ->
                TrashItemCard(title = item.name, onRestore = { viewModel.restoreConsumable(item.id) }, onHardDelete = { viewModel.hardDeleteConsumable(item.id) })
            }
            item { Text("Codes Promo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedPromos) { item ->
                TrashItemCard(title = item.code, details = "Réduction: ${item.discountPercent}${if(item.discountType == "PERCENTAGE") "%" else " FCFA"}\nValable: ${if(item.isPermanent) "Toujours" else "Temporaire"}", onRestore = { viewModel.restorePromo(item.id) }, onHardDelete = { viewModel.hardDeletePromo(item.id) })
            }
            item { Text("Règles Automatiques", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedRules) { item ->
                TrashItemCard(title = item.name, onRestore = { viewModel.restoreRule(item.id) }, onHardDelete = { viewModel.hardDeleteRule(item.id) })
            }
            item { Text("Règles Anti-Fraude", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(archivedAntiFraudRules) { item ->
                TrashItemCard(title = item.name, onRestore = { viewModel.restoreAntiFraudRule(item.id) }, onHardDelete = { viewModel.hardDeleteAntiFraudRule(item.id) })
            }
        }
    }
}

@Composable
fun TrashItemCard(title: String, details: String = "", onRestore: () -> Unit, onHardDelete: () -> Unit) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRestoreConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (expanded && details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(details, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(onClick = { showRestoreConfirm = true }) { Icon(Icons.Default.Refresh, contentDescription = "Restaurer", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer définitivement", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurer") },
            text = { Text("Voulez-vous restaurer '$title' ?") },
            confirmButton = {
                TextButton(onClick = { onRestore(); showRestoreConfirm = false }) { Text("Oui") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Non") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Suppression Définitive") },
            text = { Text("Voulez-vous supprimer DÉFINITIVEMENT '$title' ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { onHardDelete(); showDeleteConfirm = false }) { Text("Oui, Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPublicitesTab(padding: PaddingValues, viewModel: AppViewModel) {
    val announcements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.example.data.AnnouncementEntity?>(null) }
    
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var isVisibleInput by remember { mutableStateOf(true) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Gestion des Publicités & Annonces", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { 
                editingItem = null
                titleInput = ""
                messageInput = ""
                imageUrlInput = ""
                isVisibleInput = true
                showDialog = true 
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouvelle Publicité")
            }
        }
        
        items(announcements) { ann ->
            Card(modifier = Modifier.fillMaxWidth().clickable {
                editingItem = ann
                titleInput = ann.title
                messageInput = ann.message
                imageUrlInput = ann.imageUrl
                isVisibleInput = ann.isVisible
                showDialog = true
            }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ann.title, fontWeight = FontWeight.Bold)
                        Text(ann.message, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        if (!ann.isVisible) {
                            Text("Masqué", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteAnnouncement(ann.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingItem == null) "Nouvelle Publicité" else "Modifier Publicité") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Titre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("URL de l'image (Optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isVisibleInput, onCheckedChange = { isVisibleInput = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visible aux clients")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                        if (editingItem == null) {
                            viewModel.addAnnouncement(titleInput, messageInput, imageUrlInput)
                        } else {
                            viewModel.updateAnnouncement(editingItem!!.copy(title = titleInput, message = messageInput, imageUrl = imageUrlInput, isVisible = isVisibleInput))
                        }
                    }
                    showDialog = false
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLogsTab(padding: PaddingValues, viewModel: AppViewModel) {
    val allLogs by viewModel.allActivityLogs.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val allAdmins by viewModel.allAdmins.collectAsStateWithLifecycle()
    val allUsers = (allClients + allAdmins).distinctBy { it.id }

    var searchQuery by remember { mutableStateOf("") }
    var selectedActionFilter by remember { mutableStateOf("TOUS") }
    var selectedLogForDialog by remember { mutableStateOf<com.example.data.ActivityLogEntity?>(null) }

    val distinctActions = listOf("TOUS") + allLogs.map { it.action }.distinct().sorted()

    val filteredLogs = allLogs.filter { log ->
        val matchesSearch = log.description.contains(searchQuery, ignoreCase = true) ||
                log.action.contains(searchQuery, ignoreCase = true) ||
                log.userId.contains(searchQuery, ignoreCase = true) ||
                log.actorId?.contains(searchQuery, ignoreCase = true) == true
        
        val matchesFilter = selectedActionFilter == "TOUS" || log.action == selectedActionFilter
        
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Majestic Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Journal d'activité d'Administration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Surveillez et auditez les actions des administrateurs et du personnel de Bella Pressing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Actions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${allLogs.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aujourd'hui",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            val todayStart = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                            }.timeInMillis
                            val logsToday = allLogs.filter { it.timestamp >= todayStart }.size
                            Text(
                                text = "$logsToday",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Search and Filters layout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Rechercher dans les logs...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filtrer par type: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Button(onClick = { expanded = true }) {
                                    Text(selectedActionFilter)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    distinctActions.forEach { action ->
                                        DropdownMenuItem(
                                            text = { Text(action) },
                                            onClick = {
                                                selectedActionFilter = action
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (searchQuery.isNotEmpty() || selectedActionFilter != "TOUS") {
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedActionFilter = "TOUS"
                            }) {
                                Text("Réinitialiser")
                            }
                        }
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun log d'activité ne correspond aux critères.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Timeline list
        items(filteredLogs) { log ->
            val actorUser = allUsers.find { it.id == log.actorId } ?: allUsers.find { it.id == log.userId && log.actorId == null }
            val actorName = if (actorUser != null) "${actorUser.prenom} ${actorUser.nom} (${actorUser.role})" else "Système / Auto"
            
            val targetUser = allUsers.find { it.id == log.userId }
            val targetName = if (targetUser != null) "${targetUser.prenom} ${targetUser.nom}" else ""
            
            val formattedTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date(log.timestamp))

            // Action Badge Styling
            val badgeColor = when {
                log.action.contains("BAN") || log.action.contains("DELETE") -> MaterialTheme.colorScheme.errorContainer
                log.action.contains("CREATE") || log.action.contains("ADD") -> MaterialTheme.colorScheme.primaryContainer
                log.action.contains("UPDATE") -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
            val badgeTextColor = when {
                log.action.contains("BAN") || log.action.contains("DELETE") -> MaterialTheme.colorScheme.onErrorContainer
                log.action.contains("CREATE") || log.action.contains("ADD") -> MaterialTheme.colorScheme.onPrimaryContainer
                log.action.contains("UPDATE") -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            }
            val badgeIcon = when {
                log.action.contains("BAN") -> Icons.Default.Warning
                log.action.contains("DELETE") -> Icons.Default.Delete
                log.action.contains("CREATE") || log.action.contains("ADD") -> Icons.Default.Add
                else -> Icons.Default.Info
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLogForDialog = log },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Badge circle icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(badgeColor, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.action,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                                modifier = Modifier
                                    .background(badgeColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Par: $actorName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (targetName.isNotEmpty()) {
                                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Cible: $targetName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedLogForDialog != null) {
        val log = selectedLogForDialog!!
        val actorUser = allUsers.find { it.id == log.actorId } ?: allUsers.find { it.id == log.userId && log.actorId == null }
        val actorName = if (actorUser != null) "${actorUser.prenom} ${actorUser.nom}" else "Système automatique"
        val formattedTime = java.text.SimpleDateFormat("dd MMMM yyyy 'à' HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date(log.timestamp))

        AlertDialog(
            onDismissRequest = { selectedLogForDialog = null },
            title = { Text("Détails de l'événement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Action: ${log.action}", fontWeight = FontWeight.Bold)
                    Text(text = "Description: ${log.description}")
                    Text(text = "Date/Heure: $formattedTime")
                    Text(text = "Exécutant (Actor): $actorName (ID: ${log.actorId ?: "N/A"})")
                    Text(text = "ID Cible (User): ${log.userId}")
                    if (log.orderId != null) {
                        Text(text = "Commande Associée: ${log.orderId}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedLogForDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}
