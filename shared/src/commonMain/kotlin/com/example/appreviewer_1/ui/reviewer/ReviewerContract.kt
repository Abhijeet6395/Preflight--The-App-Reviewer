package com.example.appreviewer_1.ui.reviewer

import com.example.appreviewer_1.data.HistoryEntry
import com.example.appreviewer_1.domain.Stage
import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.mvi.UiEffect
import com.example.appreviewer_1.mvi.UiIntent
import com.example.appreviewer_1.mvi.UiState
import com.example.appreviewer_1.platform.PickedApk

enum class Screen { HOME, ANALYZING, REPORT }

enum class StageStatus { PENDING, RUNNING, DONE }

data class StageProgress(
    val stage: Stage,
    val status: StageStatus = StageStatus.PENDING,
    val findingCount: Int = 0,
)

sealed interface ReviewerIntent : UiIntent {
    data class SubmitGithubUrl(val url: String) : ReviewerIntent
    data class AnalyzeApk(val apk: PickedApk) : ReviewerIntent
    data object AnalyzeSample : ReviewerIntent
    data object AnalyzeSelf : ReviewerIntent
    data class OpenHistoryEntry(val id: Long) : ReviewerIntent
    data object ShareReport : ReviewerIntent
    data object BackToHome : ReviewerIntent
}

data class ReviewerState(
    val screen: Screen = Screen.HOME,
    val targetLabel: String = "",
    val stages: List<StageProgress> = Stage.entries.map { StageProgress(it) },
    val report: AnalysisReport? = null,
    val history: List<HistoryEntry> = emptyList(),
    /** score change vs the previous scan of the same app, null when unknown */
    val scoreDelta: Int? = null,
) : UiState

sealed interface ReviewerEffect : UiEffect {
    data class ShowMessage(val text: String) : ReviewerEffect
}
