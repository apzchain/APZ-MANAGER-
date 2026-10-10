```kotlin
package com.apz.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.apz.manager.APZApplication
import com.apz.manager.python.PythonBridge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as APZApplication
    val prefs = app.prefs
    val scope = rememberCoroutineScope()

    var yara by remember { mutableStateOf(prefs.enableYara) }
    var rep by remember { mutableStateOf(prefs.enableReputation) }
    var ai by remember { mutableStateOf(prefs.enableAi) }
    var vt by remember { mutableStateOf(prefs.vtKey) }
    var ha by remember { mutableStateOf(prefs.haKey) }
    var md by remember { mutableStateOf(prefs.mdKey) }
    var host by remember { mutableStateOf(prefs.ollamaHost) }
    var model by remember { mutableStateOf(prefs.ollamaModel) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleRow("اسکن YARA", yara) { yara = it }
            ToggleRow("اعتبارسنجی آنلاین (نیاز به کلید API)", rep) { rep = it }
            ToggleRow("تحلیل AI با Ollama", ai) { ai = it }

            Spacer(Modifier.height(8.dp))
            Text("کلیدهای API", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = vt,
                onValueChange = { vt = it },
                label = { Text("VirusTotal API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ha,
                onValueChange = { ha = it },
                label = { Text("Hybrid Analysis API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = md,
                onValueChange = { md = it },
                label = { Text("MetaDefender API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Ollama", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Ollama URL (مثلاً http://192.168.1.10:11434)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("مدل (مثلاً qwen2.5:7b-instruct)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    prefs.enableYara = yara
                    prefs.enableReputation = rep
                    prefs.enableAi = ai
                    prefs.vtKey = vt.trim()
                    prefs.haKey = ha.trim()
                    prefs.mdKey = md.trim()
                    prefs.ollamaHost = host.trim()
                    prefs.ollamaModel = model.trim()

                    scope.launch {
                        runCatching { PythonBridge.reconfigure(prefs) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره") }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
```
