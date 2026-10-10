```kotlin
package com.apz.manager.data.prefs

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("apz_prefs", Context.MODE_PRIVATE)

    var enableYara: Boolean
        get() = sp.getBoolean("enable_yara", true)
        set(v) = sp.edit().putBoolean("enable_yara", v).apply()

    var enableReputation: Boolean
        get() = sp.getBoolean("enable_reputation", false)
        set(v) = sp.edit().putBoolean("enable_reputation", v).apply()

    var enableAi: Boolean
        get() = sp.getBoolean("enable_ai", false)
        set(v) = sp.edit().putBoolean("enable_ai", v).apply()

    var vtKey: String
        get() = sp.getString("vt_key", "").orEmpty()
        set(v) = sp.edit().putString("vt_key", v).apply()

    var haKey: String
        get() = sp.getString("ha_key", "").orEmpty()
        set(v) = sp.edit().putString("ha_key", v).apply()

    var mdKey: String
        get() = sp.getString("md_key", "").orEmpty()
        set(v) = sp.edit().putString("md_key", v).apply()

    var ollamaHost: String
        get() = sp.getString("ollama_host", "").orEmpty()
        set(v) = sp.edit().putString("ollama_host", v).apply()

    var ollamaModel: String
        get() = sp.getString("ollama_model", "qwen2.5:7b-instruct").orEmpty()
        set(v) = sp.edit().putString("ollama_model", v).apply()
}
```
