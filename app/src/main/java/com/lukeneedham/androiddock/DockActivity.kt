package com.lukeneedham.androiddock

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hosts the corner sheet of open apps: a quarter circle in the bottom-right corner of the screen.
 * The app icons run along its arc. Has no other UI: the activity is transparent, and when the
 * sheet goes away the whole task is removed, so the app never lingers in the system task switcher.
 */
class DockActivity : AppCompatActivity() {

    private class OpenApp(val packageName: String, val label: String, val icon: Drawable)

    /** A quarter circle centred on the bottom-right corner of its own bounds. */
    private class QuarterCircleView(context: Context, color: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(width.toFloat(), height.toFloat(), width.toFloat(), paint)
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
            QuarterCircleView(this, sheetColor).apply { isClickable = true },
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
            val apps = loadOpenApps(maxItems)
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
    private fun showApps(root: ViewGroup, apps: List<OpenApp>, rows: List<DockPrefs.Row>, radius: Int, textColor: Int) {
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
        val innerPx = DockPrefs.getInnerOffset(this).dp
        val scale = radius.toFloat() / (innerPx + rows.sumOf { it.widthDp.dp })
        var inner = innerPx * scale
        val placed = rows.mapIndexed { i, row ->
            val band = row.widthDp.dp * scale
            val centreRadius = inner + band / 2
            inner += band
            val n = filled[i]
            if (n == 0) return@mapIndexed centreRadius to 0
            val step = (Math.PI / 2 / n).toFloat()
            val chord = 2 * centreRadius * sin(step / 2)
            val size = minOf(MAX_ICON_SIZE.dp.toFloat(), band / ROW_SPACING, chord / ICON_SPACING)
                .toInt()
                .coerceAtLeast(MIN_ICON_SIZE.dp)
            centreRadius to size
        }

        var next = 0
        filled.forEachIndexed { i, n ->
            val (centreRadius, size) = placed[i]
            val step = (Math.PI / 2 / n).toFloat()
            repeat(n) { slot ->
                val app = apps[next++]
                // The most recent app is nearest the bottom edge, where the thumb that pressed the
                // corner button is, and the oldest is at the top of the arc.
                val angle = step * (slot + 0.5f)
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

    private fun launch(app: OpenApp) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        closeSheet()
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * The [limit] most recently used apps from the last day, most recent first. Android gives
     * no list of running apps to ordinary apps, so this is built from usage events (needs usage
     * access). Runs off the main thread.
     */
    private fun loadOpenApps(limit: Int): List<OpenApp> {
        if (!SetupState.isUsageAccessGranted(this)) return emptyList()
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(now - WINDOW_MS, now)
        val lastUsed = HashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }
        val launchers = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        // The app the user was in when they opened the dock is the most recently used one other
        // than the dock itself. It is already on screen behind the sheet, so it is left out.
        val currentApp = lastUsed.entries
            .filter { it.key != packageName }
            .maxByOrNull { it.value }
            ?.key
        return lastUsed.entries
            .filter { it.key != packageName && it.key != currentApp && it.key !in launchers }
            .sortedByDescending { it.value }
            .asSequence()
            .mapNotNull { (pkg, _) ->
                if (packageManager.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                try {
                    val info = packageManager.getApplicationInfo(pkg, 0)
                    OpenApp(pkg, packageManager.getApplicationLabel(info).toString(), packageManager.getApplicationIcon(info))
                } catch (e: Exception) {
                    null
                }
            }
            .take(limit)
            .toList()
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
        private const val WINDOW_MS = 24L * 60 * 60 * 1000
    }
}
