package com.example.appreviewer_1

import com.example.appreviewer_1.data.HistoryRepository
import com.example.appreviewer_1.data.SampleTarget
import com.example.appreviewer_1.domain.model.AiInsight
import com.example.appreviewer_1.domain.model.AnalysisReport
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryRepositoryTest {

    private val dir = Files.createTempDirectory("history").toString()
    private val repository = HistoryRepository(dirProvider = { dir })

    private fun report(score: Int) = AnalysisReport(
        metadata = SampleTarget.metadata,
        findings = emptyList(),
        score = score,
        grade = "B",
        insight = AiInsight("test", "summary", emptyList()),
    )

    @Test
    fun firstScanHasNoDelta() {
        val result = repository.record(report(58))
        assertNull(result.scoreDelta)
        assertEquals(1, result.history.size)
    }

    @Test
    fun rescanOfSamePackageReportsDelta() {
        repository.record(report(58))
        val second = repository.record(report(84))
        assertEquals(26, second.scoreDelta)
        assertEquals(2, second.history.size)
    }

    @Test
    fun historyRoundTripsThroughDisk() {
        repository.record(report(70))
        val reloaded = HistoryRepository(dirProvider = { dir }).load()
        assertEquals(1, reloaded.size)
        assertEquals(70, reloaded.first().report.score)
    }
}
