package com.lukeneedham.androiddock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val THUMB_RADIUS = 10.dp
private val TRACK_HEIGHT = 4.dp

/**
 * A horizontal slider, 48dp tall. Dragging or tapping anywhere on it sets the value; with [steps]
 * stops between the ends, the value snaps to them and a tick marks each.
 */
@Composable
internal fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = AppTheme.current.colors
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    var widthPx by remember { mutableIntStateOf(0) }
    val span = valueRange.endInclusive - valueRange.start

    fun fractionOf(v: Float) = if (span == 0f) 0f else ((v - valueRange.start) / span).coerceIn(0f, 1f)

    fun valueAt(x: Float, thumbPx: Float): Float {
        val fraction = ((x - thumbPx) / (widthPx - 2 * thumbPx)).coerceIn(0f, 1f)
        val snapped = if (steps > 0) (fraction * (steps + 1)).roundToInt() / (steps + 1f) else fraction
        return valueRange.start + snapped * span
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                setProgress { target ->
                    latestOnValueChange(target.coerceIn(valueRange))
                    true
                }
            }
            .pointerInput(valueRange, steps) {
                val thumbPx = THUMB_RADIUS.toPx()
                detectTapGestures { latestOnValueChange(valueAt(it.x, thumbPx)) }
            }
            .pointerInput(valueRange, steps) {
                val thumbPx = THUMB_RADIUS.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { latestOnValueChange(valueAt(it.x, thumbPx)) },
                    onHorizontalDrag = { change, _ -> latestOnValueChange(valueAt(change.position.x, thumbPx)) },
                )
            },
    ) {
        val thumb = THUMB_RADIUS.toPx()
        val track = TRACK_HEIGHT.toPx()
        val centreY = size.height / 2
        val start = thumb
        val end = size.width - thumb
        val thumbX = start + (end - start) * fractionOf(value)
        drawLine(colors.sliderInactiveTrack, Offset(start, centreY), Offset(end, centreY), track, StrokeCap.Round)
        drawLine(colors.sliderActiveTrack, Offset(start, centreY), Offset(thumbX, centreY), track, StrokeCap.Round)
        if (steps > 0) {
            repeat(steps + 2) { i ->
                val x = start + (end - start) * i / (steps + 1)
                drawCircle(if (x <= thumbX) colors.sliderActiveTick else colors.sliderInactiveTick, track / 2, Offset(x, centreY))
            }
        }
        drawCircle(colors.sliderThumb, thumb, Offset(thumbX, centreY))
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSliderPreview() = AppTheme {
    AppSlider(value = 3f, onValueChange = {}, valueRange = 0f..10f, steps = 9)
}
