package com.lukeneedham.androiddock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The fan's background on the layout page: a strip showing the gradient as it is now, from the
 * corner on the left to the outer edge on the right, and a button that opens [GradientEditorSheet].
 */
@Composable
internal fun BackgroundGradientRow(stops: List<ColorStop>, onChange: (List<ColorStop>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(R.string.bg_title))
        GradientStrip(stops, selected = null, modifier = Modifier.padding(top = 8.dp))
        FilledTonalButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(stringResource(R.string.bg_edit)) }
    }
    if (open) GradientEditorSheet(stops, onChange) { open = false }
}

private val STRIP_HEIGHT = 32.dp
private val MARKER_RADIUS = 9.dp
private val MARKER_AREA = 28.dp

/**
 * The gradient drawn along a strip, over grey so translucent colours can be judged. With a
 * [selected] stop it also shows a marker under the strip for each stop, and tapping near one
 * calls [onSelect] with it.
 */
@Composable
private fun GradientStrip(
    stops: List<ColorStop>,
    selected: Int?,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
) {
    val description = stringResource(R.string.bg_strip_description)
    val height = if (selected == null) STRIP_HEIGHT else STRIP_HEIGHT + MARKER_AREA
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(stops, onSelect) {
                if (onSelect == null) return@pointerInput
                detectTapGestures { tap ->
                    val nearest = stops.indices.minByOrNull { abs(stops[it].position * size.width - tap.x) }
                    if (nearest != null) onSelect(nearest)
                }
            },
    ) {
        val strip = Size(size.width, STRIP_HEIGHT.toPx())
        drawRect(Color.Gray, size = strip)
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = SheetGradient.expand(stops).map { it.position to Color(it.color) }.toTypedArray(),
                startX = 0f,
                endX = size.width,
            ),
            size = strip,
        )
        if (selected == null) return@Canvas
        val radius = MARKER_RADIUS.toPx()
        val y = STRIP_HEIGHT.toPx() + MARKER_AREA.toPx() / 2
        stops.forEachIndexed { index, stop ->
            // Kept inside the strip's ends, so a stop at 0 or 1 is not cut off.
            val x = (stop.position * size.width).coerceIn(radius, size.width - radius)
            val isSelected = index == selected
            drawCircle(Color(stop.color).copy(alpha = 1f), radius = radius, center = Offset(x, y))
            drawCircle(
                color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                radius = radius,
                center = Offset(x, y),
                style = Stroke(width = (if (isSelected) 3.dp else 1.dp).toPx()),
            )
            if (isSelected) {
                drawLine(Color.White, Offset(x, STRIP_HEIGHT.toPx()), Offset(x, y - radius), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

/**
 * The bottom sheet that edits the background gradient: the gradient with a marker for each stop,
 * then the selected stop's position and colour, and buttons to add and remove stops. One stop
 * makes the background a single flat colour. Every change is saved as it is made.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientEditorSheet(
    stops: List<ColorStop>,
    onChange: (List<ColorStop>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, stops.lastIndex)
    val stop = stops[index]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(stringResource(R.string.bg_editor_title), style = MaterialTheme.typography.titleMedium)
            GradientStrip(stops, selected = index, modifier = Modifier.padding(top = 12.dp), onSelect = { selected = it })
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.bg_corner), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.bg_edge), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                stringResource(R.string.bg_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                stringResource(R.string.stop_title, index + 1, stops.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(stringResource(R.string.stop_position, (stop.position * 100).roundToInt()), modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = stop.position,
                onValueChange = { position ->
                    // Moving past another stop changes the order; the selection follows the stop.
                    val (moved, at) = SheetGradient.moved(stops, index, position)
                    selected = at
                    onChange(moved)
                },
                valueRange = 0f..1f,
            )
            ColorSliders(stop.color) { color ->
                onChange(stops.mapIndexed { i, s -> if (i == index) s.copy(color = color) else s })
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        val (added, at) = SheetGradient.added(stops)
                        selected = at
                        onChange(added)
                    },
                    enabled = stops.size < SheetGradient.STOPS_MAX,
                ) { Text(stringResource(R.string.stop_add)) }
                OutlinedButton(
                    onClick = {
                        selected = index.coerceAtMost(stops.size - 2).coerceAtLeast(0)
                        onChange(SheetGradient.removed(stops, index))
                    },
                    enabled = stops.size > 1,
                ) { Text(stringResource(R.string.stop_remove)) }
            }
        }
    }
}
