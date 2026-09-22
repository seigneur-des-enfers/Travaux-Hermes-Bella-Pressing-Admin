import re

with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'r') as f:
    content = f.read()

content = re.sub(r'(import .*?)import ', r'\1\nimport ', content)
content = re.sub(r'(import .*?)import ', r'\1\nimport ', content)
content = re.sub(r'(import .*?)import ', r'\1\nimport ', content)
content = re.sub(r'(import .*?)import ', r'\1\nimport ', content)
content = re.sub(r'Stateimport', 'State\nimport', content)
content = re.sub(r'MutableStateState', 'MutableState\n', content)
content = re.sub(r'import (.*?)fun ', r'import \1\n\nfun ', content)

with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'w') as f:
    f.write(content)
