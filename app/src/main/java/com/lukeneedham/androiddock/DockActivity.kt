package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
     * A quarter circle centred on the bottom-right corner of its own bounds.
     */
    private class SheetBackgroundView(
        context: Context,
        private val cornerColor: Int,
        private val edgeColor: Int,
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cornerColor }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // Fades from the corner to the sheet's outer edge.
            paint.shader =
                RadialGradient(w.toFloat(), h.toFloat(), w.toFloat(), cornerColor, edgeColor, Shader.TileMode.CLAMP)
        }

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
        }
    }

    /**
     * The inner and outer radius of each row, in px, scaled so they fit a sheet of [radius].
     * The first row is the corner space for the app on screen, which starts at the corner.
     */
    private fun rowBands(rows: List<DockPrefs.Row>, radius: Int): List<Pair<Float, Float>> {
        val scale = radius.toFloat() / rows.sumOf { it.widthDp.dp }
        var edge = 0f
        return rows.map { row ->
            val start = edge
            edge += row.widthDp.dp * scale
            start to edge
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A tap to close can arrive before this activity has been created. A recreation (such as
        // a rotation) was not asked for by a tap, but is still a sheet to keep.
        val wanted = starting
        starting = false
        if (!wanted && savedInstanceState == null) {
            finishAndRemoveTask()
            return
        }
        current = this
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeSheet()
        })
        setContentView(buildContent())
    }

    /** The square holding the whole sheet; it scales about the screen corner. */
    private var sheet: View? = null
    var closing = false
        private set

    /**
     * Shrinks the sheet back into the corner, then closes this activity. Gives a tick, unless
     * the caller has already given its own feedback for the touch that closed it.
     */
    fun closeSheet(haptic: Boolean = true) {
        if (closing) return
        closing = true
        val view = sheet
        if (haptic) Haptics.tick(view ?: window.decorView)
        if (view == null) {
            finishAndRemoveTask()
            return
        }
        view.animate().cancel()
        view.animate()
            .scaleX(0f).scaleY(0f)
            .setDuration(EXIT_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { if (closing) finishAndRemoveTask() }
            .start()
        // The animation does not run while the sheet is not visible (for example once it has
        // launched an app), which would leave a closing sheet registered as current forever.
        view.postDelayed(forceFinish, EXIT_MS * 2)
    }

    private val forceFinish = Runnable { if (closing) finishAndRemoveTask() }

    /** A tap to open arrived while this sheet was still closing: grow it back instead. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Not a new activity, so nothing else consumes the tap's request to open.
        val wanted = starting
        starting = false
        if (!wanted) return
        if (isFinishing) {
            // Too late to revive this one; the tap to open must still get a sheet.
            starting = true
            try {
                startActivity(Intent(applicationContext, DockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: RuntimeException) {
                starting = false
            }
            return
        }
        val view = sheet ?: return
        if (!closing) return
        closing = false
        view.removeCallbacks(forceFinish)
        view.animate().cancel()
        view.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onStop() {
        // A sheet that is no longer visible (Home, screen off, an app launched from it) has no
        // purpose and nothing left to animate, so it must not linger as an unseen "open" sheet.
        if (!isChangingConfigurations) {
            closing = true
            finishAndRemoveTask()
        }
        super.onStop()
    }

    private fun buildContent(): View {
        val sheetColor = DockPrefs.getColor(this, DockPrefs.ColorSetting.SHEET)
        val edgeColor = DockPrefs.getColor(this, DockPrefs.ColorSetting.SHEET_EDGE)
        // Text sits across the sheet, so it is judged against the middle of the fade.
        val backdrop = ColorUtils.blendARGB(sheetColor, edgeColor, 0.5f)
        val textColor = if (ColorUtils.calculateLuminance(backdrop) > 0.5) Color.BLACK else Color.WHITE

        // Tapping outside the quarter circle closes the sheet.
        val root = FrameLayout(this).apply { setOnClickListener { closeSheet() } }

        val screenWidth = resources.displayMetrics.widthPixels
        // The corner space for the app on screen counts as a row of one, ahead of the user's rows.
        val rows = listOf(DockPrefs.Row(1, DockPrefs.getCurrentWidth(this))) + DockPrefs.getRows(this)
        // The rows' widths add up to the sheet's radius, shrunk to fit narrow screens.
        val radius = minOf(rows.sumOf { it.widthDp.dp }, (screenWidth * 0.9f).toInt())

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
            // A tap to close can land before this first frame; do not grow what is closing.
            if (closing) return@post
            sheetView.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(ENTER_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Clickable so a tap on the empty part of the sheet does not fall through and close it.
        sheetView.addView(
            SheetBackgroundView(this, sheetColor, edgeColor).apply { isClickable = true },
            FrameLayout.LayoutParams(radius, radius),
        )

        val maxItems = rows.drop(1).sumOf { it.count }
        reload = {
            val generation = ++loadGeneration
            Thread {
                val recents = RecentApps.loadRecents(this, maxItems)
                runOnUiThread {
                    // A newer load has started since, so this one is out of date.
                    if (isDestroyed || isFinishing || generation != loadGeneration) return@runOnUiThread
                    appViews.forEach { sheetView.removeView(it) }
                    appViews.clear()
                    showApps(sheetView, recents, rows, radius, textColor)
                }
            }.start()
        }
        reload?.invoke()
        return root
    }

    /** The views showApps added, so they can be cleared when the list is reloaded. */
    private val appViews = ArrayList<View>()
    private var loadGeneration = 0
    private var reload: (() -> Unit)? = null

    private fun addAppView(root: ViewGroup, view: View, params: ViewGroup.LayoutParams) {
        appViews.add(view)
        root.addView(view, params)
    }

    /**
     * Closes [app], the one on screen: opens Home, and ends it. Takes it off the sheet too, so it
     * does not show as a recent app once it has been left.
     */
    private fun killCurrent(app: RecentApps.App) {
        DockPrefs.dismissApp(this, app.packageName)
        closeSheet(haptic = false)
        RecentApps.homeAndKill(this, app)
    }

    /**
     * Ends [app]'s background processes, and takes it off the sheet until the user opens it again,
     * filling its place.
     */
    private fun kill(app: RecentApps.App) {
        RecentApps.kill(this, app)
        DockPrefs.dismissApp(this, app.packageName)
        reload?.invoke()
    }

    /**
     * Spreads the icons evenly along one or more arcs (rows), between the two straight edges of
     * the sheet. Each row sizes its icons to the room it has. The first row is the exception: it
     * is the corner space for the app on screen, and holds just that app, or nothing on the home
     * screen.
     */
    private fun showApps(root: ViewGroup, recents: RecentApps.Recents, rows: List<DockPrefs.Row>, radius: Int, textColor: Int) {
        val current = recents.current
        val apps = recents.others
        if (apps.isEmpty()) {
            addAppView(
                root,
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
            if (current == null) return
        }

        // Row 0 is the corner space. The rest fill from the inside; a row with no app left stays
        // empty.
        var left = apps.size
        val filled = rows.mapIndexed { i, row ->
            if (i == 0) return@mapIndexed if (current != null) 1 else 0
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
            // The app on screen sits on the diagonal, whatever the padding.
            while (i > 0 && size > MIN_ICON_SIZE.dp && n > 0) {
                val (from, to) = arc(centreRadius, size)
                val fits = if (n == 1) to >= from else {
                    val step = (to - from) / (n - 1)
                    to > from && 2 * centreRadius * sin(step / 2) >= size * ICON_SPACING
                }
                if (fits) break
                size--
            }
            size = size.coerceAtLeast(MIN_ICON_SIZE.dp)
            val (from, to) = if (i == 0) (Math.PI / 4).toFloat().let { it to it } else arc(centreRadius, size)
            Placement(centreRadius, size, from, to)
        }

        var next = 0
        filled.forEachIndexed { i, n ->
            val (centreRadius, size, from, to) = placed[i]
            repeat(n) { slot ->
                val isCurrent = i == 0
                val app = if (isCurrent) current!! else apps[next++]
                // The most recent app is nearest the bottom edge, where the thumb that pressed the
                // corner button is, and the oldest is at the top of the arc.
                val angle = if (n == 1) (from + to) / 2 else from + (to - from) * slot / (n - 1)
                val cx = centreRadius * cos(angle)
                val cy = centreRadius * sin(angle)
                addAppView(
                    root,
                    ImageView(this).apply {
                        setImageDrawable(app.icon)
                        contentDescription = app.label
                        isClickable = true
                        setOnClickListener {
                            Haptics.confirm(this)
                            // Already on screen behind the sheet, so there is nothing to switch to.
                            if (isCurrent) closeSheet(haptic = false) else launch(app)
                        }
                        setOnLongClickListener {
                            Haptics.longPress(this)
                            if (isCurrent) killCurrent(app) else kill(app)
                            true
                        }
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
        closeSheet(haptic = false)
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

        /**
         * A tap has asked for the sheet to open, and no activity has yet taken up the request.
         * Taps in that gap have no activity to act on, so this stands in for the sheet.
         */
        @Volatile
        var starting = false

        /** Whether a tap on the corner should close the sheet, rather than open it. */
        val isOpen: Boolean
            get() = starting || current?.let { !it.closing && !it.isFinishing } == true

        /** Cancels a pending open, and closes the sheet if it is up. */
        fun close(haptic: Boolean = true) {
            starting = false
            current?.closeSheet(haptic)
        }

        private const val ENTER_MS = 280L
        private const val EXIT_MS = 200L
        private const val MAX_ICON_SIZE = 64
        private const val MIN_ICON_SIZE = 16
        private const val ROW_SPACING = 1.2f
        private const val ICON_SPACING = 1.15f
    }
}
