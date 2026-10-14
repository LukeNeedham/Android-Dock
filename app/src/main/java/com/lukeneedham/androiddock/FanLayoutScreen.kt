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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.rotate
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.compositeOver
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
    val haptic = LocalHapticFeedback.current
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
    // The background is edited on the main page; here it only shows behind the fan.
    val stops = remember { DockPrefs.getSheetStops(context) }

    SubPage(R.string.fan_layout_title, onBack, footer, header) {
        FanPreview(currentWidth, bottomPad, sidePad, rows.toList(), selected, stops, corner = corner, showCurrent = showCurrent, onSelect = { selected = it },
            onAddRow = if (rows.size < DockPrefs.ROWS_MAX) ({
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                rows.add(DockPrefs.newRow())
                selected = FanPart.Ring(rows.lastIndex)
                DockPrefs.setRows(context, rows.toList())
            }) else null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
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

private const val PLUS_RADIUS_DP = 16f
private const val PLUS_GAP_DP = 8f

/**
 * Where the add-ring button sits, in pixels: halfway down the preview, just beyond the outermost
 * ring, and kept inside the canvas.
 */
private fun plusCentre(width: Float, height: Float, density: Float, corner: DockPrefs.Corner, radiusDp: Float, sidePad: Int): Offset {
    val r = PLUS_RADIUS_DP * density
    val dirX = if (corner.end) -1f else 1f
    val x = (if (corner.end) width - sidePad * density else sidePad * density) + dirX * (radiusDp * density + PLUS_GAP_DP * density + r)
    return Offset(x.coerceIn(r, width - r), height / 2)
}

/** Sliders on this page have a white accent. */
@Composable
private fun whiteSliderColors() = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = Color.White,
    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
    activeTickColor = Color.Black.copy(alpha = 0.4f),
    inactiveTickColor = Color.White.copy(alpha = 0.5f),
)

/** How thick the bars for the margins are, beside the preview. */
private val MARGIN_BAR = 50.dp

/** Every part that can be selected, wrapped so that all are in view: tapping one selects it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartList(ringCount: Int, corner: DockPrefs.Corner, selected: FanPart, onSelect: (FanPart) -> Unit) {
    val bottomLabel = stringResource(R.string.part_margin, stringResource(corner.verticalEdge)).replaceFirstChar { it.uppercase() }
    val sideLabel = stringResource(R.string.part_margin, stringResource(corner.horizontalEdge)).replaceFirstChar { it.uppercase() }
    val cornerLabel = stringResource(R.string.row_current_title)
    val ringLabels = (0 until ringCount).map { stringResource(R.string.row_title, it + 1) }
    val parts = buildList {
        add(FanPart.BottomMargin to bottomLabel)
        add(FanPart.SideMargin to sideLabel)
        add(FanPart.Ring(-1) to cornerLabel)
        ringLabels.forEachIndexed { i, label -> add(FanPart.Ring(i) to label) }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        parts.forEach { (part, label) ->
            val on = part == selected
            val base = MaterialTheme.colorScheme.surfaceVariant
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (on) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else base)
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
    onAddRow: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .then(if (thumbnailDp != null) Modifier.size(thumbnailDp.dp).clip(RoundedCornerShape(8.dp)) else Modifier.fillMaxWidth()),
    ) {
        // The bars for the margins sit outside the preview, beside the corner's two edges.
        val interactive = onSelect != null && thumbnailDp == null
        val bar = if (interactive) MARGIN_BAR else 0.dp
        val previewWidth = maxWidth - bar
        // Row 0, flush against the corner, holds a single icon: the app on screen, or nothing when
        // that is hidden. It is drawn like every other ring.
        val ringZero = if (inner > 0) listOf(DockPrefs.Row(if (showCurrent) 1 else 0, inner)) else emptyList()
        val allRows = ringZero + rows
        // As in the sheet: the rows' widths, row 0's included, make the radius, shrunk to fit the width.
        val total = allRows.sumOf { it.widthDp }
        // A thumbnail draws the whole fan at full size and shrinks it to fit its box.
        // Life-size: the same radius the sheet gets on this screen, which is not shrunk to fit the
        // preview.
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val radiusDp = if (thumbnailDp != null) total.toFloat().coerceAtLeast(1f)
        else min(total.toFloat(), screenWidth * 0.9f).coerceAtLeast(1f)
        // The fan, its margin and the add button may be wider than the room beside the bars, and
        // then the preview scrolls sideways.
        val neededDp = radiusDp + sidePad + if (onAddRow != null && interactive) PLUS_RADIUS_DP * 2 + PLUS_GAP_DP else 0f
        val canvasWidth = max(previewWidth.value, neededDp).dp
        val scroll = rememberScrollState()
        if (interactive && corner.end) LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
        // Whatever is selected is highlighted the same way as a ring: the preview's background with
        // a translucent white over it.
        val highlight = Color.White.copy(alpha = 0.2f)
        val barColor = MaterialTheme.colorScheme.surfaceVariant
        val barSelectedColor = Color.White
        // The rings are centred on the corner moved in by the padding, but the background is
        // centred on the corner itself, and reaches far enough to hold the outermost ring.
        val backgroundDp = radiusDp + hypot(sidePad.toFloat(), bottomPad.toFloat())
        val insetStart = if (!corner.end) bar else 0.dp
        val insetTop = if (!corner.bottom) bar else 0.dp
        Box(
            modifier = Modifier
                .padding(start = insetStart, top = insetTop, end = if (corner.end) bar else 0.dp, bottom = if (corner.bottom) bar else 0.dp)
                .then(if (thumbnailDp != null) Modifier.fillMaxSize() else Modifier)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (interactive) Modifier.horizontalScroll(scroll) else Modifier),
        ) {
        Canvas(
            modifier = (if (thumbnailDp != null) Modifier.size(thumbnailDp.dp) else Modifier.width(canvasWidth).height((radiusDp + bottomPad).dp))
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
                .pointerInput(onSelect != null, onAddRow != null, allRows, radiusDp, bottomPad, sidePad, corner) {
                    if (onSelect == null) return@pointerInput
                    detectTapGestures { tap ->
                        if (onAddRow != null) {
                            val plus = plusCentre(size.width.toFloat(), size.height.toFloat(), density, corner, radiusDp, sidePad)
                            if (hypot(tap.x - plus.x, tap.y - plus.y) <= PLUS_RADIUS_DP * density) {
                                onAddRow()
                                return@detectTapGestures
                            }
                        }
                        // The space of a margin selects it, even though it can be thin.
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
            if (onAddRow != null && interactive) {
                val plus = plusCentre(size.width, size.height, dp, corner, radiusDp, sidePad)
                val r = PLUS_RADIUS_DP * dp
                drawCircle(Color.White, r, plus)
                val arm = r * 0.5f
                val stroke = 2.5f * dp
                drawLine(Color.Black, Offset(plus.x - arm, plus.y), Offset(plus.x + arm, plus.y), strokeWidth = stroke)
                drawLine(Color.Black, Offset(plus.x, plus.y - arm), Offset(plus.x, plus.y + arm), strokeWidth = stroke)
            }
            // A margin is empty space, showing the background; only when selected is it highlighted.
            if (interactive) {
                if (selected == FanPart.BottomMargin && bottomPad > 0) {
                    drawRect(highlight, Offset(0f, if (corner.bottom) size.height - bottomPad * dp else 0f), androidx.compose.ui.geometry.Size(size.width, bottomPad * dp))
                }
                if (selected == FanPart.SideMargin && sidePad > 0) {
                    drawRect(highlight, Offset(if (corner.end) size.width - sidePad * dp else 0f, 0f), androidx.compose.ui.geometry.Size(sidePad * dp, size.height))
                }
            }
        }
        } // the box that holds the canvas
        if (onSelect != null && interactive) {
            val bottomLabel = stringResource(R.string.part_margin, stringResource(corner.verticalEdge)).replaceFirstChar { it.uppercase() }
            val sideLabel = stringResource(R.string.part_margin, stringResource(corner.horizontalEdge)).replaceFirstChar { it.uppercase() }
            // The bottom bar spans the preview's width and the side bar its height, on the far
            // side of the preview from the corner's edge they edit.
            MarginBar(
                bottomLabel, selected == FanPart.BottomMargin, barColor, barSelectedColor, vertical = false, onClick = { onSelect(FanPart.BottomMargin) },
                modifier = Modifier
                    .align(if (corner.bottom) Alignment.BottomStart else Alignment.TopStart)
                    .offset(x = insetStart)
                    .width(previewWidth)
                    .height(bar),
            )
            MarginBar(
                sideLabel, selected == FanPart.SideMargin, barColor, barSelectedColor, vertical = true, onClick = { onSelect(FanPart.SideMargin) },
                modifier = Modifier
                    .align(if (corner.end) Alignment.TopEnd else Alignment.TopStart)
                    .offset(y = insetTop)
                    .width(bar)
                    .height((radiusDp + bottomPad).dp),
            )
        }
    }
}

/**
 * The fixed part of a margin: a bar beside the preview, as thick as a finger, with square corners,
 * highlighted like a selected ring. The margin it edits is plain space beside it. [vertical] bars stand at the side, and turn their label to run along them.
 */
@Composable
private fun MarginBar(label: String, selected: Boolean, color: Color, selectedColor: Color, vertical: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (selected) selectedColor else color)
            .clickable(onClick = onClick),
    ) {
        Text(
            // The arrow points along the distance it changes; turned with the label, it still does.
            "↕  $label",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.Black else colors.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = if (vertical) Modifier.wrapContentSize(unbounded = true).rotate(-90f) else Modifier,
        )
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
                val pad = if (bottom) bottomPad else sidePad
                Text(stringResource(R.string.slider_padding, pad), modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = pad.toFloat(),
                    onValueChange = { if (bottom) onBottomPad(it.toInt()) else onSidePad(it.toInt()) },
                    valueRange = 0f..DockPrefs.PADDING_MAX.toFloat(),
                    colors = whiteSliderColors(),
                )
            }
        }
    } else if (selected < 0) {
        // The corner ring: its width, and whether it shows the app on screen. Nothing else.
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Checkbox(checked = showCurrent, onCheckedChange = onShowCurrent, colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black))
                    Text(stringResource(R.string.row_show_current))
                }
                Text(stringResource(R.string.row_width, currentWidth))
                Slider(
                    value = currentWidth.toFloat(),
                    onValueChange = { onCurrentWidth(it.toInt()) },
                    valueRange = DockPrefs.currentWidthMin(showCurrent).toFloat()..DockPrefs.CURRENT_WIDTH_MAX.toFloat(),
                    colors = whiteSliderColors(),
                )
            }
        }
    } else {
    val index = selected.coerceIn(0, rows.lastIndex)
    val row = rows[index]
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Spacer(Modifier.height(12.dp))
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
                colors = whiteSliderColors(),
            )
            Text(stringResource(R.string.row_width, row.widthDp))
            Slider(
                value = row.widthDp.toFloat(),
                onValueChange = {
                    rows[index] = row.copy(widthDp = it.toInt())
                    save()
                },
                valueRange = DockPrefs.ROW_WIDTH_MIN.toFloat()..DockPrefs.ROW_WIDTH_MAX.toFloat(),
                colors = whiteSliderColors(),
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    rows.removeAt(index)
                    onSelect(FanPart.Ring(index.coerceAtMost(rows.lastIndex)))
                    save()
                },
                enabled = rows.size > 1,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(delete)
            }
        }
    }
    }
    Spacer(Modifier.height(24.dp))
}
