package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.di.Graph
import com.example.ui.Navigation
import com.example.ui.theme.MyApplicationTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private val crashLog = mutableStateOf<String?>(null)

    private fun sendCrashLogToFirestoreHttp(log: String) {
        thread {
            try {
                val url = URL("https://firestore.googleapis.com/v1/projects/bella-pressing-c596f/databases/(default)/documents/crashes?key=AIzaSyAXlEQhWaRiH5w9pW1m8mgRv7wxX0GnXSQ")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val escapedLog = log.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                val json = "{\"fields\": {\"log\": {\"stringValue\": \"" + escapedLog + "\"}}}"
                conn.outputStream.write(json.toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            android.util.Log.e("FATAL_ERROR", stackTrace)
            sendCrashLogToFirestoreHttp("UNCAUGHT: " + stackTrace)
            crashLog.value = stackTrace
        }

        try {
            Graph.provide(this)
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    val log = crashLog.value
                    if (log != null) {
                        Scaffold { padding ->
                            SelectionContainer {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                    item { Text(text = "CRASH DETECTED:\n\n$log") }
                                }
                            }
                        }
                    } else {
                        Navigation()
                    }
                }
            }
        } catch (e: Throwable) {
            val stackTrace = android.util.Log.getStackTraceString(e)
            sendCrashLogToFirestoreHttp("INIT CRASH: " + stackTrace)
            crashLog.value = stackTrace
            setContent {
                MyApplicationTheme {
                    Scaffold { padding ->
                        SelectionContainer {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                item { Text(text = "INIT CRASH DETECTED:\n\n${crashLog.value}") }
                            }
                        }
                    }
                }
            }
        }
    }
}
