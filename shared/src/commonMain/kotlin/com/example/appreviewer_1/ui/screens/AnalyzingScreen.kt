package com.example.appreviewer_1.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appreviewer_1.ui.components.GlowCard
import com.example.appreviewer_1.ui.components.LottieBox
import com.example.appreviewer_1.ui.reviewer.StageProgress
import com.example.appreviewer_1.ui.reviewer.StageStatus
import com.example.appreviewer_1.ui.theme.NeonCyan
import com.example.appreviewer_1.ui.theme.NeonGreen
import com.example.appreviewer_1.ui.theme.Slate
import com.example.appreviewer_1.ui.theme.SurfaceNavyHigh

@Composable
fun AnalyzingScreen(targetLabel: String, stages: List<StageProgress>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LottieBox(
            path = "files/lottie_scan.json",
            modifier = Modifier.size(220.dp),
            contentDescription = "Scanning",
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Auditing",
            style = MaterialTheme.typography.titleMedium,
            color = Slate,
        )
        Text(
            text = targetLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(28.dp))

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, stage ->
                StageRow(stage)
                if (index != stages.lastIndex) Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun StageRow(progress: StageProgress) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseScale",
    )

    val dotColor by animateColorAsState(
        targetValue = when (progress.status) {
            StageStatus.PENDING -> SurfaceNavyHigh
            StageStatus.RUNNING -> NeonCyan
            StageStatus.DONE -> NeonGreen
        },
        label = "dotColor",
    )
    val rowAlpha = if (progress.status == StageStatus.PENDING) 0.45f else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha(rowAlpha),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .scale(if (progress.status == StageStatus.RUNNING) pulseScale else 1f)
                .clip(CircleShape)
                .background(dotColor),
            contentAlignment = Alignment.Center,
        ) {
            if (progress.status == StageStatus.DONE) {
                Text("✓", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                text = progress.stage.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (progress.status == StageStatus.DONE && progress.findingCount > 0) {
                    "${progress.findingCount} findings"
                } else {
                    progress.stage.caption
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        }
    }
}
