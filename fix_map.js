const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

code = code.replace(
/    var showMapDialog by remember \{ mutableStateOf\(false\) \}/,
`    var showMapDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var isSatellite by remember { mutableStateOf(false) }
    val defaultLocation = LatLng(config?.pressingLat ?: 12.6392, config?.pressingLng ?: -8.0029)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    val mapProperties = MapProperties(mapType = if (isSatellite) MapType.SATELLITE else MapType.NORMAL)`
);

code = code.replace(
/                Button\(\n                    onClick = \{ showMapDialog = true \},\n                    modifier = Modifier\.fillMaxWidth\(\),\n                    shape = RoundedCornerShape\(12\.dp\)\n                \) \{\n                    Icon\(Icons\.Default\.Add, contentDescription = null\)\n                    Spacer\(modifier = Modifier\.width\(8\.dp\)\)\n                    Text\(if \(distanceKm\.isEmpty\(\)\) "Placer sur la carte \(Calcul distance\)" else "Distance : \$distanceKm km"\)\n                \}/,
`                Spacer(modifier = Modifier.height(8.dp))
                Text("Ciblez votre position (Calcul distance) - Distance : \${if (distanceKm.isEmpty()) "0" else distanceKm} km", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp))) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        onMapClick = { latLng -> 
                            selectedLocation = latLng
                            val dist = FloatArray(1)
                            Location.distanceBetween(defaultLocation.latitude, defaultLocation.longitude, latLng.latitude, latLng.longitude, dist)
                            val distKm = dist[0] / 1000f
                            distanceKm = java.util.Locale.US.let { String.format(it, "%.2f", distKm) }
                        }
                    ) {
                        if (selectedLocation != null) {
                            Marker(
                                state = MarkerState(position = selectedLocation!!),
                                title = "Ma Position"
                            )
                        }
                        Marker(
                            state = MarkerState(position = defaultLocation),
                            title = "Pressing"
                        )
                    }
                    // Controls
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Satellite", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(checked = isSatellite, onCheckedChange = { isSatellite = it }, modifier = Modifier.scale(0.8f))
                    }
                }`
);

// Now remove the dialog
code = code.replace(
/    if \(showMapDialog\) \{[\s\S]*?    \}/,
`// Map dialog removed, integrated directly into UI`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("Map embedded in CreateOrderScreen");
