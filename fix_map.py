with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'r') as f:
    content = f.read()

content = content.replace('    val defaultLocation = LatLng(12.6392, -8.0029) // Bamako default\n    val cameraPositionState = rememberCameraPositionState {\n        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)\n    }\n    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }', '')

with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'w') as f:
    f.write(content)
print("Removed state")
