package com.example.appreviewer_1.data

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.SourceType

/**
 * Lightweight AndroidManifest.xml text parser for GitHub-sourced projects.
 * Regex over XML is fine here: manifests are machine-written, and we only
 * extract flat attributes — no nesting semantics.
 */
object ManifestParser {

    private val permissionRegex =
        Regex("""<uses-permission[^>]*android:name\s*=\s*"([^"]+)"""")
    private val packageRegex =
        Regex("""<manifest[^>]*package\s*=\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
    private val applicationTagRegex =
        Regex("""<application\b[^>]*>""", RegexOption.DOT_MATCHES_ALL)
    private val componentRegex =
        Regex("""<(activity|service|receiver)\b[^>]*?(/?)>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(
        manifestXml: String,
        sourceLabel: String,
        gradleScript: String? = null,
    ): AppMetadata {
        val permissions = permissionRegex.findAll(manifestXml)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val applicationTag = applicationTagRegex.find(manifestXml)?.value.orEmpty()

        var activities = 0
        var services = 0
        var receivers = 0
        var exportedActivities = 0
        var exportedServices = 0
        var exportedReceivers = 0
        componentRegex.findAll(manifestXml).forEach { match ->
            val exported = match.value.contains("""android:exported="true"""")
            when (match.groupValues[1]) {
                "activity" -> { activities++; if (exported) exportedActivities++ }
                "service" -> { services++; if (exported) exportedServices++ }
                "receiver" -> { receivers++; if (exported) exportedReceivers++ }
            }
        }

        return AppMetadata(
            sourceType = SourceType.GITHUB,
            sourceLabel = sourceLabel,
            packageName = packageRegex.find(manifestXml)?.groupValues?.get(1)
                ?: gradleScript?.let { extractGradleString(it, "applicationId") },
            appName = null,
            versionName = gradleScript?.let { extractGradleString(it, "versionName") },
            minSdk = gradleScript?.let { extractGradleInt(it, "minSdk") },
            targetSdk = gradleScript?.let { extractGradleInt(it, "targetSdk") },
            permissions = permissions,
            activityCount = activities,
            serviceCount = services,
            receiverCount = receivers,
            exportedActivities = exportedActivities,
            exportedServices = exportedServices,
            exportedReceivers = exportedReceivers,
            debuggable = applicationTag.contains("""android:debuggable="true""""),
            allowBackup = applicationTag.contains("""android:allowBackup="true""""),
            usesCleartextTraffic = applicationTag.contains("""android:usesCleartextTraffic="true""""),
        )
    }

    private fun extractGradleInt(script: String, key: String): Int? =
        Regex("""$key(?:Version)?\s*=?\s*(\d+)""").find(script)?.groupValues?.get(1)?.toIntOrNull()

    private fun extractGradleString(script: String, key: String): String? =
        Regex("""$key\s*=?\s*"([^"]+)"""").find(script)?.groupValues?.get(1)
}
