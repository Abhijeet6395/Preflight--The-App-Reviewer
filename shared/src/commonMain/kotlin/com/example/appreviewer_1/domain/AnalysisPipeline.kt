package com.example.appreviewer_1.domain

import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.analyzer.Analyzer
import com.example.appreviewer_1.domain.analyzer.ScoreCalculator
import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.domain.model.AppMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

enum class Stage(val label: String, val caption: String) {
    INSPECT("Unpacking", "Reading package metadata"),
    STATIC("Static analysis", "Manifest, permissions & policy rules"),
    AI("AI insights", "Composing reviewer narrative on-device"),
    REPORT("Report", "Scoring and grading"),
}

sealed interface PipelineEvent {
    data class StageStarted(val stage: Stage) : PipelineEvent
    data class StageCompleted(val stage: Stage, val findingCount: Int = 0) : PipelineEvent
    data class Completed(val report: AnalysisReport) : PipelineEvent
    data class Failed(val message: String) : PipelineEvent
}

/**
 * Orchestrates inspect -> static analysis -> AI insight -> report as a cold
 * flow of stage events the UI can animate. The small delays pace the stage
 * checklist so each step is readable on screen; real work dominates on large
 * inputs.
 */
class AnalysisPipeline(
    private val analyzers: List<Analyzer>,
    private val aiEngine: AiEngine,
    private val stagePacingMillis: Long = 700,
) {

    fun run(inspect: suspend () -> AppMetadata): Flow<PipelineEvent> = flow {
        emit(PipelineEvent.StageStarted(Stage.INSPECT))
        val metadata = inspect()
        delay(stagePacingMillis)
        emit(PipelineEvent.StageCompleted(Stage.INSPECT))

        emit(PipelineEvent.StageStarted(Stage.STATIC))
        val findings = analyzers.flatMap { it.analyze(metadata) }
        delay(stagePacingMillis)
        emit(PipelineEvent.StageCompleted(Stage.STATIC, findings.size))

        emit(PipelineEvent.StageStarted(Stage.AI))
        val insight = aiEngine.generateInsight(metadata, findings)
        delay(stagePacingMillis)
        emit(PipelineEvent.StageCompleted(Stage.AI))

        emit(PipelineEvent.StageStarted(Stage.REPORT))
        val score = ScoreCalculator.score(findings)
        delay(stagePacingMillis / 2)
        emit(PipelineEvent.StageCompleted(Stage.REPORT, findings.size))

        emit(
            PipelineEvent.Completed(
                AnalysisReport(
                    metadata = metadata,
                    findings = findings,
                    score = score,
                    grade = ScoreCalculator.grade(score),
                    insight = insight,
                )
            )
        )
    }.catch { e ->
        emit(PipelineEvent.Failed(e.message ?: "Analysis failed"))
    }
}
