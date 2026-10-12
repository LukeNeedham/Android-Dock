package com.lukeneedham.androiddock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The fan's background on the layout page: a small square showing the gradient as the fan will
 * draw it, and a button that opens [GradientEditorSheet].
 */
@Composable
internal fun BackgroundGradientRow(
    stops: List<ColorStop>,
    corner: DockPrefs.Corner,
    onChange: (List<ColorStop>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(R.string.bg_title))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            GradientBox(stops, corner, Modifier.size(112.dp))
            FilledTonalButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.bg_edit))
            }
        }
    }
    if (open) GradientEditorSheet(stops, corner, onChange) { open = false }
}

private val HANDLE_RADIUS = 11.dp
private val SELECTED_HANDLE_RADIUS = 15.dp
private val HANDLE_REACH = 36.dp

/** The room an editable box leaves round itself, so a handle at its corner is whole and can be touched. */
private val HANDLE_MARGIN = 28.dp

/** Where the gradient's radius ends, as a fraction of the diagonal of the box. */
private const val GRADIENT_REACH = 0.9f

/**
 * Where the stops sit in a [GradientBox]: the box is a square of the given [side] whose top left
 * is at ([margin], [margin]) in the drawing, and its stops lie along the diagonal from the
 * [corner] the gradient is centred on, through the middle of the box. A stop's position is its
 * distance along that line as a fraction of [radius], which is the gradient's radius. The line
 * starts at the corner, so a handle at position 0 sits on it, half outside the box.
 */
private class DiagonalGeometry(val side: Float, corner: DockPrefs.Corner, val margin: Float) {
    val origin = Offset(margin + if (corner.end) side else 0f, margin + if (corner.bottom) side else 0f)
    private val direction = Offset(if (corner.end) -1f else 1f, if (corner.bottom) -1f else 1f) / sqrt(2f)
    val radius = side * sqrt(2f) * GRADIENT_REACH

    /** The point on the line for [position]. */
    fun point(position: Float): Offset = origin + direction * (position * radius)

    /** How far along the line [at] is, as a position that may be outside 0 to 1. */
    fun projection(at: Offset): Float {
        val relative = at - origin
        return (relative.x * direction.x + relative.y * direction.y) / radius
    }
}

/**
 * The stop whose handle is within [reach] of [at], the closest one. Where handles are stacked,
 * one that is not [selected] wins, so touching the stack again moves on to the next stop.
 */
private fun nearestStop(
    stops: List<ColorStop>,
    selected: Int?,
    geometry: DiagonalGeometry,
    at: Offset,
    reach: Float,
): Int? =
    stops.indices
        .map { it to (geometry.point(stops[it].position) - at).getDistance() }
        .filter { it.second <= reach }
        .minWithOrNull(compareBy<Pair<Int, Float>>({ it.second }, { if (it.first == selected) 1 else 0 }))
        ?.first

/**
 * The background gradient drawn as the fan draws it: a quarter circle centred on the [corner] of
 * a square box, over grey so translucent colours can be judged.
 *
 * With [onChange], the stops can be edited here. Each stop has a numbered handle on the diagonal
 * through the box, which is dragged along it to change the stop's position. Touching a handle
 * selects its stop ([selected], reported to [onSelect]).
 */
@Composable
private fun GradientBox(
    stops: List<ColorStop>,
    corner: DockPrefs.Corner,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: (Int) -> Unit = {},
    onChange: ((List<ColorStop>) -> Unit)? = null,
) {
    // An editable box has room round it for its handles, which can sit outside the box itself.
    val margin = if (onChange == null) 0.dp else HANDLE_MARGIN
    // The gesture below must outlive the recompositions its own changes cause.
    val latestStops by rememberUpdatedState(stops)
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnChange by rememberUpdatedState(onChange)
    val description = stringResource(R.string.bg_box_description)
    val textMeasurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .then(
                if (onChange == null) Modifier else Modifier.pointerInput(corner) {
                    val reach = HANDLE_REACH.toPx()
                    val marginPx = margin.toPx()
                    awaitEachGesture {
                        val geometry = DiagonalGeometry(size.width - 2 * marginPx, corner, marginPx)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var active = nearestStop(latestStops, latestSelected, geometry, down.position, reach)
                            ?: return@awaitEachGesture
                        down.consume()
                        latestOnSelect(active)
                        // The stop follows the finger's movement, not the finger itself, so it does
                        // not jump when a handle held off the line is grabbed.
                        val startPosition = latestStops[active].position
                        val startProjection = geometry.projection(down.position)
                        // The list is kept here as well, as a drag can report twice before the
                        // screen has recomposed with the first change.
                        var working = latestStops
                        drag(down.id) { change ->
                            change.consume()
                            val position = startPosition + geometry.projection(change.position) - startProjection
                            val (moved, at) = SheetGradient.moved(working, active, position)
                            working = moved
                            active = at
                            latestOnSelect(at)
                            latestOnChange?.invoke(moved)
                        }
                    }
                },
            ),
    ) {
        val geometry = DiagonalGeometry(size.width - 2 * margin.toPx(), corner, margin.toPx())
        val box = Rect(Offset(geometry.margin, geometry.margin), Size(geometry.side, geometry.side))
        // Only the gradient is cut to the box; the handles are drawn over its edge.
        clipPath(Path().apply { addRoundRect(RoundRect(box, CornerRadius(12.dp.toPx()))) }) {
            drawRect(Color.Gray, box.topLeft, box.size)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = SheetGradient.expand(stops).map { it.position to Color(it.color) }.toTypedArray(),
                    center = geometry.origin,
                    radius = geometry.radius,
                ),
                radius = geometry.radius,
                center = geometry.origin,
            )
        }
        if (onChange == null) return@Canvas
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = geometry.origin,
            end = geometry.point(1f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
        )
        // The selected handle last, so it is on top of any it is stacked with.
        stops.indices.sortedBy { it == selected }.forEach { index ->
            drawHandle(textMeasurer, accent, index + 1, stops[index], geometry.point(stops[index].position), index == selected)
        }
    }
}

/**
 * A stop's handle: its colour (over grey) with its number in it. It always has a border, a white
 * ring between two dark ones, so it shows against any colour behind it. The selected handle is
 * bigger and has an [accent] ring round it as well.
 */
private fun DrawScope.drawHandle(
    textMeasurer: TextMeasurer,
    accent: Color,
    number: Int,
    stop: ColorStop,
    centre: Offset,
    isSelected: Boolean,
) {
    val radius = (if (isSelected) SELECTED_HANDLE_RADIUS else HANDLE_RADIUS).toPx()
    val ring = (if (isSelected) 3.dp else 2.dp).toPx()
    val dark = Color.Black.copy(alpha = 0.75f)
    if (isSelected) {
        drawCircle(accent, radius + 7.dp.toPx(), centre, style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.35f), radius + 7.dp.toPx(), centre)
    }
    drawCircle(Color.Gray, radius, centre)
    drawCircle(Color(stop.color), radius, centre)
    drawCircle(dark, radius + ring / 2 + 1.dp.toPx() / 2, centre, style = Stroke(width = 1.dp.toPx()))
    drawCircle(Color.White, radius, centre, style = Stroke(width = ring))
    drawCircle(dark, radius - ring / 2 - 1.dp.toPx() / 2, centre, style = Stroke(width = 1.dp.toPx()))

    // The number is dark or light, whichever shows better on the colour over the grey.
    val shown = ColorUtils.compositeColors(stop.color, Color.Gray.toArgb())
    val label = textMeasurer.measure(
        number.toString(),
        TextStyle(
            fontSize = if (isSelected) 14.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (ColorUtils.calculateLuminance(shown) > 0.5) Color.Black else Color.White,
        ),
    )
    drawText(label, topLeft = centre - Offset(label.size.width / 2f, label.size.height / 2f))
}

/**
 * The bottom sheet that edits the background gradient: the gradient in a box with a handle for each
 * stop to drag along the diagonal, then the selected stop's colour, and buttons to add and remove
 * stops. One stop makes the background a single flat colour. Every change is saved as it is made.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientEditorSheet(
    stops: List<ColorStop>,
    corner: DockPrefs.Corner,
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
            GradientBox(
                stops, corner,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                selected = index,
                onSelect = { selected = it },
                onChange = onChange,
            )
            Text(
                stringResource(R.string.stop_title, index + 1, stops.size, (stop.position * 100).roundToInt()),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
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
