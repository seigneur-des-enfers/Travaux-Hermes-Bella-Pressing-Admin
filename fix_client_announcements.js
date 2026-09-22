const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', 'utf8');

const target = `            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),`;

const replacement = `            }
        }
        
        if (announcements.any { it.isVisible }) {
            item {
                Text("Annonces & Publicités", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(announcements.filter { it.isVisible }) { ann ->
                        Card(modifier = Modifier.width(280.dp), shape = RoundedCornerShape(12.dp)) {
                            Column {
                                if (ann.imageUrl.isNotBlank()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Media", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(ann.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ann.message, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/ClientDashboard.kt', code);
console.log("ClientDashboard announcements UI added");
