const fs = require('fs');

let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'utf8');

const target = `    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {`;

const replacement = `    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {`;

code = code.replace(target, replacement);

if (!code.includes('import androidx.compose.foundation.verticalScroll')) {
    code = code.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState');
}

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AuthScreens.kt', code);
console.log("RegisterScreen scroll fixed");
