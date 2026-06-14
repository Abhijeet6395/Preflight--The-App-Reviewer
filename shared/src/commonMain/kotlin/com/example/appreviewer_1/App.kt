package com.example.appreviewer_1

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appreviewer_1.ui.components.AuroraBackground
import com.example.appreviewer_1.ui.reviewer.ReviewerEffect
import com.example.appreviewer_1.ui.reviewer.ReviewerIntent
import com.example.appreviewer_1.ui.reviewer.ReviewerViewModel
import com.example.appreviewer_1.ui.reviewer.Screen
import com.example.appreviewer_1.ui.screens.AnalyzingScreen
import com.example.appreviewer_1.ui.screens.HomeScreen
import com.example.appreviewer_1.ui.screens.PrivacyScreen
import com.example.appreviewer_1.ui.screens.ReportScreen
import com.example.appreviewer_1.ui.theme.AppReviewerTheme

@Composable
fun App() {
    AppReviewerTheme {
        val viewModel: ReviewerViewModel = viewModel { ReviewerViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHost = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ReviewerEffect.ShowMessage -> snackbarHost.showSnackbar(effect.text)
                }
            }
        }

        AuroraBackground {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHost) },
                containerColor = Color.Transparent,
                // transparent container can't resolve a contentColorFor() match, so
                // without this every unstyled Text falls back to black
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize(),
            ) { _ ->
                AnimatedContent(
                    targetState = state.screen,
                    transitionSpec = {
                        (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 10 })
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(state.history, viewModel::onIntent)
                        Screen.PRIVACY -> PrivacyScreen(viewModel::onIntent)
                        Screen.ANALYZING -> AnalyzingScreen(state.targetLabel, state.stages)
                        Screen.REPORT -> state.report?.let { report ->
                            ReportScreen(
                                report = report,
                                scoreDelta = state.scoreDelta,
                                onShare = { viewModel.onIntent(ReviewerIntent.ShareReport) },
                                onBack = { viewModel.onIntent(ReviewerIntent.BackToHome) },
                            )
                        }
                    }
                }
            }
        }
    }
}
