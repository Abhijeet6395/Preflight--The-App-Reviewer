package com.example.appreviewer_1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appreviewer_1.data.HistoryEntry
import com.example.appreviewer_1.platform.rememberApkPicker
import com.example.appreviewer_1.platform.supportsApkPicking
import com.example.appreviewer_1.platform.supportsSelfAudit
import com.example.appreviewer_1.ui.components.FeatureChip
import com.example.appreviewer_1.ui.components.GlowCard
import com.example.appreviewer_1.ui.components.LottieBox
import com.example.appreviewer_1.ui.reviewer.ReviewerIntent
import com.example.appreviewer_1.ui.theme.NeonCyan
import com.example.appreviewer_1.ui.theme.NeonViolet
import com.example.appreviewer_1.ui.theme.Slate
import com.example.appreviewer_1.ui.theme.scoreColor

@Composable
fun HomeScreen(
    history: List<HistoryEntry>,
    onIntent: (ReviewerIntent) -> Unit,
) {
    var githubUrl by remember { mutableStateOf("") }
    val pickApk = rememberApkPicker { picked ->
        picked?.let { onIntent(ReviewerIntent.AnalyzeApk(it)) }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    @Composable
    fun StaggerIn(index: Int, content: @Composable () -> Unit) {
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(500, delayMillis = index * 130)) +
                slideInVertically(tween(500, delayMillis = index * 130)) { it / 3 },
        ) { content() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))

        StaggerIn(0) {
            LottieBox(
                path = "files/lottie_orb.json",
                modifier = Modifier.size(170.dp),
                contentDescription = null,
            )
        }

        Spacer(Modifier.height(12.dp))

        StaggerIn(1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Preflight",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(NeonCyan, NeonViolet)),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Clear your app for takeoff.\nCatch Play Store rejections before reviewers do — fully on-device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        StaggerIn(2) {
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                if (supportsApkPicking) {
                    Button(
                        onClick = pickApk,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    ) {
                        Text("Scan an APK", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("or", color = Slate, style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                }

                OutlinedTextField(
                    value = githubUrl,
                    onValueChange = { githubUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("github.com/owner/repo") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onIntent(ReviewerIntent.SubmitGithubUrl(githubUrl)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Review a GitHub repo")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        StaggerIn(3) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { onIntent(ReviewerIntent.AnalyzeSample) }) {
                    Text("Try the sample audit →", color = NeonViolet, fontWeight = FontWeight.SemiBold)
                }
                if (supportsSelfAudit) {
                    TextButton(onClick = { onIntent(ReviewerIntent.AnalyzeSelf) }) {
                        Text("✦ Audit this app itself", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        StaggerIn(4) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeatureChip("0 API keys")
                FeatureChip("On-device AI")
                FeatureChip("Lint-grade checks")
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            StaggerIn(5) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recent scans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    history.take(5).forEach { entry ->
                        HistoryRow(
                            entry = entry,
                            onClick = { onIntent(ReviewerIntent.OpenHistoryEntry(entry.id)) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        StaggerIn(6) {
            TextButton(onClick = { onIntent(ReviewerIntent.OpenPrivacy) }) {
                Text("Privacy", color = Slate, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val report = entry.report
    GlowCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        glow = scoreColor(report.score).copy(alpha = 0.6f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.metadata.appName ?: report.metadata.sourceLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${report.findings.size} findings · Grade ${report.grade}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
            Text(
                text = "${report.score}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scoreColor(report.score),
            )
        }
    }
}
