const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
/    Column\(modifier = Modifier\.fillMaxSize\(\)\.padding\(padding\)\) \{[\s\S]*?    if \(showBottomSheet && selectedClient != null\) \{/m,
`    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                                Text("\${client.prenom} \${client.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(client.telephone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                if (client.isSuspended) {
                                    Text("Suspendu", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
    
    if (showBottomSheet && selectedClient != null) {`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("AdminClientsTab fixed");
