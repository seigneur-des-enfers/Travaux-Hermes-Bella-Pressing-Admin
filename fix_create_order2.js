const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

// Remove duplicate cameraLauncher
code = code.replace(
/    val cameraLauncher = rememberLauncherForActivityResult\(androidx\.activity\.result\.contract\.ActivityResultContracts\.TakePicturePreview\(\)\) \{ bitmap ->\n        if \(bitmap != null\) \{\n            val file = File\(context\.cacheDir, "photo_\\\$\{System\.currentTimeMillis\(\)\}\.png"\)\n            file\.outputStream\(\)\.use \{ bitmap\.compress\(Bitmap\.CompressFormat\.PNG, 100, it\) \}\n            photoUri = file\.absolutePath\n        \}\n    \}/,
''
);
// wait, let's just use regex carefully or delete lines.
