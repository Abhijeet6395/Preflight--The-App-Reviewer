package com.example.appreviewer_1

import com.example.appreviewer_1.domain.ai.PromptBuilder
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.domain.model.SourceType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptBuilderTest {

    private fun finding(
        severity: Severity,
        id: String = "x",
        category: Category = Category.SECURITY,
    ) = Finding(
        id = id,
        title = "$id title",
        detail = "$id detail.",
        recommendation = "$id fix.",
        severity = severity,
        category = category,
    )

    @Test
    fun criticalAppGetsRejectionInstruction() {
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk", targetSdk = 30)
        val prompt = PromptBuilder.build(meta, listOf(finding(Severity.CRITICAL)))
        assertTrue(prompt.contains("would be rejected"))
        assertTrue(prompt.contains("not ready"))
    }

    @Test
    fun includesConcreteAppFacts() {
        val meta = AppMetadata(
            sourceType = SourceType.APK,
            sourceLabel = "x.apk",
            appName = "Demo",
            targetSdk = 30,
            permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            trackers = listOf("Google AdMob"),
        )
        val prompt = PromptBuilder.build(meta, listOf(finding(Severity.HIGH)))
        assertContains(prompt, "Demo")
        assertContains(prompt, "READ_SMS")     // the specific sensitive permission, named
        assertContains(prompt, "Google AdMob") // the specific tracker, named
        assertContains(prompt, "34")           // the Play target-SDK gap, called out
    }

    @Test
    fun spellsOutFindingDetailAndFix() {
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk")
        val prompt = PromptBuilder.build(meta, listOf(finding(Severity.HIGH, id = "debuggable")))
        assertContains(prompt, "debuggable detail.")
        assertContains(prompt, "debuggable fix.")
    }

    @Test
    fun cleanAppGetsConfidentGo() {
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk", targetSdk = 35)
        val prompt = PromptBuilder.build(meta, emptyList())
        assertTrue(prompt.contains("passed every static check"))
    }

    @Test
    fun alwaysRequestsPlainProseOutput() {
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk")
        val prompt = PromptBuilder.build(meta, listOf(finding(Severity.MEDIUM)))
        assertTrue(prompt.lowercase().contains("no markdown"))
        assertTrue(prompt.lowercase().contains("plain prose only"))
    }

    @Test
    fun capsDetailedFindingsAndSummarizesRest() {
        val many = (1..10).map { finding(Severity.MEDIUM, id = "f$it") }
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk")
        val prompt = PromptBuilder.build(meta, many)
        assertContains(prompt, "more lower-severity finding(s).")
    }

    // --- two-phase split ---

    @Test
    fun instructionIsStaticPersonaWithFormatContract() {
        // Phase 1 holds no app data, so it's a constant carrying persona + format.
        assertContains(PromptBuilder.instruction, "Google Play app reviewer")
        assertTrue(PromptBuilder.instruction.lowercase().contains("plain prose only"))
        assertTrue(PromptBuilder.instruction.lowercase().contains("no markdown"))
    }

    @Test
    fun appContextCarriesRiskAndFactsButNotPersona() {
        val meta = AppMetadata(
            sourceType = SourceType.APK,
            sourceLabel = "x.apk",
            appName = "Demo",
            trackers = listOf("Google AdMob"),
        )
        val context = PromptBuilder.appContext(meta, listOf(finding(Severity.CRITICAL)))
        assertContains(context, "would be rejected")   // risk directive
        assertContains(context, "Demo")                // app facts
        assertContains(context, "Google AdMob")
        // the persona lives only in phase 1, proving the split
        assertTrue(!context.contains("Google Play app reviewer"))
    }

    @Test
    fun buildJoinsInstructionAndContext() {
        val meta = AppMetadata(sourceType = SourceType.APK, sourceLabel = "x.apk")
        val findings = listOf(finding(Severity.HIGH))
        val expected = "${PromptBuilder.instruction}\n\n${PromptBuilder.appContext(meta, findings)}"
        assertEquals(expected, PromptBuilder.build(meta, findings))
    }
}
