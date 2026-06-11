package com.example.appreviewer_1

import com.example.appreviewer_1.data.SampleTarget
import com.example.appreviewer_1.domain.analyzer.ScoreCalculator
import com.example.appreviewer_1.domain.analyzer.defaultAnalyzers
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.domain.model.SourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzerTest {

    private fun analyze(metadata: AppMetadata) =
        defaultAnalyzers().flatMap { it.analyze(metadata) }

    @Test
    fun sampleAppTriggersCriticalFindings() {
        val findings = analyze(SampleTarget.metadata)

        assertTrue(findings.any { it.id == "sec_debuggable" && it.severity == Severity.CRITICAL })
        assertTrue(findings.any { it.id == "sec_cleartext" })
        assertTrue(findings.any { it.id == "policy_target_old" })
        assertTrue(findings.any { it.id == "perm_restricted" })
    }

    @Test
    fun cleanAppScoresHigh() {
        val clean = AppMetadata(
            sourceType = SourceType.APK,
            sourceLabel = "clean.apk",
            appName = "Clean",
            packageName = "dev.example.clean",
            versionName = "1.0.0",
            minSdk = 26,
            targetSdk = 36,
            permissions = listOf("android.permission.INTERNET"),
            activityCount = 3,
            debuggable = false,
            allowBackup = false,
            usesCleartextTraffic = false,
        )
        val findings = analyze(clean)
        val score = ScoreCalculator.score(findings)

        assertTrue(findings.none { it.severity == Severity.CRITICAL }, "expected no criticals")
        assertTrue(score >= 95, "expected >=95, got $score")
        assertEquals("A+", ScoreCalculator.grade(score))
    }

    @Test
    fun scoreIsBoundedAndMonotonic() {
        val messy = ScoreCalculator.score(analyze(SampleTarget.metadata))
        assertTrue(messy in 5..100)
        assertTrue(messy < 60, "sample app should score poorly, got $messy")
    }
}
