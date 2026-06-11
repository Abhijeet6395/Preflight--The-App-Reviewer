package com.example.appreviewer_1.platform

import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.ai.RuleBasedAiEngine
import com.example.appreviewer_1.domain.ai.topPriorities
import com.example.appreviewer_1.domain.model.AiInsight
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Gemini Nano via the ML Kit GenAI Prompt API — fully on-device, no API key.
 *
 * Nano only ships on supported devices (Pixel 9+, recent Galaxy S, etc.), so
 * every path that can fail falls back to [RuleBasedAiEngine]; the report
 * labels which engine actually produced the insight.
 */
class GeminiNanoAiEngine(
    private val fallback: AiEngine = RuleBasedAiEngine(),
) : AiEngine {

    override val displayName = "Gemini Nano · on-device"

    // Outlives any single scan so a triggered model download isn't cancelled
    // when the user leaves the analyzing screen.
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun generateInsight(
        metadata: AppMetadata,
        findings: List<Finding>,
    ): AiInsight {
        return try {
            val model = Generation.getClient()
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE -> Unit
                FeatureStatus.DOWNLOADABLE -> {
                    // Kick off the model download so a later scan can use Nano,
                    // but don't make this scan wait on a multi-GB fetch.
                    downloadScope.launch {
                        runCatching { model.download().collect {} }
                    }
                    return fallback.generateInsight(metadata, findings)
                }
                else -> return fallback.generateInsight(metadata, findings)
            }

            val response = model.generateContent(buildPrompt(metadata, findings))
            val text = response.candidates.firstOrNull()?.text?.trim()
            if (text.isNullOrBlank()) {
                fallback.generateInsight(metadata, findings)
            } else {
                AiInsight(
                    engineName = displayName,
                    summary = text,
                    priorities = topPriorities(findings),
                )
            }
        } catch (e: Exception) {
            fallback.generateInsight(metadata, findings)
        }
    }

    private fun buildPrompt(metadata: AppMetadata, findings: List<Finding>): String {
        val actionable = findings.filter { it.severity != Severity.INFO }
        return buildString {
            appendLine(
                "You are an experienced Google Play app reviewer. Based on this " +
                    "static-analysis result, write a 2-3 sentence verdict for the " +
                    "developer: is the app ready to submit, what is the overall risk, " +
                    "and what matters most. Plain text only, no markdown, no lists."
            )
            appendLine()
            appendLine("App: ${metadata.appName ?: metadata.sourceLabel}")
            metadata.targetSdk?.let { appendLine("Targets API $it") }
            appendLine("Requests ${metadata.permissions.size} permissions")
            if (metadata.trackers.isNotEmpty()) {
                appendLine("Bundled third-party SDKs: ${metadata.trackers.joinToString()}")
            }
            appendLine()
            appendLine("Findings:")
            if (actionable.isEmpty()) {
                appendLine("- none, all static checks passed")
            } else {
                actionable.forEach {
                    appendLine("- [${it.severity.label}] ${it.title}")
                }
            }
        }
    }
}
