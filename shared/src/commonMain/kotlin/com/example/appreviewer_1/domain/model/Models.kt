package com.example.appreviewer_1.domain.model

import kotlinx.serialization.Serializable

enum class Severity(val weight: Int, val label: String) {
    CRITICAL(15, "Critical"),
    HIGH(10, "High"),
    MEDIUM(6, "Medium"),
    LOW(3, "Low"),
    INFO(0, "Info"),
}

enum class Category(val title: String) {
    SECURITY("Security"),
    PRIVACY("Privacy"),
    POLICY("Play Policy"),
    ACCESSIBILITY("Accessibility"),
    EXPERIENCE("Experience"),
}

@Serializable
data class Finding(
    val id: String,
    val title: String,
    val detail: String,
    val recommendation: String,
    val severity: Severity,
    val category: Category,
)

enum class SourceType { APK, GITHUB, SAMPLE }

/** Everything the analyzers know about an app, extracted from an APK or a repo. */
@Serializable
data class AppMetadata(
    val sourceType: SourceType,
    val sourceLabel: String,
    val appName: String? = null,
    val packageName: String? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val permissions: List<String> = emptyList(),
    val activityCount: Int = 0,
    val serviceCount: Int = 0,
    val receiverCount: Int = 0,
    val exportedActivities: Int = 0,
    val exportedServices: Int = 0,
    val exportedReceivers: Int = 0,
    val debuggable: Boolean? = null,
    val allowBackup: Boolean? = null,
    val usesCleartextTraffic: Boolean? = null,
    // APK-only footprint data (null/empty when source is GitHub)
    val apkSizeBytes: Long? = null,
    val dexSizeBytes: Long? = null,
    val nativeAbis: List<String> = emptyList(),
    val trackers: List<String> = emptyList(),
)

@Serializable
data class AiInsight(
    val engineName: String,
    val summary: String,
    val priorities: List<String>,
)

@Serializable
data class AnalysisReport(
    val metadata: AppMetadata,
    val findings: List<Finding>,
    val score: Int,
    val grade: String,
    val insight: AiInsight,
) {
    fun findingsByCategory(): Map<Category, List<Finding>> =
        findings.groupBy { it.category }
            .mapValues { (_, list) -> list.sortedBy { it.severity.ordinal } }
}
