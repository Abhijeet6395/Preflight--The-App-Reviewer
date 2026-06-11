package com.example.appreviewer_1.domain.ai

import com.example.appreviewer_1.domain.model.AiInsight
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

/**
 * On-device insight generation. Implementations must work fully offline —
 * "no data leaves the device" is the product's core promise.
 *
 * To plug in Gemini Nano on supported devices, provide an Android
 * implementation backed by the Google AI Edge SDK (com.google.ai.edge.aicore)
 * and return it from `createAiEngine()` in Platform.android.kt; fall back to
 * [RuleBasedAiEngine] when the device doesn't ship Nano.
 */
interface AiEngine {
    val displayName: String
    suspend fun generateInsight(metadata: AppMetadata, findings: List<Finding>): AiInsight
}

/** Deterministic "fix first" list shared by all engines — the LLM narrates, the rules rank. */
fun topPriorities(findings: List<Finding>): List<String> =
    findings
        .filter { it.severity != Severity.INFO }
        .sortedBy { it.severity.ordinal }
        .take(3)
        .map { it.recommendation }

/**
 * Deterministic, dependency-free insight composer. Reads the structured
 * findings and writes a reviewer-style narrative, so the demo works on any
 * device with zero API cost.
 */
class RuleBasedAiEngine : AiEngine {

    override val displayName = "On-device insight engine"

    override suspend fun generateInsight(
        metadata: AppMetadata,
        findings: List<Finding>,
    ): AiInsight {
        val critical = findings.count { it.severity == Severity.CRITICAL }
        val high = findings.count { it.severity == Severity.HIGH }
        val actionable = findings.count { it.severity != Severity.INFO }
        val appLabel = metadata.appName ?: metadata.packageName ?: metadata.sourceLabel

        val verdict = when {
            critical > 0 ->
                "$appLabel is not ready to submit: $critical critical " +
                    issueWord(critical) + " would likely cause an immediate Play rejection."
            high > 0 ->
                "$appLabel is close, but $high high-severity " + issueWord(high) +
                    " should be fixed before review — they are the kind reviewers actually catch."
            actionable > 0 ->
                "$appLabel looks submission-ready. The remaining $actionable " +
                    issueWord(actionable) + " are polish items worth a quick pass."
            else ->
                "$appLabel sailed through every static check. Run the runtime accessibility " +
                    "scanner and ship it."
        }

        val context = buildString {
            if (metadata.permissions.isNotEmpty()) {
                append("It requests ${metadata.permissions.size} permissions")
                val target = metadata.targetSdk
                if (target != null) append(" and targets API $target")
                append(". ")
            }
            val worstCategory = findings
                .filter { it.severity != Severity.INFO }
                .groupBy { it.category }
                .maxByOrNull { (_, list) -> list.sumOf { it.severity.weight } }
                ?.key
            if (worstCategory != null) {
                append("Most of the risk is concentrated in ${worstCategory.title.lowercase()}.")
            }
        }

        return AiInsight(
            engineName = displayName,
            summary = listOf(verdict, context).filter { it.isNotBlank() }.joinToString(" "),
            priorities = topPriorities(findings),
        )
    }

    private fun issueWord(count: Int) = if (count == 1) "issue" else "issues"
}
