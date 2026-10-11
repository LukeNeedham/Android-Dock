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
import kotlin.math.sin

/**
 * The fan's layout, on a page of its own: a preview of the fan at the top, drawn with placeholder
 * circles for apps, and below it the controls for the rows. Every change shows in the preview as
 * it is made.
 */
@Composable
internal fun FanLayoutScreen(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    var inner by remember { mutableIntStateOf(DockPrefs.getInnerOffset(context)) }
    var bottomPad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM)) }
    var sidePad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.SIDE)) }
    val rows = remember { mutableStateListOf<DockPrefs.Row>().apply { addAll(DockPrefs.getRows(context)) } }
    // The row the sliders edit, chosen by tapping it in the preview and tinted there.
    var selectedRow by remember { mutableIntStateOf(0) }
    var sheetColor by remember { mutableIntStateOf(DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET)) }
    var edgeColor by remember { mutableIntStateOf(DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET_EDGE)) }

    SubPage(R.string.fan_layout_title, onBack, footer, header) {
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
            RowsEditor(context, rows, selectedRow) { selectedRow = it }
            NavBarSpacer()
        }
    }
}

private const val MAX_ICON_SIZE = 64
private const val MIN_ICON_SIZE = 16
private const val ROW_SPACING = 1.2f
private const val ICON_SPACING = 1.15f

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
                fun arc(size: Float): Pair<Float, Float> {
                    val from = asin(((bottom + size / 2f) / centreRadius).coerceAtMost(1f))
                    val to = (PI / 2).toFloat() - asin(((side + size / 2f) / centreRadius).coerceAtMost(1f))
                    return if (to >= from) from to to else (PI / 4).toFloat().let { it to it }
                }

                val n = row.count
                var size = min(MAX_ICON_SIZE * dp, (end - start) / ROW_SPACING)
                while (size > MIN_ICON_SIZE * dp) {
                    val (from, to) = arc(size)
                    val fits = if (n == 1) to >= from else {
                        val step = (to - from) / (n - 1)
                        to > from && 2 * centreRadius * sin(step / 2) >= size * ICON_SPACING
                    }
                    if (fits) break
                    size -= 1f
                }
                size = size.coerceAtLeast(MIN_ICON_SIZE * dp)
                val (from, to) = arc(size)
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
private fun RowsEditor(context: Context, rows: MutableList<DockPrefs.Row>, selected: Int, onSelect: (Int) -> Unit) {
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
            Text(stringResource(R.string.row_width, row.widthDp))
            Slider(
                value = row.widthDp.toFloat(),
                onValueChange = {
                    rows[index] = row.copy(widthDp = it.toInt())
                    save()
                },
                valueRange = DockPrefs.ROW_WIDTH_MIN.toFloat()..DockPrefs.ROW_WIDTH_MAX.toFloat(),
            )
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
