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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
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
 * The app icons run along its arc and the settings cog sits in the corner, at the centre of the
 * circle. Has no other UI: the activity is transparent, and when the sheet goes away the whole
 * task is removed, so the app never lingers in the system task switcher.
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

    /** Closes the sheet, and with it this activity. */
    fun closeSheet() {
        finishAndRemoveTask()
    }

    private fun buildContent(): View {
        val sheetColor = DockPrefs.getColor(this, DockPrefs.ColorSetting.SHEET)
        val textColor = if (ColorUtils.calculateLuminance(sheetColor) > 0.5) Color.BLACK else Color.WHITE

        // Tapping outside the quarter circle closes the sheet.
        val root = FrameLayout(this).apply { setOnClickListener { closeSheet() } }

        val screenWidth = resources.displayMetrics.widthPixels
        val radius = minOf(MAX_RADIUS.dp, (screenWidth * 0.9f).toInt())

        // Clickable so a tap on the empty part of the sheet does not fall through and close it.
        root.addView(
            QuarterCircleView(this, sheetColor).apply { isClickable = true },
            FrameLayout.LayoutParams(radius, radius, Gravity.BOTTOM or Gravity.END),
        )

        val cogSize = COG_SIZE.dp
        root.addView(
            ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_menu_preferences)
                imageTintList = ColorStateList.valueOf(textColor)
                background = null
                contentDescription = getString(R.string.sheet_settings)
                setOnClickListener {
                    startActivity(Intent(this@DockActivity, SettingsActivity::class.java))
                    closeSheet()
                }
            },
            FrameLayout.LayoutParams(cogSize, cogSize, Gravity.BOTTOM or Gravity.END),
        )

        val loading = ProgressBar(this).apply { indeterminateTintList = ColorStateList.valueOf(textColor) }
        val spinnerSize = 40.dp
        val spinnerCentre = (radius * 0.6f).toInt()
        root.addView(
            loading,
            FrameLayout.LayoutParams(spinnerSize, spinnerSize, Gravity.BOTTOM or Gravity.END).apply {
                // On the diagonal of the quarter circle.
                val offset = (spinnerCentre * COS_45 - spinnerSize / 2).toInt()
                rightMargin = offset
                bottomMargin = offset
            },
        )

        val maxItems = DockPrefs.getMaxItems(this)
        Thread {
            val apps = loadOpenApps(maxItems)
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                root.removeView(loading)
                showApps(root, apps, radius, textColor)
            }
        }.start()
        return root
    }

    /**
     * Spreads the icons evenly along the arc, between the two straight edges of the sheet. The
     * icons are as large as will fit without touching, up to [MAX_ICON_SIZE].
     */
    private fun showApps(root: FrameLayout, apps: List<OpenApp>, radius: Int, textColor: Int) {
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

        val count = apps.size
        val step = (Math.PI / 2 / count).toFloat()
        val margin = EDGE_MARGIN.dp
        val maxSize = MAX_ICON_SIZE.dp
        val minSize = MIN_ICON_SIZE.dp
        // Largest icon whose neighbours are a gap apart along the chord between their centres.
        var size = maxSize
        while (size > minSize) {
            val centreRadius = radius - margin - size / 2f
            if (2 * centreRadius * sin(step / 2) >= size * ICON_SPACING) break
            size--
        }
        val centreRadius = radius - margin - size / 2f

        apps.forEachIndexed { index, app ->
            // The oldest app is at the top of the arc, the most recent nearest the bottom edge,
            // where the thumb that pressed the corner button is.
            val angle = step * (index + 0.5f)
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

    private fun launch(app: OpenApp) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        closeSheet()
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * The [limit] most recently used apps from the last day, oldest first so the most recent is
     * last. Android gives no list of running apps to ordinary apps, so this is built from usage
     * events (needs usage access). Runs off the main thread.
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
            .reversed()
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

        private const val MAX_RADIUS = 300
        private const val COG_SIZE = 56
        private const val MAX_ICON_SIZE = 56
        private const val MIN_ICON_SIZE = 24
        private const val EDGE_MARGIN = 12
        private const val ICON_SPACING = 1.15f
        private const val COS_45 = 0.7071f
        private const val WINDOW_MS = 24L * 60 * 60 * 1000
    }
}
