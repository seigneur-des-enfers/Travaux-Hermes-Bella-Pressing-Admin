const fs = require('fs');
let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/        when \(selectedTab\) \{[\s\S]*?        \}\n    \}\n\}\n\nfun exportInvoiceToPDF/,
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
}

fun exportInvoiceToPDF`
);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("Bracket fixed");
