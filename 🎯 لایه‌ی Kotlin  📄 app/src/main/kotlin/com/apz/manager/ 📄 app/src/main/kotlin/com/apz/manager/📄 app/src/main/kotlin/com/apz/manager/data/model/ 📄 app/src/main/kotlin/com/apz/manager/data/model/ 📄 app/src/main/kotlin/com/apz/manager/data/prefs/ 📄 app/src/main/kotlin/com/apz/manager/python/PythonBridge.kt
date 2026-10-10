
```kotlin
package com.apz.manager.python

import android.content.Context
import com.apz.manager.APZApplication
import com.apz.manager.data.model.HistoryItem
import com.apz.manager.data.model.Report
import com.apz.manager.data.prefs.AppPreferences
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PythonBridge {

    private val gson = Gson()
    @Volatile private var initialized = false
    private lateinit var module: PyObject

    suspend fun init(context: Context, prefs: AppPreferences) = withContext(Dispatchers.IO) {
        if (initialized) return@withContext

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        module = py.getModule("apz_android")

        val rulesDir = File(context.filesDir, "rules").apply { mkdirs() }
        copyRules(context, rulesDir)

        val dbDir = File(context.filesDir, "db").apply { mkdirs() }
        val dbPath = File(dbDir, "apz.db").absolutePath

        val config = mapOf(
            "rules_dir" to rulesDir.absolutePath,
            "db_path" to dbPath,
            "enable_yara" to prefs.enableYara,
            "enable_reputation" to prefs.enableReputation,
            "enable_ai" to prefs.enableAi,
            "vt_key" to prefs.vtKey,
            "ha_key" to prefs.haKey,
            "md_key" to prefs.mdKey,
            "ollama_host" to prefs.ollamaHost,
            "ollama_model" to prefs.ollamaModel
        )
        module.callAttr("configure", gson.toJson(config)).toString()
        initialized = true
    }

    suspend fun reconfigure(prefs: AppPreferences) = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext
        val rulesDir = File("/data/data/com.apz.manager/files/rules")
        val dbPath = "/data/data/com.apz.manager/files/db/apz.db"
        val config = mapOf(
            "rules_dir" to rulesDir.absolutePath,
            "db_path" to dbPath,
            "enable_yara" to prefs.enableYara,
            "enable_reputation" to prefs.enableReputation,
            "enable_ai" to prefs.enableAi,
            "vt_key" to prefs.vtKey,
            "ha_key" to prefs.haKey,
            "md_key" to prefs.mdKey,
            "ollama_host" to prefs.ollamaHost,
            "ollama_model" to prefs.ollamaModel
        )
        module.callAttr("configure", gson.toJson(config))
    }

    suspend fun analyze(apkPath: String): Report = withContext(Dispatchers.IO) {
        val json = module.callAttr("analyze", apkPath).toString()
        gson.fromJson(json, Report::class.java)
    }

    suspend fun history(limit: Int = 100): List<HistoryItem> = withContext(Dispatchers.IO) {
        val json = module.callAttr("history", limit, 0).toString()
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        gson.fromJson<List<HistoryItem>>(json, type).orEmpty()
    }

    suspend fun getReport(sha256: String): Report? = withContext(Dispatchers.IO) {
        val json = module.callAttr("get_report", sha256).toString()
        if (json == "null" || json.isBlank()) null
        else gson.fromJson(json, Report::class.java)
    }

    suspend fun deleteReport(sha256: String) = withContext(Dispatchers.IO) {
        module.callAttr("delete_report", sha256)
    }

    suspend fun checkAI(host: String): Boolean = withContext(Dispatchers.IO) {
        module.callAttr("ai_check", host).toJava(Boolean::class.java)
    }

    private fun copyRules(context: Context, dest: File) {
        val assets = context.assets
        val rules = assets.list("rules").orEmpty()
        rules.forEach { name ->
            val out = File(dest, name)
            if (!out.exists()) {
                assets.open("rules/$name").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }
}
```
