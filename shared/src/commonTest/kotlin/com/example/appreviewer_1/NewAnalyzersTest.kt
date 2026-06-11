package com.example.appreviewer_1

import com.example.appreviewer_1.data.MarkdownExporter
import com.example.appreviewer_1.data.SampleTarget
import com.example.appreviewer_1.domain.analyzer.FootprintAnalyzer
import com.example.appreviewer_1.domain.analyzer.ScoreCalculator
import com.example.appreviewer_1.domain.analyzer.TrackerAnalyzer
import com.example.appreviewer_1.domain.model.AiInsight
import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.domain.model.SourceType
import kotlin.test.Test
import kotlin.test.assertTrue

class NewAnalyzersTest {

    @Test
    fun trackerAnalyzerFlagsSampleSdks() {
        val findings = TrackerAnalyzer().analyze(SampleTarget.metadata)
        assertTrue(findings.any { it.id == "privacy_trackers" && it.severity == Severity.HIGH })
        assertTrue(findings.any { it.id == "privacy_ad_stack" })
    }

    @Test
    fun trackerAnalyzerSilentWithoutTrackers() {
        val clean = AppMetadata(sourceType = SourceType.APK, sourceLabel = "clean.apk")
        assertTrue(TrackerAnalyzer().analyze(clean).isEmpty())
    }

    @Test
    fun footprintAnalyzerFlagsSizeAndLegacyAbis() {
        val findings = FootprintAnalyzer().analyze(SampleTarget.metadata)
        assertTrue(findings.any { it.id == "footprint_apk_size" })
        assertTrue(findings.any { it.id == "footprint_dex_size" })
        assertTrue(findings.any { it.id == "footprint_legacy_abi" })
    }

    @Test
    fun markdownExportContainsScoreFindingsAndInsight() {
        val findings = TrackerAnalyzer().analyze(SampleTarget.metadata)
        val score = ScoreCalculator.score(findings)
        val report = AnalysisReport(
            metadata = SampleTarget.metadata,
            findings = findings,
            score = score,
            grade = ScoreCalculator.grade(score),
            insight = AiInsight("Test engine", "Summary text.", listOf("Fix the thing")),
        )
        val md = MarkdownExporter.export(report)

        assertTrue(md.contains("# Preflight audit"))
        assertTrue(md.contains("Score: $score/100"))
        assertTrue(md.contains("Summary text."))
        assertTrue(md.contains("Fix the thing"))
        assertTrue(md.contains("third-party SDKs detected"))
    }
}
