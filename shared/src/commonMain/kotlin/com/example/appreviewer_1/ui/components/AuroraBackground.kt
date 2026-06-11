package com.example.appreviewer_1.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.appreviewer_1.ui.theme.DeepSpace
import com.example.appreviewer_1.ui.theme.NeonCyan
import com.example.appreviewer_1.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting aurora blobs behind every screen — pure Compose, no assets. */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .drawBehind {
                val w = size.width
                val h = size.height

                fun blob(color: Color, cx: Float, cy: Float, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }

                blob(
                    NeonViolet.copy(alpha = 0.18f),
                    w * (0.22f + 0.10f * sin(t)),
                    h * (0.18f + 0.07f * cos(t * 1.3f)),
                    w * 0.72f,
                )
                blob(
                    NeonCyan.copy(alpha = 0.10f),
                    w * (0.85f + 0.08f * cos(t)),
                    h * (0.45f + 0.10f * sin(t * 0.8f)),
                    w * 0.62f,
                )
                blob(
                    Color(0xFF2563EB).copy(alpha = 0.16f),
                    w * (0.42f + 0.12f * sin(t * 0.6f)),
                    h * 0.92f,
                    w * 0.85f,
                )
            },
        content = content,
    )
}
