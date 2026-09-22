with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'r') as f:
    content = f.read()
    
content = content.replace('fun LoginScreen(', '@Composable\nfun LoginScreen(')
content = content.replace('fun RegisterScreen(', '@Composable\nfun RegisterScreen(')

with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'w') as f:
    f.write(content)
