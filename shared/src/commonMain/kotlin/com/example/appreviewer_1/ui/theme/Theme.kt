package com.example.appreviewer_1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.appreviewer_1.domain.model.Severity

val DeepSpace = Color(0xFF0B1020)
val SurfaceNavy = Color(0xFF141B30)
val SurfaceNavyHigh = Color(0xFF1C2542)
val NeonCyan = Color(0xFF4FE3FD)
val NeonViolet = Color(0xFF8B7CF6)
val NeonGreen = Color(0xFF34D399)
val Amber = Color(0xFFFBBF24)
val Tangerine = Color(0xFFFB923C)
val Coral = Color(0xFFF87171)
val Slate = Color(0xFF94A3B8)
val Mist = Color(0xFFE2E8F0)

val Severity.color: Color
    get() = when (this) {
        Severity.CRITICAL -> Coral
        Severity.HIGH -> Tangerine
        Severity.MEDIUM -> Amber
        Severity.LOW -> NeonCyan
        Severity.INFO -> Slate
    }

fun scoreColor(score: Int): Color = when {
    score >= 85 -> NeonGreen
    score >= 60 -> Amber
    else -> Coral
}

private val DarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepSpace,
    secondary = NeonViolet,
    onSecondary = DeepSpace,
    tertiary = NeonGreen,
    background = DeepSpace,
    onBackground = Mist,
    surface = SurfaceNavy,
    onSurface = Mist,
    surfaceVariant = SurfaceNavyHigh,
    onSurfaceVariant = Slate,
    error = Coral,
    outline = Color(0xFF31405F),
)

@Composable
fun AppReviewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
