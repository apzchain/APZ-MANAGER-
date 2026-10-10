```kotlin
package com.apz.manager.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.apz.manager.data.repository.AnalysisRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    onDone: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnalysisRepository(ctx) }

    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                ctx.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) { /* not persistable */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحلیل APK") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedUri?.lastPathSegment ?: "فایلی انتخاب نشده",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    picker.launch(arrayOf("application/vnd.android.package-archive",
                        "application/octet-stream", "*/*"))
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text("انتخاب فایل APK") }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    running = true
                    error = null
                    scope.launch {
                        try {
                            val report = repo.analyze(uri)
                            onDone(report.sha256)
                        } catch (t: Throwable) {
                            error = t.message ?: "خطای ناشناخته"
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = selectedUri != null && !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text("شروع تحلیل") }

            if (running) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "در حال تحلیل... ممکن است چند دقیقه طول بکشد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "خطا: $it",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```
