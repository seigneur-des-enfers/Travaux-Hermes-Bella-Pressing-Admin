const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

// For Orders
code = code.replace(/val filteredOrders = allOrders\.filter \{/g, 'val filteredOrders = allOrders.filter { !it.isArchived &&');
// If there's a simple orders list, wait, AdminOrdersTab:
code = code.replace(/items\(filteredOrders\) \{ order ->/g, 'items(filteredOrders) { order ->');
// We need to ensure we filter out archived items in all tabs.
// Let's just do a global replace for the usages, or patch the specific functions.

// In AdminOrdersTab
code = code.replace(/val filteredOrders = allOrders\.filter \{/g, 'val filteredOrders = allOrders.filter { !it.isArchived &&');
code = code.replace(/val statsOrders = allOrders/g, 'val statsOrders = allOrders.filter { !it.isArchived }');

// In AdminStocksTab
code = code.replace(/val allConsumables by viewModel.allConsumables.collectAsStateWithLifecycle\(\)/, 'val allConsumables by viewModel.allConsumables.collectAsStateWithLifecycle()\n    val activeConsumables = allConsumables.filter { !it.isArchived }');
code = code.replace(/items\(allConsumables\)/g, 'items(activeConsumables)');

// In AdminCatalogueTab
code = code.replace(/val allSpecialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle\(\)/, 'val allSpecialItems by viewModel.allSpecialItems.collectAsStateWithLifecycle()\n    val activeSpecialItems = allSpecialItems.filter { !it.isArchived }');
code = code.replace(/items\(allSpecialItems\)/g, 'items(activeSpecialItems)');

// In AdminSettingsTab
code = code.replace(/val allPromos by viewModel.allPromos.collectAsStateWithLifecycle\(\)/, 'val allPromos by viewModel.allPromos.collectAsStateWithLifecycle()\n    val activePromos = allPromos.filter { !it.isArchived }');
code = code.replace(/items\(allPromos\)/g, 'items(activePromos)');
code = code.replace(/val allAntiFraudRules by viewModel.allAntiFraudRules.collectAsStateWithLifecycle\(\)/, 'val allAntiFraudRules by viewModel.allAntiFraudRules.collectAsStateWithLifecycle()\n    val activeAntiFraudRules = allAntiFraudRules.filter { !it.isArchived }');
code = code.replace(/items\(allAntiFraudRules\)/g, 'items(activeAntiFraudRules)');

// In AdminStaffTab
code = code.replace(/val allAdmins by viewModel.allAdmins.collectAsStateWithLifecycle\(\)/, 'val allAdmins by viewModel.allAdmins.collectAsStateWithLifecycle()\n    val activeAdmins = allAdmins.filter { !it.isArchived }');
code = code.replace(/items\(allAdmins\)/g, 'items(activeAdmins)');

// In AdminRulesTab
code = code.replace(/val allRules by viewModel.allRules.collectAsStateWithLifecycle\(\)/, 'val allRules by viewModel.allRules.collectAsStateWithLifecycle()\n    val activeRules = allRules.filter { !it.isArchived }');
code = code.replace(/items\(allRules\)/g, 'items(activeRules)');

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("Filtered out archived items from UI tabs");
