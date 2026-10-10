```kotlin
package com.apz.manager.data.repository

import android.content.Context
import com.apz.manager.data.model.Report
import com.apz.manager.python.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AnalysisRepository(private val context: Context) {

    suspend fun analyze(uri: android.net.Uri): Report = withContext(Dispatchers.IO) {
        val local = copyToLocal(uri)
        try {
            PythonBridge.analyze(local.absolutePath)
        } finally {
            local.delete()
        }
    }

    private fun copyToLocal(uri: android.net.Uri): File {
        val tmp = File(context.cacheDir, "pending.apk")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open uri" }
            tmp.outputStream().use { input.copyTo(it) }
        }
        return tmp
    }
}
```
