const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

code = code.replace(
/import androidx\.compose\.runtime\.\*/,
`import androidx.compose.runtime.*
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.platform.LocalContext`
);

code = code.replace(
/    var promoCode by remember \{ mutableStateOf\(""\) \}/,
`    var promoCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "photo_\${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            photoUri = file.absolutePath
        }
    }`
);

code = code.replace(
/            Button\(\n                onClick = \{ photoUri = "mock_photo_uri_\$\{System\.currentTimeMillis\(\)\}" \}, \/\/ Simulated photo capture\n                modifier = Modifier\.fillMaxWidth\(\),\n                colors = ButtonDefaults\.buttonColors\(containerColor = MaterialTheme\.colorScheme\.secondary\),\n                shape = RoundedCornerShape\(12\.dp\)\n            \) \{\n                Icon\(Icons\.Default\.Add, contentDescription = null\)\n                Spacer\(modifier = Modifier\.width\(8\.dp\)\)\n                Text\(if \(photoUri\.isEmpty\(\)\) "Prendre une photo \(Preuve visuelle\)" else "Photo capturée ✓"\)\n            \}/,
`            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri.isEmpty()) "Prendre une photo (Preuve visuelle)" else "Photo capturée ✓")
            }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', code);
console.log("Camera fixed");
