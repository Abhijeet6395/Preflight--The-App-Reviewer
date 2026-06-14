package com.example.appreviewer_1.domain.ai

/**
 * Output guardrail for the LLM verdict: forces the model's text back to plain
 * prose and rejects empty/garbled results so the caller can retry or fall back.
 *
 * Pure string logic, kept in commonMain so it's unit-testable without a device.
 */
object VerdictSanitizer {

    private const val MIN_LEN = 16
    private const val MAX_LEN = 1200

    private val HEADING_OR_BULLET = Regex("(?m)^\\s*(#{1,6}|[-*•])\\s+")

    // Strip emphasis/code markers only — NOT underscores, which appear in
    // identifiers the verdict legitimately names (READ_SMS, targetSdk…).
    private val EMPHASIS_MARKS = Regex("[*`]")

    private val WHITESPACE = Regex("\\s+")

    /** Returns clean single-paragraph prose, or null if the result is too short
     *  or too long to trust. */
    fun clean(raw: String?): String? {
        if (raw == null) return null
        val prose = raw
            .replace("```", " ")
            .replace(HEADING_OR_BULLET, "")
            .replace(EMPHASIS_MARKS, "")
            .replace(WHITESPACE, " ")
            .trim()
        return prose.takeIf { it.length in MIN_LEN..MAX_LEN }
    }
}
