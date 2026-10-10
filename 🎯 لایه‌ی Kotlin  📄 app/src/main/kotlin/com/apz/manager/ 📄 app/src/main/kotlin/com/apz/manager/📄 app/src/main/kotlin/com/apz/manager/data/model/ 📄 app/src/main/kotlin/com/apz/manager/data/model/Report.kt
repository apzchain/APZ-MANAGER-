```kotlin
package com.apz.manager.data.model

import com.google.gson.annotations.SerializedName

data class Report(
    @SerializedName("file_name") val fileName: String = "",
    @SerializedName("sha256") val sha256: String = "",
    @SerializedName("size_bytes") val sizeBytes: Long = 0,
    @SerializedName("risk_score") val riskScore: Int = 0,
    @SerializedName("risk_level") val riskLevel: String = "CLEAN",
    @SerializedName("confidence") val confidence: Int = 0,
    @SerializedName("breakdown") val breakdown: Map<String, Double> = emptyMap(),
    @SerializedName("findings") val findings: List<Finding> = emptyList(),
    @SerializedName("explanation") val explanation: List<String> = emptyList(),
    @SerializedName("permissions") val permissions: List<String> = emptyList(),
    @SerializedName("exported") val exported: List<String> = emptyList(),
    @SerializedName("debuggable") val debuggable: Boolean = false,
    @SerializedName("package") val packageName: String = "",
    @SerializedName("version_name") val versionName: String = "",
    @SerializedName("min_sdk") val minSdk: Int = 0,
    @SerializedName("target_sdk") val targetSdk: Int = 0,
    @SerializedName("threat") val threat: Threat = Threat(),
    @SerializedName("reputation") val reputation: Map<String, Any> = emptyMap(),
    @SerializedName("analysis_time_seconds") val analysisTimeSeconds: Double = 0.0,
    @SerializedName("ai") val ai: AIResult? = null,
    @SerializedName("limitations") val limitations: List<String> = emptyList()
) {
    val level: RiskLevel get() = RiskLevel.from(riskLevel)
}

data class Finding(
    @SerializedName("title_fa") val titleFa: String = "",
    @SerializedName("weight") val weight: Int = 0,
    @SerializedName("detail_fa") val detailFa: String = "",
    @SerializedName("evidence") val evidence: List<String> = emptyList()
)

data class Threat(
    @SerializedName("yara") val yara: List<YaraMatch> = emptyList(),
    @SerializedName("iocs") val iocs: Map<String, List<String>> = emptyMap(),
    @SerializedName("mitre") val mitre: List<Mitre> = emptyList()
)

data class YaraMatch(
    @SerializedName("rule") val rule: String = "",
    @SerializedName("severity") val severity: String = "medium",
    @SerializedName("tags") val tags: List<String> = emptyList()
)

data class Mitre(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = ""
)

data class AIResult(
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("risk_explanation") val riskExplanation: String? = null,
    @SerializedName("recommendations") val recommendations: String? = null,
    @SerializedName("error") val error: String? = null
)

data class HistoryItem(
    @SerializedName("sha256") val sha256: String = "",
    @SerializedName("file_name") val fileName: String = "",
    @SerializedName("risk_score") val riskScore: Int = 0,
    @SerializedName("risk_level") val riskLevel: String = "CLEAN",
    @SerializedName("analyzed_at") val analyzedAt: String = ""
)
```
