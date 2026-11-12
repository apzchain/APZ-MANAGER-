package com.apz.manager.domain

data class InstalledApp(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
    val permissions: List<String>,
    val apkPath: String
)

data class RiskResult(val score: Int, val level: String, val reasons: List<String>)
