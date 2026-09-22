const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'utf8');

code = code.replace(
/import androidx\.compose\.runtime\.\*/,
`import androidx.compose.runtime.*
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.draw.clip`
);

code = code.replace(
/    var coordonnees by remember \{ mutableStateOf\(""\) \}\n    var isAdmin by remember \{ mutableStateOf\(false\) \}/,
`    var coordonnees by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    
    val defaultLocation = LatLng(12.6392, -8.0029) // Bamako default
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }`
);

code = code.replace(
/                modifier = Modifier\.fillMaxSize\(\)\n                \.padding\(padding\)\n                \.padding\(horizontal = 24\.dp\),\n            horizontalAlignment = Alignment\.CenterHorizontally\n        \) \{/,
`                modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {`
);

code = code.replace(
/import androidx\.compose\.foundation\.layout\.\*/,
`import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll`
);

code = code.replace(
/            OutlinedTextField\(\n                value = coordonnees, onValueChange = \{ coordonnees = it \}, \n                label = \{ Text\("Coordonnées de résidence \(Optionnel\)"\) \},\n                leadingIcon = \{ Icon\(Icons\.Default\.Home, contentDescription = null\) \},\n                modifier = Modifier\.fillMaxWidth\(\),\n                shape = RoundedCornerShape\(12\.dp\)\n            \)/,
`            OutlinedTextField(
                value = coordonnees, onValueChange = { coordonnees = it }, 
                label = { Text("Adresse de résidence") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ou pointez votre adresse sur la carte :", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng -> 
                        selectedLocation = latLng
                        coordonnees = "\${latLng.latitude}, \${latLng.longitude}"
                    }
                ) {
                    if (selectedLocation != null) {
                        Marker(
                            state = MarkerState(position = selectedLocation!!),
                            title = "Mon adresse"
                        )
                    }
                }
            }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AuthScreens.kt', code);
console.log("Register screen updated with Map");
