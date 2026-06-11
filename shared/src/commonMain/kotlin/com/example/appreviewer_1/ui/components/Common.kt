package com.example.appreviewer_1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.ui.theme.NeonCyan
import com.example.appreviewer_1.ui.theme.SurfaceNavy
import com.example.appreviewer_1.ui.theme.SurfaceNavyHigh
import com.example.appreviewer_1.ui.theme.color
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(24.dp)

/** Gradient-surfaced card with a subtle neon border glow. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glow: Color = NeonCyan,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(SurfaceNavy, SurfaceNavyHigh)))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(glow.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f)),
                ),
                shape = CardShape,
            )
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    Text(
        text = severity.label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = severity.color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(severity.color.copy(alpha = 0.14f))
            .border(1.dp, severity.color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun FeatureChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceNavyHigh.copy(alpha = 0.8f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Reveals text character-by-character — used for the AI insight narrative. */
@Composable
fun TypewriterText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    var visibleChars by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (visibleChars < text.length) {
            delay(11)
            visibleChars = (visibleChars + 2).coerceAtMost(text.length)
        }
    }
    Text(text = text.take(visibleChars), style = style, color = color, modifier = modifier)
}
