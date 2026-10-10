package com.lukeneedham.androiddock

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The fan's layout, on a page of its own: a preview of the fan at the top, drawn with placeholder
 * circles for apps, and below it the controls for the rows. Every change shows in the preview as
 * it is made.
 */
@Composable
internal fun FanLayoutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var inner by remember { mutableIntStateOf(DockPrefs.getInnerOffset(context)) }
    var bottomPad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM)) }
    var sidePad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.SIDE)) }
    val rows = remember { mutableStateListOf<DockPrefs.Row>().apply { addAll(DockPrefs.getRows(context)) } }
    // The row the sliders edit, chosen by tapping it in the preview and tinted there.
    var selectedRow by remember { mutableIntStateOf(0) }
    var sheetColor by remember { mutableIntStateOf(DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET)) }
    var edgeColor by remember { mutableIntStateOf(DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET_EDGE)) }

    SubPage(R.string.fan_layout_title, onBack) {
        FanPreview(inner, bottomPad, sidePad, rows.toList(), selectedRow, sheetColor, edgeColor, onRowTap = { selectedRow = it })
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.rows_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(stringResource(R.string.slider_inner_offset, inner), modifier = Modifier.padding(top = 16.dp))
            Slider(
                value = inner.toFloat(),
                onValueChange = {
                    inner = it.toInt()
                    DockPrefs.setInnerOffset(context, inner)
                },
                valueRange = 0f..DockPrefs.INNER_MAX.toFloat(),
            )
            Text(stringResource(R.string.slider_padding_bottom, bottomPad), modifier = Modifier.padding(top = 16.dp))
            Slider(
                value = bottomPad.toFloat(),
                onValueChange = {
                    bottomPad = it.toInt()
                    DockPrefs.setPadding(context, DockPrefs.Padding.BOTTOM, bottomPad)
                },
                valueRange = 0f..DockPrefs.PADDING_MAX.toFloat(),
            )
            Text(stringResource(R.string.slider_padding_side, sidePad), modifier = Modifier.padding(top = 16.dp))
            Slider(
                value = sidePad.toFloat(),
                onValueChange = {
                    sidePad = it.toInt()
                    DockPrefs.setPadding(context, DockPrefs.Padding.SIDE, sidePad)
                },
                valueRange = 0f..DockPrefs.PADDING_MAX.toFloat(),
            )
            ColorSettingRow(context, DockPrefs.ColorSetting.SHEET, R.string.color_sheet_corner) { sheetColor = it }
            ColorSettingRow(context, DockPrefs.ColorSetting.SHEET_EDGE, R.string.color_sheet_edge) { edgeColor = it }
            RowsEditor(context, rows, inner, bottomPad, sidePad, selectedRow) { selectedRow = it }
        }
    }
}

private const val MAX_ICON_SIZE = 64
private const val MIN_ICON_SIZE = 16
/** The smallest icon size the slider offers, which keeps the ring's width above [DockPrefs.ROW_WIDTH_MIN]. */
private const val SLIDER_ICON_MIN = 28
private const val ROW_SPACING = 1.2f
private const val ICON_SPACING = 1.15f

/** The angles, from the bottom edge, between which a ring's icons of [size] can be centred. */
private fun arcAngles(size: Float, centreRadius: Float, bottom: Float, side: Float): Pair<Float, Float> {
    val from = asin(((bottom + size / 2f) / centreRadius).coerceAtMost(1f))
    val to = (PI / 2).toFloat() - asin(((side + size / 2f) / centreRadius).coerceAtMost(1f))
    return if (to >= from) from to to else (PI / 4).toFloat().let { it to it }
}

private fun slotsFit(n: Int, size: Float, centreRadius: Float, bottom: Float, side: Float): Boolean {
    val (from, to) = arcAngles(size, centreRadius, bottom, side)
    if (n == 1) return to >= from
    val step = (to - from) / (n - 1)
    return to > from && 2 * centreRadius * sin(step / 2) >= size * ICON_SPACING
}

/**
 * The icon size the sheet gives a ring: its width allows [thickness] / [ROW_SPACING], at most
 * [MAX_ICON_SIZE], shrunk until [n] icons fit along the arc, but never below [MIN_ICON_SIZE].
 * All lengths are in pixels at [density].
 */
private fun fitIconSize(n: Int, centreRadius: Float, thickness: Float, bottom: Float, side: Float, density: Float): Float {
    var size = min(MAX_ICON_SIZE * density, thickness / ROW_SPACING)
    while (size > MIN_ICON_SIZE * density && !slotsFit(n, size, centreRadius, bottom, side)) size -= 1f
    return size.coerceAtLeast(MIN_ICON_SIZE * density)
}

/** Distinct, fixed colours for the placeholder apps. */
private val PLACEHOLDER_COLORS = listOf(
    0xFFE57373, 0xFFFFB74D, 0xFFFFF176, 0xFF81C784, 0xFF4DB6AC, 0xFF4FC3F7,
    0xFF7986CB, 0xFFBA68C8, 0xFFF06292, 0xFFA1887F, 0xFF90A4AE, 0xFFAED581,
).map { Color(it) }

/**
 * The fan as the sheet would draw it with every row full: the same geometry as [DockActivity],
 * but with a coloured circle for each app. The selected ring is tinted white, and tapping a ring selects it. Sits in the bottom-right of a box, like the screen
 * corner.
 */
@Composable
internal fun FanPreview(
    inner: Int,
    bottomPad: Int,
    sidePad: Int,
    rows: List<DockPrefs.Row>,
    selectedRow: Int?,
    sheetColor: Int,
    edgeColor: Int,
    thumbnailDp: Int? = null,
    onRowTap: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .then(if (thumbnailDp != null) Modifier.size(thumbnailDp.dp).clip(RoundedCornerShape(8.dp)) else Modifier.fillMaxWidth())
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // As in the sheet: the gap plus the rows' widths is the radius, shrunk to fit the width.
        val total = inner + rows.sumOf { it.widthDp }
        // A thumbnail draws the whole fan at full size and shrinks it to fit its box.
        val radiusDp = if (thumbnailDp != null) total.toFloat().coerceAtLeast(1f)
        else min(total.toFloat(), maxWidth.value * 0.9f).coerceAtLeast(1f)
        Canvas(
            modifier = (if (thumbnailDp != null) Modifier.size(thumbnailDp.dp) else Modifier.width(maxWidth).height(radiusDp.dp))
                .align(Alignment.BottomEnd)
                // The circles are centred on the corner, so without this they spill out of the quarter.
                .clipToBounds()
                .pointerInput(onRowTap != null, inner, rows, radiusDp) {
                    if (onRowTap == null) return@pointerInput
                    detectTapGestures { tap ->
                        val distance = hypot(size.width - tap.x, size.height - tap.y)
                        val scale = radiusDp * density / (total * density)
                        var edge = inner * density * scale
                        rows.forEachIndexed { index, row ->
                            edge += row.widthDp * density * scale
                            if (distance <= edge) {
                                // A tap in the gap before row 1 selects row 1.
                                onRowTap(index)
                                return@detectTapGestures
                            }
                        }
                    }
                },
        ) {
            val dp = density
            val fit = if (thumbnailDp != null) thumbnailDp * dp / (radiusDp * dp) else 1f
            withTransform({ scale(fit, fit, pivot = Offset(size.width, size.height)) }) {
            val radius = radiusDp * dp
            val corner = Offset(size.width, size.height)
            val scale = radius / (total * dp)

            val quarter = Path().apply { addOval(androidx.compose.ui.geometry.Rect(corner, radius)) }
            clipPath(quarter) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(sheetColor), Color(edgeColor)),
                        center = corner,
                        radius = radius,
                    ),
                    radius = radius,
                    center = corner,
                )
            }

            var edge = inner * dp * scale
            var colorIndex = 0
            rows.forEachIndexed { rowIndex, row ->
                val start = edge
                edge += row.widthDp * dp * scale
                val end = edge
                val centreRadius = (start + end) / 2
                if (rowIndex == selectedRow) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = centreRadius,
                        center = corner,
                        style = Stroke(width = end - start),
                    )
                }

                val bottom = bottomPad * dp
                val side = sidePad * dp
                val n = row.count
                val size = fitIconSize(n, centreRadius, end - start, bottom, side, dp)
                val (from, to) = arcAngles(size, centreRadius, bottom, side)
                repeat(n) { slot ->
                    val angle = if (n == 1) (from + to) / 2 else from + (to - from) * slot / (n - 1)
                    drawCircle(
                        color = PLACEHOLDER_COLORS[colorIndex++ % PLACEHOLDER_COLORS.size],
                        radius = size / 2,
                        center = Offset(corner.x - centreRadius * cos(angle), corner.y - centreRadius * sin(angle)),
                    )
                }
            }
            }
        }
    }
}

/**
 * Edits the sheet's rings: one set of controls for the [selected] ring, which is chosen by tapping it
 * in the preview. Delete acts on the selected ring too.
 */
@Composable
private fun RowsEditor(
    context: Context,
    rows: MutableList<DockPrefs.Row>,
    inner: Int,
    bottomPad: Int,
    sidePad: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val delete = stringResource(R.string.row_delete)
    fun save() = DockPrefs.setRows(context, rows.toList())

    val index = selected.coerceIn(0, rows.lastIndex)
    val row = rows[index]
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.row_title, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        rows.removeAt(index)
                        onSelect(index.coerceAtMost(rows.lastIndex))
                        save()
                    },
                    enabled = rows.size > 1,
                ) { Text("✕", modifier = Modifier.semantics { contentDescription = delete }) }
            }
            Text(stringResource(R.string.row_count, row.count))
            Slider(
                value = row.count.toFloat(),
                onValueChange = {
                    if (it.toInt() != row.count) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    rows[index] = row.copy(count = it.toInt())
                    save()
                },
                valueRange = DockPrefs.ROW_COUNT_MIN.toFloat()..DockPrefs.ROW_COUNT_MAX.toFloat(),
                steps = DockPrefs.ROW_COUNT_MAX - DockPrefs.ROW_COUNT_MIN - 1,
            )
            // A ring's width is its icon size plus margin; the size is what the user sets.
            val iconSize = (row.widthDp / ROW_SPACING).toInt().coerceIn(SLIDER_ICON_MIN, MAX_ICON_SIZE)
            Text(stringResource(R.string.row_icon_size, iconSize))
            Slider(
                value = iconSize.toFloat(),
                onValueChange = {
                    rows[index] = row.copy(
                        widthDp = (it.toInt() * ROW_SPACING).roundToInt()
                            .coerceIn(DockPrefs.ROW_WIDTH_MIN, DockPrefs.ROW_WIDTH_MAX),
                    )
                    save()
                },
                valueRange = SLIDER_ICON_MIN.toFloat()..MAX_ICON_SIZE.toFloat(),
                steps = MAX_ICON_SIZE - SLIDER_ICON_MIN - 1,
            )
            // Mirror the sheet's geometry in dp (density 1) to tell when the icons are squeezed.
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val total = inner + rows.sumOf { it.widthDp }
            val scale = min(total.toFloat(), screenWidth * 0.9f) / total
            val start = (inner + rows.take(index).sumOf { it.widthDp }) * scale
            val thickness = row.widthDp * scale
            val centre = start + thickness / 2
            val shown = fitIconSize(row.count, centre, thickness, bottomPad.toFloat(), sidePad.toFloat(), 1f)
            val wanted = min(MAX_ICON_SIZE.toFloat(), thickness / ROW_SPACING)
            if (shown < wanted - 0.5f) {
                val fitting = (1..row.count).lastOrNull {
                    slotsFit(it, wanted, centre, bottomPad.toFloat(), sidePad.toFloat())
                } ?: 0
                Text(
                    stringResource(R.string.row_fit_warning, fitting, shown.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (rows.size < DockPrefs.ROWS_MAX) {
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                rows.add(DockPrefs.newRow())
                onSelect(rows.lastIndex)
                save()
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        ) { Text(stringResource(R.string.row_add)) }
    }
}
