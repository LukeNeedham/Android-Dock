package com.lukeneedham.androiddock

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Hosts the bottom sheet of open apps. Has no UI of its own: the activity is transparent and
 * exists only to show the sheet. When the sheet goes away, the whole task is removed, so the
 * app never lingers in the system task switcher.
 */
class DockActivity : AppCompatActivity() {

    private var sheet: BottomSheetDialog? = null

    private class OpenApp(val packageName: String, val label: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = this
        showSheet()
    }

    /** Closes the sheet, and with it this activity. */
    fun closeSheet() {
        sheet?.dismiss() ?: finishAndRemoveTask()
    }

    private fun showSheet() {
        val sheetColor = DockPrefs.getColor(this, DockPrefs.ColorSetting.SHEET) or OPAQUE
        val textColor = if (ColorUtils.calculateLuminance(sheetColor) > 0.5) Color.BLACK else Color.WHITE

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(buildContent(textColor))
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?.backgroundTintList = ColorStateList.valueOf(sheetColor)
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setOnDismissListener { finishAndRemoveTask() }
        dialog.show()
        sheet = dialog
    }

    private fun buildContent(textColor: Int): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val alignRight = DockPrefs.isAlignRight(this)

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(PADDING.dp, PADDING.dp, PADDING.dp / 2, PADDING.dp / 2)
        }
        header.addView(
            TextView(this).apply {
                setText(R.string.sheet_title)
                setTextColor(textColor)
                textSize = 20f
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
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
            LinearLayout.LayoutParams(48.dp, 48.dp),
        )
        root.addView(header)

        // The list area has a fixed height for the most apps it may show, so the sheet is the
        // same size while loading, when empty and when full.
        val maxItems = DockPrefs.getMaxItems(this)
        val area = FrameLayout(this)
        root.addView(
            area,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxItems * ROW_HEIGHT.dp + PADDING.dp),
        )
        val loading = ProgressBar(this).apply { indeterminateTintList = ColorStateList.valueOf(textColor) }
        area.addView(loading, FrameLayout.LayoutParams(48.dp, 48.dp, Gravity.CENTER))

        Thread {
            val apps = loadOpenApps(maxItems)
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                area.removeView(loading)
                showApps(area, apps, textColor, alignRight)
            }
        }.start()
        return root
    }

    private fun showApps(area: FrameLayout, apps: List<OpenApp>, textColor: Int, alignRight: Boolean) {
        if (apps.isEmpty()) {
            area.addView(
                TextView(this).apply {
                    setText(R.string.sheet_empty)
                    setTextColor(textColor)
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
            return
        }
        // Rows are packed against the bottom, so the most recent app (last) is nearest the button.
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, PADDING.dp)
        }
        apps.forEach { list.addView(appRow(it, textColor, alignRight)) }
        area.addView(
            list,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    private fun appRow(app: OpenApp, textColor: Int, alignRight: Boolean): View {
        val icon = ImageView(this).apply { setImageDrawable(app.icon) }
        val label = TextView(this).apply {
            text = app.label
            setTextColor(textColor)
            textSize = 16f
            setPadding(16.dp, 0, 16.dp, 0)
        }
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL or if (alignRight) Gravity.END else Gravity.START
            setPadding(PADDING.dp, 0, PADDING.dp, 0)
            isClickable = true
            setOnClickListener { launch(app) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEIGHT.dp)
            if (alignRight) {
                addView(label)
                addView(icon, LinearLayout.LayoutParams(40.dp, 40.dp))
            } else {
                addView(icon, LinearLayout.LayoutParams(40.dp, 40.dp))
                addView(label)
            }
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
        return lastUsed.entries
            .filter { it.key != packageName && it.key !in launchers }
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
        sheet?.setOnDismissListener(null)
        sheet?.dismiss()
        super.onDestroy()
    }

    companion object {
        /** The sheet currently on screen, so the corner button can close it. */
        var current: DockActivity? = null
            private set

        private const val PADDING = 24
        private const val ROW_HEIGHT = 56
        private const val OPAQUE = 0xFF000000.toInt()
        private const val WINDOW_MS = 24L * 60 * 60 * 1000
    }
}
