package com.example.appreviewer_1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.ui.components.FeatureChip
import com.example.appreviewer_1.ui.components.GlowCard
import com.example.appreviewer_1.ui.components.LottieBox
import com.example.appreviewer_1.ui.components.ScoreRing
import com.example.appreviewer_1.ui.components.SeverityBadge
import com.example.appreviewer_1.ui.components.TypewriterText
import com.example.appreviewer_1.ui.theme.Coral
import com.example.appreviewer_1.ui.theme.NeonGreen
import com.example.appreviewer_1.ui.theme.NeonViolet
import com.example.appreviewer_1.ui.theme.Slate
import com.example.appreviewer_1.ui.theme.color

@Composable
fun ReportScreen(
    report: AnalysisReport,
    scoreDelta: Int?,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    Text("←  New scan", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onShare) {
                    Text("Share ↗", fontWeight = FontWeight.SemiBold, color = NeonViolet)
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (report.score >= 85) {
                        LottieBox(
                            path = "files/lottie_success.json",
                            modifier = Modifier.size(90.dp),
                            iterations = 1,
                            contentDescription = "Passed",
                        )
                    }
                    ScoreRing(score = report.score, grade = report.grade)
                    if (scoreDelta != null && scoreDelta != 0) {
                        Spacer(Modifier.height(8.dp))
                        val improved = scoreDelta > 0
                        Text(
                            text = if (improved) "▲ +$scoreDelta since last scan" else "▼ $scoreDelta since last scan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (improved) NeonGreen else Coral,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FeatureChip(report.metadata.sourceLabel)
                        report.metadata.packageName?.let { FeatureChip(it) }
                        report.metadata.targetSdk?.let { FeatureChip("target $it") }
                        FeatureChip("${report.metadata.permissions.size} permissions")
                        report.metadata.apkSizeBytes?.let {
                            FeatureChip("${it / (1024 * 1024)} MB")
                        }
                    }
                }
            }
        }

        item {
            GlowCard(modifier = Modifier.fillMaxWidth(), glow = NeonViolet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = NeonViolet, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Insight",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = report.insight.engineName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TypewriterText(
                    text = report.insight.summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (report.insight.priorities.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Fix first",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonViolet,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    report.insight.priorities.forEachIndexed { index, priority ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${index + 1}.",
                                color = NeonViolet,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(priority, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        report.findingsByCategory().forEach { (category, findings) ->
            item(key = "header_${category.name}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${findings.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Slate,
                    )
                }
            }
            items(findings, key = { it.id }) { finding ->
                FindingCard(finding)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun FindingCard(finding: Finding) {
    var expanded by remember { mutableStateOf(false) }

    GlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(animationSpec = spring()),
        glow = finding.severity.color,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(10.dp))
            SeverityBadge(finding.severity)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = finding.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "→ ${finding.recommendation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
