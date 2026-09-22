import java.io.File

fun main() {
    val file = File("app/src/main/java/com/example/ui/screens/AuthScreens.kt")
    var content = file.readText()
    
    // Replace the problematic block entirely
    val oldBlock = """
                Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) { Text("Map is disabled without API Key", modifier = Modifier.align(Alignment.Center)) }
// GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng -> 
                        selectedLocation = latLng
                        coordonnees = "${'$'}{latLng.latitude}, ${'$'}{latLng.longitude}"
                    }
                ) {
                    if (selectedLocation != null) {
                        Marker(
                            state = MarkerState(position = selectedLocation!!),
                            title = "Mon adresse"
                        )
                    }
                }
"""

    val newBlock = """
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.LightGray)) { androidx.compose.material3.Text("Map is disabled without API Key", modifier = Modifier.align(androidx.compose.ui.Alignment.Center)) }
"""
    
    if (content.contains(oldBlock.trim())) {
       content = content.replace(oldBlock.trim(), newBlock.trim())
       file.writeText(content)
       println("Replaced successfully")
    } else {
       println("Block not found")
    }
}
