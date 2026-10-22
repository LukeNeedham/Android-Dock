package com.lukeneedham.androiddock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lukeneedham.common.debug.scale
import com.lukeneedham.common.ui.LocalAnimationSpeed

/** A spinner for something that is loading: an arc that goes round once a second. */
@Composable
internal fun AppProgress(modifier: Modifier = Modifier) {
    val colors = AppTheme.current.colors
    val turn = rememberInfiniteTransition(label = "progress").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(LocalAnimationSpeed.current.scale(1000), easing = LinearEasing), RepeatMode.Restart),
        label = "progress turn",
    )
    Canvas(modifier = modifier.size(40.dp)) {
        val stroke = 4.dp.toPx()
        drawArc(
            color = colors.accent,
            startAngle = turn.value,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppProgressPreview() = AppTheme { AppProgress() }
