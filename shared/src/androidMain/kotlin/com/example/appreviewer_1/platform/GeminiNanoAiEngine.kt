package com.example.appreviewer_1.platform

import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.ai.PromptBuilder
import com.example.appreviewer_1.domain.ai.RuleBasedAiEngine
import com.example.appreviewer_1.domain.ai.VerdictSanitizer
import com.example.appreviewer_1.domain.ai.topPriorities
import com.example.appreviewer_1.domain.model.AiInsight
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Finding
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Gemini Nano via the ML Kit GenAI Prompt API — fully on-device, no API key.
 *
 * The prompt is built by the shared [PromptBuilder] so it's app-specific and
 * testable. There are three layers of control around the model:
 *  1. the app-aware prompt (PromptBuilder),
 *  2. generation config — low temperature + token cap for a tight, consistent
 *     reviewer verdict,
 *  3. output guardrails — the result is sanitized to plain prose; a blank/garbled
 *     result triggers one stricter retry, then the rule-based engine.
 *
 * Nano only ships on supported devices (Pixel 9+, recent Galaxy S, etc.), so every
 * path that can fail falls back to [RuleBasedAiEngine]; the report labels which
 * engine actually produced the insight.
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

            // Phase 2 — the app-specific half. Phase 1 (PromptBuilder.instruction)
            // is a constant, sent as the prompt prefix inside generate().
            val context = PromptBuilder.appContext(metadata, findings)

            // First pass at low temperature; on a blank/garbled result, one stricter
            // retry; if that still fails, fall through to the rule-based engine.
            val summary = generate(model, context, temperature = 0.3f)
                ?: generate(model, "$context\n\n$STRICT_REMINDER", temperature = 0.1f)

            if (summary == null) {
                fallback.generateInsight(metadata, findings)
            } else {
                AiInsight(
                    engineName = displayName,
                    summary = summary,
                    priorities = topPriorities(findings),
                )
            }
        } catch (e: Exception) {
            fallback.generateInsight(metadata, findings)
        }
    }

    private suspend fun generate(
        model: GenerativeModel,
        context: String,
        temperature: Float,
    ): String? {
        val request = generateContentRequest(TextPart(context)) {
            promptPrefix = PromptPrefix(PromptBuilder.instruction)
            this.temperature = temperature
            topK = TOP_K
            maxOutputTokens = MAX_OUTPUT_TOKENS
        }
        val raw = model.generateContent(request).candidates.firstOrNull()?.text
        return VerdictSanitizer.clean(raw)
    }

    private companion object {
        const val TOP_K = 40
        const val MAX_OUTPUT_TOKENS = 256
        const val STRICT_REMINDER =
            "Reply with 2-3 sentences of plain prose only. No markdown, no lists, no headings."
    }
}
