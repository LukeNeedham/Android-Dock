package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Hosts the corner sheet of open apps: a quarter circle in the bottom-right corner of the screen.
 * The app icons run along its arc. Has no other UI: the activity is transparent, and when the
 * sheet goes away the whole task is removed, so the app never lingers in the system task switcher.
 */
class DockActivity : AppCompatActivity() {

    /**
     * A quarter circle centred on the bottom-right corner of its own bounds, with a ring over it
     * for each row that has a background colour. [bands] are the rows' inner and outer radii.
     */
    private class SheetBackgroundView(
        context: Context,
        color: Int,
        private val bands: List<Pair<Float, Float>>,
        private val rowColors: List<Int>,
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        /**
         * Only the quarter circle takes touches. A touch in the square's empty corner is left
         * unhandled, so it falls through to the root and closes the sheet.
         */
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN &&
                hypot(width - event.x, height - event.y) > width
            ) return false
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(width.toFloat(), height.toFloat(), width.toFloat(), paint)
            bands.forEachIndexed { i, (start, end) ->
                if (rowColors[i] ushr 24 == 0) return@forEachIndexed
                ringPaint.color = rowColors[i]
                ringPaint.strokeWidth = end - start
                canvas.drawCircle(width.toFloat(), height.toFloat(), (start + end) / 2, ringPaint)
            }
        }
    }

    /** The inner and outer radius of each row, in px, scaled so they fit a sheet of [radius]. */
    private fun rowBands(rows: List<DockPrefs.Row>, radius: Int): List<Pair<Float, Float>> {
        val innerPx = DockPrefs.getInnerOffset(this).dp
        val scale = radius.toFloat() / (innerPx + rows.sumOf { it.widthDp.dp })
        var edge = innerPx * scale
        return rows.map { row ->
            val start = edge
            edge += row.widthDp.dp * scale
            start to edge
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = this
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeSheet()
        })
        setContentView(buildContent())
    }

    /** The square holding the whole sheet; it scales about the screen corner. */
    private var sheet: View? = null
    private var closing = false

    /** Shrinks the sheet back into the corner, then closes this activity. */
    fun closeSheet() {
        if (closing) return
        closing = true
        val view = sheet
        if (view == null) {
            finishAndRemoveTask()
            return
        }
        view.animate().cancel()
        view.animate()
            .scaleX(0f).scaleY(0f)
            .setDuration(EXIT_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { finishAndRemoveTask() }
            .start()
    }

    private fun buildContent(): View {
        val sheetColor = DockPrefs.getColor(this, DockPrefs.ColorSetting.SHEET)
        val textColor = if (ColorUtils.calculateLuminance(sheetColor) > 0.5) Color.BLACK else Color.WHITE

        // Tapping outside the quarter circle closes the sheet.
        val root = FrameLayout(this).apply { setOnClickListener { closeSheet() } }

        val screenWidth = resources.displayMetrics.widthPixels
        val rows = DockPrefs.getRows(this)
        // The gap before row 1 plus the rows' widths is the sheet's radius, shrunk to fit narrow
        // screens.
        val innerPx = DockPrefs.getInnerOffset(this).dp
        val radius = minOf(innerPx + rows.sumOf { it.widthDp.dp }, (screenWidth * 0.9f).toInt())

        // Everything in the sheet lives in this square, so one scale animates the lot.
        val sheetView = FrameLayout(this).apply {
            pivotX = radius.toFloat()
            pivotY = radius.toFloat()
            scaleX = 0f
            scaleY = 0f
        }
        sheet = sheetView
        root.addView(sheetView, FrameLayout.LayoutParams(radius, radius, Gravity.BOTTOM or Gravity.END))
        sheetView.post {
            sheetView.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(ENTER_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Clickable so a tap on the empty part of the sheet does not fall through and close it.
        sheetView.addView(
            SheetBackgroundView(this, sheetColor, rowBands(rows, radius), rows.map { if (it.showColor) it.color else 0 }).apply { isClickable = true },
            FrameLayout.LayoutParams(radius, radius),
        )

        val loading = ProgressBar(this).apply { indeterminateTintList = ColorStateList.valueOf(textColor) }
        val spinnerSize = 40.dp
        val spinnerCentre = (radius * 0.6f).toInt()
        sheetView.addView(
            loading,
            FrameLayout.LayoutParams(spinnerSize, spinnerSize, Gravity.BOTTOM or Gravity.END).apply {
                // On the diagonal of the quarter circle.
                val offset = (spinnerCentre * COS_45 - spinnerSize / 2).toInt()
                rightMargin = offset
                bottomMargin = offset
            },
        )

        val maxItems = rows.sumOf { it.count }
        Thread {
            val apps = RecentApps.load(this, maxItems)
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                sheetView.removeView(loading)
                showApps(sheetView, apps, rows, radius, textColor)
            }
        }.start()
        return root
    }

    /**
     * Spreads the icons evenly along one or more arcs (rows), between the two straight edges of
     * the sheet. Each row sizes its icons to the room it has.
     */
    private fun showApps(root: ViewGroup, apps: List<RecentApps.App>, rows: List<DockPrefs.Row>, radius: Int, textColor: Int) {
        if (apps.isEmpty()) {
            root.addView(
                TextView(this).apply {
                    setText(R.string.sheet_empty)
                    setTextColor(textColor)
                    gravity = Gravity.CENTER
                },
                FrameLayout.LayoutParams(
                    (radius * 0.45f).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.END,
                ).apply {
                    rightMargin = (radius * 0.35f).toInt()
                    bottomMargin = (radius * 0.35f).toInt()
                },
            )
            return
        }

        // Row 0 is the innermost. Rows fill from the inside; a row with no app left stays empty.
        var left = apps.size
        val filled = rows.map { row ->
            val n = minOf(left, row.count)
            left -= n
            n
        }

        // Each row spans its width along the bottom edge, scaled if the sheet was shrunk to fit
        // the screen. Its icons are as large as fit in that band and without touching along the
        // chord between neighbours, up to [MAX_ICON_SIZE].
        val bands = rowBands(rows, radius)
        val bottomPad = DockPrefs.getPadding(this, DockPrefs.Padding.BOTTOM).dp
        val sidePad = DockPrefs.getPadding(this, DockPrefs.Padding.SIDE).dp
        // The icons of a row run from `from` (nearest the bottom edge) to `to` (nearest the side
        // edge), measured as the angle up from the bottom edge. The first and last icon keep the
        // padding clear of those edges, and the rest are spread evenly between them.
        data class Placement(val centreRadius: Float, val size: Int, val from: Float, val to: Float)

        fun arc(centreRadius: Float, size: Int): Pair<Float, Float> {
            val from = asin(((bottomPad + size / 2f) / centreRadius).coerceAtMost(1f))
            val to = (Math.PI / 2).toFloat() - asin(((sidePad + size / 2f) / centreRadius).coerceAtMost(1f))
            // No room for the padding: put the row's icons on the diagonal.
            return if (to >= from) from to to else (Math.PI / 4).toFloat().let { it to it }
        }

        val placed = rows.mapIndexed { i, _ ->
            val (start, end) = bands[i]
            val centreRadius = (start + end) / 2
            val n = filled[i]
            var size = minOf(MAX_ICON_SIZE.dp.toFloat(), (end - start) / ROW_SPACING).toInt()
            while (size > MIN_ICON_SIZE.dp && n > 0) {
                val (from, to) = arc(centreRadius, size)
                val fits = if (n == 1) to >= from else {
                    val step = (to - from) / (n - 1)
                    to > from && 2 * centreRadius * sin(step / 2) >= size * ICON_SPACING
                }
                if (fits) break
                size--
            }
            size = size.coerceAtLeast(MIN_ICON_SIZE.dp)
            val (from, to) = arc(centreRadius, size)
            Placement(centreRadius, size, from, to)
        }

        var next = 0
        filled.forEachIndexed { i, n ->
            val (centreRadius, size, from, to) = placed[i]
            repeat(n) { slot ->
                val app = apps[next++]
                // The most recent app is nearest the bottom edge, where the thumb that pressed the
                // corner button is, and the oldest is at the top of the arc.
                val angle = if (n == 1) (from + to) / 2 else from + (to - from) * slot / (n - 1)
                val cx = centreRadius * cos(angle)
                val cy = centreRadius * sin(angle)
                root.addView(
                    ImageView(this).apply {
                        setImageDrawable(app.icon)
                        contentDescription = app.label
                        isClickable = true
                        setOnClickListener { launch(app) }
                    },
                    FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.END).apply {
                        rightMargin = (cx - size / 2f).toInt()
                        bottomMargin = (cy - size / 2f).toInt()
                    },
                )
            }
        }
    }

    private fun launch(app: RecentApps.App) {
        closeSheet()
        RecentApps.launch(this, app)
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        /** The sheet currently on screen, so the corner button can close it. */
        var current: DockActivity? = null
            private set

        private const val ENTER_MS = 280L
        private const val EXIT_MS = 200L
        private const val MAX_ICON_SIZE = 64
        private const val MIN_ICON_SIZE = 16
        private const val ROW_SPACING = 1.2f
        private const val ICON_SPACING = 1.15f
        private const val COS_45 = 0.7071f
    }
}
