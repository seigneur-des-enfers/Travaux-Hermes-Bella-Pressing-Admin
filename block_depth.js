const fs = require('fs');
const lines = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8').split('\n');
let depth = 0;
for (let i=0; i<lines.length; i++) {
    const line = lines[i];
    if (line.includes('{')) depth += line.split('{').length - 1;
    if (line.includes('}')) depth -= line.split('}').length - 1;
    if (i >= 520 && i <= 530) {
        console.log("Line " + (i+1) + " depth=" + depth + ": " + line);
    }
}
