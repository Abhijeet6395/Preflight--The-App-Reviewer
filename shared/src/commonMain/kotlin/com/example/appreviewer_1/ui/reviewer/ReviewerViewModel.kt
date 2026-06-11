package com.example.appreviewer_1.ui.reviewer

import androidx.lifecycle.viewModelScope
import com.example.appreviewer_1.data.GithubRepoFetcher
import com.example.appreviewer_1.data.HistoryRepository
import com.example.appreviewer_1.data.MarkdownExporter
import com.example.appreviewer_1.data.SampleTarget
import com.example.appreviewer_1.domain.AnalysisPipeline
import com.example.appreviewer_1.domain.PipelineEvent
import com.example.appreviewer_1.domain.Stage
import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.analyzer.defaultAnalyzers
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.mvi.MviViewModel
import com.example.appreviewer_1.platform.PickedApk
import com.example.appreviewer_1.platform.createAiEngine
import com.example.appreviewer_1.platform.inspectApk
import com.example.appreviewer_1.platform.selfAuditMetadata
import com.example.appreviewer_1.platform.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewerViewModel(
    aiEngine: AiEngine = createAiEngine(),
    private val pipeline: AnalysisPipeline = AnalysisPipeline(defaultAnalyzers(), aiEngine),
    private val githubFetcher: GithubRepoFetcher = GithubRepoFetcher(),
    private val historyRepository: HistoryRepository = HistoryRepository(),
) : MviViewModel<ReviewerIntent, ReviewerState, ReviewerEffect>(ReviewerState()) {

    private var analysisJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { historyRepository.load() }
            setState { copy(history = stored) }
        }
    }

    override fun onIntent(intent: ReviewerIntent) {
        when (intent) {
            is ReviewerIntent.SubmitGithubUrl -> submitGithub(intent.url)
            is ReviewerIntent.AnalyzeApk ->
                startAnalysis(intent.apk.displayName) { inspectApk(intent.apk) }
            ReviewerIntent.AnalyzeSample ->
                startAnalysis(SampleTarget.metadata.sourceLabel) { SampleTarget.metadata }
            ReviewerIntent.AnalyzeSelf ->
                startAnalysis("Preflight · self-audit") { selfAuditMetadata() }
            is ReviewerIntent.OpenHistoryEntry -> openHistoryEntry(intent.id)
            ReviewerIntent.ShareReport ->
                currentState.report?.let { shareText(MarkdownExporter.export(it)) }
            ReviewerIntent.BackToHome -> {
                analysisJob?.cancel()
                setState { ReviewerState(history = history) }
            }
        }
    }

    private fun submitGithub(url: String) {
        val repo = GithubRepoFetcher.parseRepoUrl(url)
        if (repo == null) {
            sendEffect(
                ReviewerEffect.ShowMessage("That doesn't look like a GitHub repo URL — try github.com/owner/repo")
            )
            return
        }
        startAnalysis("${repo.first}/${repo.second}") { githubFetcher.fetch(url) }
    }

    private fun openHistoryEntry(id: Long) {
        val entry = currentState.history.find { it.id == id } ?: return
        setState {
            copy(
                screen = Screen.REPORT,
                report = entry.report,
                targetLabel = entry.report.metadata.sourceLabel,
                scoreDelta = null,
            )
        }
    }

    private fun startAnalysis(label: String, inspect: suspend () -> AppMetadata) {
        analysisJob?.cancel()
        setState {
            ReviewerState(
                screen = Screen.ANALYZING,
                targetLabel = label,
                stages = Stage.entries.map { StageProgress(it) },
                history = history,
            )
        }
        analysisJob = viewModelScope.launch {
            pipeline.run(inspect).collect(::reduce)
        }
    }

    private suspend fun reduce(event: PipelineEvent) {
        when (event) {
            is PipelineEvent.StageStarted -> updateStage(event.stage) {
                it.copy(status = StageStatus.RUNNING)
            }
            is PipelineEvent.StageCompleted -> updateStage(event.stage) {
                it.copy(status = StageStatus.DONE, findingCount = event.findingCount)
            }
            is PipelineEvent.Completed -> {
                val recorded = withContext(Dispatchers.IO) {
                    runCatching { historyRepository.record(event.report) }.getOrNull()
                }
                setState {
                    copy(
                        screen = Screen.REPORT,
                        report = event.report,
                        history = recorded?.history ?: history,
                        scoreDelta = recorded?.scoreDelta,
                    )
                }
            }
            is PipelineEvent.Failed -> {
                sendEffect(ReviewerEffect.ShowMessage(event.message))
                setState { copy(screen = Screen.HOME) }
            }
        }
    }

    private fun updateStage(stage: Stage, transform: (StageProgress) -> StageProgress) {
        setState {
            copy(stages = stages.map { if (it.stage == stage) transform(it) else it })
        }
    }
}
