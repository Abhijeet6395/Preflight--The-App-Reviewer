package com.example.appreviewer_1.domain.ai

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.domain.model.SourceType

/**
 * Builds the on-device LLM prompt from the concrete analysis result.
 *
 * Lives in commonMain so the prompt is one source of truth (any engine, any
 * platform) and can be unit-tested without a device. The "intelligence" is in
 * being SPECIFIC: it names the actual permissions, trackers, SDK gap and findings,
 * and adapts both the instruction and the focus to the app's risk profile —
 * instead of sending a fixed, generic template.
 *
 * Only the LLM path uses this; [RuleBasedAiEngine] composes its prose directly.
 */
object PromptBuilder {

    /** Google Play's current minimum target API for new submissions. Keep in sync
     *  with PolicyAnalyzer. */
    private const val PLAY_MIN_TARGET_SDK = 34

    /** Spell out at most this many findings so the prompt stays within Nano's
     *  small context window; the rest are summarized as a count. */
    private const val MAX_DETAILED_FINDINGS = 6

    fun build(metadata: AppMetadata, findings: List<Finding>): String {
        val actionable = findings
            .filter { it.severity != Severity.INFO }
            .sortedBy { it.severity.ordinal }
        val critical = actionable.count { it.severity == Severity.CRITICAL }
        val high = actionable.count { it.severity == Severity.HIGH }

        return buildString {
            appendLine(instruction(critical, high, actionable.size))
            appendLine()
            appendLine("APP UNDER REVIEW")
            appendLine(factSheet(metadata))
            appendLine()
            appendLine("STATIC-ANALYSIS FINDINGS (most severe first):")
            append(findingsBlock(actionable))
        }
    }

    /** The persona + ask, tuned to how much trouble the app is actually in. */
    private fun instruction(critical: Int, high: Int, actionable: Int): String {
        val ask = when {
            critical > 0 ->
                "This app would be rejected. In 2-3 sentences tell the developer plainly that " +
                    "it is not ready, name the single most important blocker, and what to fix first."
            high > 0 ->
                "This app is close but risky. In 2-3 sentences give a clear go/no-go, call out " +
                    "the highest-impact issue, and what to fix before submitting."
            actionable > 0 ->
                "This app looks submission-ready. In 2-3 sentences confirm it, then note the " +
                    "single most worthwhile polish item."
            else ->
                "This app passed every static check. In 2-3 sentences give a confident go and " +
                    "one sensible pre-launch reminder."
        }
        return "You are a senior Google Play app reviewer giving a developer a pre-submission " +
            "verdict. $ask Be specific to THIS app's findings below — reference the actual " +
            "issues, not generic advice. Plain prose only: no markdown, no bullet lists, no headings."
    }

    private fun factSheet(m: AppMetadata): String = buildList {
        add("Name: ${m.appName ?: m.packageName ?: m.sourceLabel} (${sourceLabel(m.sourceType)})")
        m.packageName?.let { add("Package: $it") }

        val sdkParts = listOfNotNull(
            m.minSdk?.let { "minSdk $it" },
            m.targetSdk?.let { "targetSdk $it" },
        )
        if (sdkParts.isNotEmpty()) {
            val gap = m.targetSdk
                ?.takeIf { it < PLAY_MIN_TARGET_SDK }
                ?.let { " — below Play's required API $PLAY_MIN_TARGET_SDK for new submissions" }
                .orEmpty()
            add("SDK: ${sdkParts.joinToString()}$gap")
        }

        if (m.permissions.isNotEmpty()) {
            val notable = m.permissions
                .filter { it in SENSITIVE_PERMISSIONS }
                .map { it.substringAfterLast('.') }
            val suffix = if (notable.isEmpty()) "" else ", notably ${notable.joinToString()}"
            add("Permissions: ${m.permissions.size}$suffix")
        }

        val exported = m.exportedActivities + m.exportedServices + m.exportedReceivers
        if (exported > 0) add("Exported components: $exported")

        val flags = listOfNotNull(
            "debuggable".takeIf { m.debuggable == true },
            "cleartext traffic allowed".takeIf { m.usesCleartextTraffic == true },
            "full-data backup allowed".takeIf { m.allowBackup == true },
        )
        if (flags.isNotEmpty()) add("Risky flags: ${flags.joinToString()}")

        if (m.trackers.isNotEmpty()) add("Third-party SDKs: ${m.trackers.joinToString()}")

        m.apkSizeBytes?.let { add("APK size: ${it / (1024 * 1024)} MB") }
    }.joinToString("\n") { "- $it" }

    private fun findingsBlock(actionable: List<Finding>): String {
        if (actionable.isEmpty()) return "None — every static check passed."
        val shown = actionable.take(MAX_DETAILED_FINDINGS).joinToString("\n") { f ->
            "- [${f.severity.label} · ${f.category.title}] ${f.title}. ${f.detail} " +
                "(Fix: ${f.recommendation})"
        }
        val extra = actionable.size - MAX_DETAILED_FINDINGS
        return if (extra > 0) "$shown\n- plus $extra more lower-severity finding(s)." else shown
    }

    private fun sourceLabel(t: SourceType) = when (t) {
        SourceType.APK -> "inspected from its APK"
        SourceType.GITHUB -> "from a public GitHub repo's manifest"
        SourceType.SAMPLE -> "bundled sample"
    }

    /** Permissions worth naming explicitly in the prompt — the ones reviewers and
     *  users actually react to. Mirrors PermissionAnalyzer's risk sets. */
    private val SENSITIVE_PERMISSIONS = setOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
    )
}
