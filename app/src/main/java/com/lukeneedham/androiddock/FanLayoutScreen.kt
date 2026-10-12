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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.max
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
    val corner = DockPrefs.getCorner(context)
    var inner by remember { mutableIntStateOf(DockPrefs.getCurrentWidth(context)) }
    var showCurrent by remember { mutableStateOf(DockPrefs.getShowCurrent(context)) }
    val currentWidth = inner.coerceAtLeast(DockPrefs.currentWidthMin(showCurrent))
    var bottomPad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM)) }
    var sidePad by remember { mutableIntStateOf(DockPrefs.getPadding(context, DockPrefs.Padding.SIDE)) }
    val rows = remember { mutableStateListOf<DockPrefs.Row>().apply { addAll(DockPrefs.getRows(context)) } }
    // The part of the fan the controls edit, chosen in the preview or the list above the controls,
    // and tinted in the preview.
    var selected by remember { mutableStateOf<FanPart>(FanPart.Ring(0)) }
    var stops by remember { mutableStateOf(DockPrefs.getSheetStops(context)) }

    SubPage(R.string.fan_layout_title, onBack, footer, header) {
        FanPreview(currentWidth, bottomPad, sidePad, rows.toList(), selected, stops, corner = corner, showCurrent = showCurrent, onSelect = { selected = it })
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
            PartList(rows.size, corner, selected, onSelect = { selected = it })
            RowsEditor(
                context, rows, selected, corner,
                bottomPad = bottomPad,
                sidePad = sidePad,
                onBottomPad = {
                    bottomPad = it
                    DockPrefs.setPadding(context, DockPrefs.Padding.BOTTOM, it)
                },
                onSidePad = {
                    sidePad = it
                    DockPrefs.setPadding(context, DockPrefs.Padding.SIDE, it)
                },
                currentWidth = currentWidth,
                showCurrent = showCurrent,
                onCurrentWidth = {
                    inner = it
                    DockPrefs.setCurrentWidth(context, it)
                },
                onShowCurrent = {
                    showCurrent = it
                    DockPrefs.setShowCurrent(context, it)
                },
            ) { selected = it }
            BackgroundGradientRow(stops, corner) {
                stops = it
                DockPrefs.setSheetStops(context, it)
            }
            NavBarSpacer()
        }
    }
}

/** A part of the fan that the controls can edit. */
internal sealed interface FanPart {
    /** A ring: -1 is the corner ring for the current app, 0 the first of the rows. */
    data class Ring(val index: Int) : FanPart

    /** The gap between the fan and the corner's horizontal edge. */
    data object BottomMargin : FanPart

    /** The gap between the fan and the corner's vertical edge. */
    data object SideMargin : FanPart
}

private val MIN_TOUCH = 48.dp
private val GRIP_LENGTH = 40.dp
private val GRIP_THICKNESS = 14.dp

/** Every part that can be selected, in a row that scrolls: tapping one selects it. */
@Composable
private fun PartList(ringCount: Int, corner: DockPrefs.Corner, selected: FanPart, onSelect: (FanPart) -> Unit) {
    val bottomLabel = stringResource(R.string.part_margin, stringResource(corner.verticalEdge)).replaceFirstChar { it.uppercase() }
    val sideLabel = stringResource(R.string.part_margin, stringResource(corner.horizontalEdge)).replaceFirstChar { it.uppercase() }
    val cornerLabel = stringResource(R.string.row_current_title)
    val ringLabels = (0 until ringCount).map { stringResource(R.string.row_title, it + 1) }
    val parts = buildList {
        add(FanPart.Ring(-1) to cornerLabel)
        ringLabels.forEachIndexed { i, label -> add(FanPart.Ring(i) to label) }
        add(FanPart.BottomMargin to bottomLabel)
        add(FanPart.SideMargin to sideLabel)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
    ) {
        parts.forEach { (part, label) ->
            val on = part == selected
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(part) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

private const val MAX_ICON_SIZE = 64
private const val MIN_ICON_SIZE = DockPrefs.ICON_SIZE_MIN
private const val ROW_SPACING = 1.2f
private const val ICON_SPACING = 1.15f

/** Distinct, fixed colours for the placeholder apps. */
private val PLACEHOLDER_COLORS = listOf(
    0xFFE57373, 0xFFFFB74D, 0xFFFFF176, 0xFF81C784, 0xFF4DB6AC, 0xFF4FC3F7,
    0xFF7986CB, 0xFFBA68C8, 0xFFF06292, 0xFFA1887F, 0xFF90A4AE, 0xFFAED581,
).map { Color(it) }

/**
 * The fan as the sheet would draw it with every row full: the same geometry as [DockActivity],
 * but with a coloured circle for each app. The selected ring is tinted white, and tapping a ring selects it. Sits in the chosen corner of a box, like the screen
 * corner.
 */
@Composable
internal fun FanPreview(
    inner: Int,
    bottomPad: Int,
    sidePad: Int,
    rows: List<DockPrefs.Row>,
    selected: FanPart?,
    stops: List<ColorStop>,
    corner: DockPrefs.Corner = DockPrefs.Corner.BOTTOM_RIGHT,
    showCurrent: Boolean = true,
    thumbnailDp: Int? = null,
    onSelect: ((FanPart) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .then(if (thumbnailDp != null) Modifier.size(thumbnailDp.dp).clip(RoundedCornerShape(8.dp)) else Modifier.fillMaxWidth())
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // Row 0, flush against the corner, holds a single icon: the app on screen, or nothing when
        // that is hidden. It is drawn like every other ring.
        val ringZero = if (inner > 0) listOf(DockPrefs.Row(if (showCurrent) 1 else 0, inner)) else emptyList()
        val allRows = ringZero + rows
        // As in the sheet: the rows' widths, row 0's included, make the radius, shrunk to fit the width.
        val total = allRows.sumOf { it.widthDp }
        // A thumbnail draws the whole fan at full size and shrinks it to fit its box.
        val radiusDp = if (thumbnailDp != null) total.toFloat().coerceAtLeast(1f)
        else min(total.toFloat(), maxWidth.value * 0.9f).coerceAtLeast(1f)
        // The rings are centred on the corner moved in by the padding, but the background is
        // centred on the corner itself, and reaches far enough to hold the outermost ring.
        val backgroundDp = radiusDp + hypot(sidePad.toFloat(), bottomPad.toFloat())
        Canvas(
            modifier = (if (thumbnailDp != null) Modifier.size(thumbnailDp.dp) else Modifier.width(maxWidth).height(backgroundDp.dp))
                .align(
                    when (corner) {
                        DockPrefs.Corner.BOTTOM_RIGHT -> Alignment.BottomEnd
                        DockPrefs.Corner.BOTTOM_LEFT -> Alignment.BottomStart
                        DockPrefs.Corner.TOP_LEFT -> Alignment.TopStart
                        DockPrefs.Corner.TOP_RIGHT -> Alignment.TopEnd
                    },
                )
                // The circles are centred on the corner, so without this they spill out of the quarter.
                .clipToBounds()
                .pointerInput(onSelect != null, allRows, radiusDp, bottomPad, sidePad, corner) {
                    if (onSelect == null) return@pointerInput
                    detectTapGestures { tap ->
                        // The margins can be nothing wide, so their grips are tapped first, and
                        // are at least a finger wide.
                        val grips = gripCentres(size.width.toFloat(), size.height.toFloat(), density, corner, radiusDp, bottomPad, sidePad)
                        val half = MIN_TOUCH.toPx() / 2
                        fun hit(c: Offset) = kotlin.math.abs(tap.x - c.x) <= half && kotlin.math.abs(tap.y - c.y) <= half
                        if (hit(grips.first)) { onSelect(FanPart.BottomMargin); return@detectTapGestures }
                        if (hit(grips.second)) { onSelect(FanPart.SideMargin); return@detectTapGestures }
                        val inBottomBand = if (corner.bottom) tap.y >= size.height - bottomPad * density else tap.y <= bottomPad * density
                        val inSideBand = if (corner.end) tap.x >= size.width - sidePad * density else tap.x <= sidePad * density
                        if (inBottomBand && bottomPad > 0) { onSelect(FanPart.BottomMargin); return@detectTapGestures }
                        if (inSideBand && sidePad > 0) { onSelect(FanPart.SideMargin); return@detectTapGestures }
                        val centreX = if (corner.end) size.width - sidePad * density else sidePad * density
                        val centreY = if (corner.bottom) size.height - bottomPad * density else bottomPad * density
                        val distance = hypot(centreX - tap.x, centreY - tap.y)
                        val scale = radiusDp * density / (total * density)
                        var edge = 0f
                        allRows.forEachIndexed { index, row ->
                            edge += row.widthDp * density * scale
                            if (distance <= edge) {
                                onSelect(FanPart.Ring(index - ringZero.size))
                                return@detectTapGestures
                            }
                        }
                    }
                },
        ) {
            val dp = density
            // The screen's corner, which the background is centred on, and the corner moved in by
            // the padding, which the rings are centred on.
            // dirX and dirY point from the screen's corner into the screen.
            val dirX = if (corner.end) -1f else 1f
            val dirY = if (corner.bottom) -1f else 1f
            val screenCorner = Offset(if (corner.end) size.width else 0f, if (corner.bottom) size.height else 0f)
            val ringCentre = Offset(screenCorner.x + dirX * sidePad * dp, screenCorner.y + dirY * bottomPad * dp)
            // The square of side r that has the given corner point as its corner of the screen.
            fun quarter(origin: Offset, r: Float) = androidx.compose.ui.geometry.Rect(
                if (corner.end) origin.x - r else origin.x,
                if (corner.bottom) origin.y - r else origin.y,
                if (corner.end) origin.x else origin.x + r,
                if (corner.bottom) origin.y else origin.y + r,
            )
            val radius = radiusDp * dp
            val backgroundRadius = backgroundDp * dp
            val fit = if (thumbnailDp != null) thumbnailDp / backgroundDp else 1f
            withTransform({
                scale(fit, fit, pivot = screenCorner)
                // The quarter the background fills, before it is scaled.
                quarter(screenCorner, backgroundRadius).let { clipRect(it.left, it.top, it.right, it.bottom) }
            }) {
            val scale = radius / (total * dp)

            val quarter = Path().apply { addOval(androidx.compose.ui.geometry.Rect(screenCorner, backgroundRadius)) }
            clipPath(quarter) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = SheetGradient.expand(stops).map { it.position to Color(it.color) }.toTypedArray(),
                        center = screenCorner,
                        radius = backgroundRadius,
                    ),
                    radius = backgroundRadius,
                    center = screenCorner,
                )
            }

            var edge = 0f
            allRows.forEachIndexed { index, row ->
                val rowIndex = index - ringZero.size
                val start = edge
                edge += row.widthDp * dp * scale
                val end = edge
                val centreRadius = (start + end) / 2
                if (selected == FanPart.Ring(rowIndex)) {
                    // Only the quarter the rings fill, not the padding.
                    val fill = quarter(ringCentre, radius)
                    clipRect(fill.left, fill.top, fill.right, fill.bottom) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = centreRadius,
                            center = ringCentre,
                            style = Stroke(width = end - start),
                        )
                    }
                }

                if (row.count == 0) return@forEachIndexed

                // The angles of the first and last icon, which cross over when the icons are too big
                // for the ring: then the icon does not fit.
                fun arc(size: Float): Pair<Float, Float> {
                    val inset = asin((size / 2f / centreRadius).coerceAtMost(1f))
                    return inset to (PI / 2).toFloat() - inset
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
                // Even the smallest icon is too big: put it on the diagonal.
                val (from, to) = arc(size).let { (a, b) ->
                    if (b >= a) a to b else (PI / 4).toFloat().let { it to it }
                }
                repeat(n) { slot ->
                    val angle = if (n == 1) (from + to) / 2 else from + (to - from) * slot / (n - 1)
                    drawCircle(
                        // Each row starts from its own place in the palette, so the colours of a row
                        // do not depend on how many icons the rows before it hold.
                        color = PLACEHOLDER_COLORS[(index * 5 + slot) % PLACEHOLDER_COLORS.size],
                        radius = size / 2,
                        center = Offset(ringCentre.x + dirX * centreRadius * cos(angle), ringCentre.y + dirY * centreRadius * sin(angle)),
                    )
                }
            }
            }
            if (onSelect != null) drawMarginGrips(corner, radiusDp, bottomPad, sidePad, selected)
        }
    }
}

/** Centres of the bottom and side margin grips, in the canvas's pixels. */
private fun gripCentres(width: Float, height: Float, density: Float, corner: DockPrefs.Corner, radiusDp: Float, bottomPad: Int, sidePad: Int): Pair<Offset, Offset> {
    val gripLength = GRIP_LENGTH.value * density
    val thickness = GRIP_THICKNESS.value * density
    // Halfway along the fan's reach, and in the middle of the margin or, when that is thin, inside the canvas.
    val along = radiusDp * density / 2
    val bottomDepth = max(bottomPad * density / 2, thickness / 2)
    val sideDepth = max(sidePad * density / 2, thickness / 2)
    val x = (if (corner.end) width - along else along).coerceIn(gripLength / 2, width - gripLength / 2)
    val y = (if (corner.bottom) height - along else along).coerceIn(gripLength / 2, height - gripLength / 2)
    val bottom = Offset(x, if (corner.bottom) height - bottomDepth else bottomDepth)
    val side = Offset(if (corner.end) width - sideDepth else sideDepth, y)
    return bottom to side
}

/**
 * Shows that the margins exist and can be tapped: a band over each one, as thick as the margin,
 * and a grip on it. The selected one is brighter.
 */
private fun DrawScope.drawMarginGrips(corner: DockPrefs.Corner, radiusDp: Float, bottomPad: Int, sidePad: Int, selected: FanPart?) {
    val (bottom, side) = gripCentres(size.width, size.height, density, corner, radiusDp, bottomPad, sidePad)
    val gripLength = GRIP_LENGTH.toPx()
    val thickness = GRIP_THICKNESS.toPx()
    fun band(horizontal: Boolean, pad: Int, grip: Offset, on: Boolean) {
        val depth = pad * density
        val tint = Color.White.copy(alpha = if (on) 0.4f else 0.15f)
        if (depth > 0f) {
            if (horizontal) drawRect(tint, Offset(0f, if (corner.bottom) size.height - depth else 0f), androidx.compose.ui.geometry.Size(size.width, depth))
            else drawRect(tint, Offset(if (corner.end) size.width - depth else 0f, 0f), androidx.compose.ui.geometry.Size(depth, size.height))
        }
        val w = if (horizontal) gripLength else thickness
        val h = if (horizontal) thickness else gripLength
        val topLeft = Offset(grip.x - w / 2, grip.y - h / 2)
        val sz = androidx.compose.ui.geometry.Size(w, h)
        val round = androidx.compose.ui.geometry.CornerRadius(thickness / 2)
        drawRoundRect(if (on) Color.White else Color.White.copy(alpha = 0.6f), topLeft, sz, round)
        drawRoundRect(Color.Black.copy(alpha = 0.5f), topLeft, sz, round, style = Stroke(width = 1.5f * density))
        // Two short lines along the grip, so it reads as something to hold.
        for (i in -1..1 step 2) {
            val o = i * thickness / 5
            val a = if (horizontal) Offset(grip.x - gripLength / 4, grip.y + o) else Offset(grip.x + o, grip.y - gripLength / 4)
            val b = if (horizontal) Offset(grip.x + gripLength / 4, grip.y + o) else Offset(grip.x + o, grip.y + gripLength / 4)
            drawLine(Color.Black.copy(alpha = 0.5f), a, b, strokeWidth = 1.5f * density)
        }
    }
    band(true, bottomPad, bottom, selected == FanPart.BottomMargin)
    band(false, sidePad, side, selected == FanPart.SideMargin)
}

/**
 * Edits the sheet's rings: one set of controls for the [selected] ring, which is chosen by tapping it
 * in the preview. Delete acts on the selected ring too.
 */
@Composable
private fun RowsEditor(
    context: Context,
    rows: MutableList<DockPrefs.Row>,
    selectedPart: FanPart,
    corner: DockPrefs.Corner,
    bottomPad: Int,
    sidePad: Int,
    onBottomPad: (Int) -> Unit,
    onSidePad: (Int) -> Unit,
    currentWidth: Int,
    showCurrent: Boolean,
    onCurrentWidth: (Int) -> Unit,
    onShowCurrent: (Boolean) -> Unit,
    onSelect: (FanPart) -> Unit,
) {
    val selected = (selectedPart as? FanPart.Ring)?.index
    val haptic = LocalHapticFeedback.current
    val delete = stringResource(R.string.row_delete)
    fun save() = DockPrefs.setRows(context, rows.toList())

    if (selected == null) {
        val bottom = selectedPart == FanPart.BottomMargin
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                val edge = stringResource(if (bottom) corner.verticalEdge else corner.horizontalEdge)
                val pad = if (bottom) bottomPad else sidePad
                Text(
                    stringResource(R.string.part_margin, edge).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(stringResource(R.string.slider_padding, edge, pad))
                Slider(
                    value = pad.toFloat(),
                    onValueChange = { if (bottom) onBottomPad(it.toInt()) else onSidePad(it.toInt()) },
                    valueRange = 0f..DockPrefs.PADDING_MAX.toFloat(),
                )
            }
        }
    } else if (selected < 0) {
        // The corner ring: its width, and whether it shows the app on screen. Nothing else.
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Text(
                    stringResource(R.string.row_current_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showCurrent, onCheckedChange = onShowCurrent)
                    Text(stringResource(R.string.row_show_current))
                }
                Text(stringResource(R.string.row_width, currentWidth))
                Slider(
                    value = currentWidth.toFloat(),
                    onValueChange = { onCurrentWidth(it.toInt()) },
                    valueRange = DockPrefs.currentWidthMin(showCurrent).toFloat()..DockPrefs.CURRENT_WIDTH_MAX.toFloat(),
                )
            }
        }
    } else {
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
                        onSelect(FanPart.Ring(index.coerceAtMost(rows.lastIndex)))
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
    }
    if (rows.size < DockPrefs.ROWS_MAX) {
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                rows.add(DockPrefs.newRow())
                onSelect(FanPart.Ring(rows.lastIndex))
                save()
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        ) { Text(stringResource(R.string.row_add)) }
    }
}
