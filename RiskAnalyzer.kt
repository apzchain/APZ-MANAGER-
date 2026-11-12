package com.apz.manager.domain

object RiskAnalyzer {
    private val sensitive = mapOf(
        "android.permission.READ_SMS" to 25,
        "android.permission.SEND_SMS" to 30,
        "android.permission.RECORD_AUDIO" to 15,
        "android.permission.CAMERA" to 10,
        "android.permission.READ_CONTACTS" to 15,
        "android.permission.ACCESS_FINE_LOCATION" to 15,
        "android.permission.READ_CALL_LOG" to 25,
        "android.permission.REQUEST_INSTALL_PACKAGES" to 20,
        "android.permission.SYSTEM_ALERT_WINDOW" to 20,
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to 25
    )

    fun score(app: InstalledApp): RiskResult {
        val matches = app.permissions.filter { it in sensitive }
        val score = matches.sumOf { sensitive[it] ?: 0 }.coerceAtMost(100)
        val level = when {
            score >= 60 -> "High"
            score >= 25 -> "Medium"
            else -> "Low"
        }
        val reasons = matches.map { it.substringAfterLast(".") }
        return RiskResult(score, level, reasons)
    }
}
