```kotlin
package com.apz.manager

import android.app.Application
import android.util.Log
import com.apz.manager.data.prefs.AppPreferences
import com.apz.manager.python.PythonBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class APZApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var prefs: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)

        appScope.launch {
            try {
                PythonBridge.init(this@APZApplication, prefs)
                Log.i(TAG, "Python initialized")
            } catch (t: Throwable) {
                Log.e(TAG, "Python init failed", t)
            }
        }
    }

    companion object {
        const val TAG = "APZ"
    }
}
```

---
