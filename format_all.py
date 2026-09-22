with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'r') as f:
    content = f.read()

# brute force it manually since it is stuck
new_content = """package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel

"""

# get everything from "fun LoginScreen" onwards
import re
match = re.search(r'fun LoginScreen.*', content, re.DOTALL)
if match:
    new_content += match.group(0)

with open('app/src/main/java/com/example/ui/screens/AuthScreens.kt', 'w') as f:
    f.write(new_content)
