package com.apz.manager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.apz.manager.domain.InstalledApp
import com.apz.manager.domain.RiskAnalyzer

class AppRepository(private val context: Context) {
    @Suppress("DEPRECATION")
    fun installedApps(): List<InstalledApp> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else pm.getInstalledApplications(0)
        return packages.map { info ->
            val requested = try {
                val pi = if (Build.VERSION.SDK_INT >= 33)
                    pm.getPackageInfo(info.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                else pm.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
                pi.requestedPermissions?.toList().orEmpty()
            } catch (_: Exception) { emptyList() }
            InstalledApp(
                label = pm.getApplicationLabel(info).toString(),
                packageName = info.packageName,
                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                permissions = requested,
                apkPath = info.sourceDir
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun risk(app: InstalledApp) = RiskAnalyzer.score(app)
}
