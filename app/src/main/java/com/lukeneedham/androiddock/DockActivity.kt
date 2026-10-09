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
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
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
        dialog.setOnDismissListener { finishAndRemoveTask() }
        dialog.show()
        sheet = dialog
    }

    private fun buildContent(textColor: Int): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

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

        val apps = loadOpenApps()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, PADDING.dp)
        }
        if (apps.isEmpty()) {
            list.addView(
                TextView(this).apply {
                    setText(R.string.sheet_empty)
                    setTextColor(textColor)
                    setPadding(PADDING.dp, 0, PADDING.dp, 0)
                },
            )
        }
        apps.forEach { list.addView(appRow(it, textColor)) }
        root.addView(ScrollView(this).apply { addView(list) })
        return root
    }

    private fun appRow(app: OpenApp, textColor: Int): View =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(PADDING.dp, 8.dp, PADDING.dp, 8.dp)
            isClickable = true
            setOnClickListener { launch(app) }
            addView(
                ImageView(this@DockActivity).apply { setImageDrawable(app.icon) },
                LinearLayout.LayoutParams(40.dp, 40.dp),
            )
            addView(
                TextView(this@DockActivity).apply {
                    text = app.label
                    setTextColor(textColor)
                    textSize = 16f
                    setPadding(16.dp, 0, 0, 0)
                },
            )
        }

    private fun launch(app: OpenApp) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        closeSheet()
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Apps used in the last day, most recent first. Android gives no list of running apps to
     * ordinary apps, so this is built from usage events (needs usage access): an app counts as
     * open until it is seen moving to the background more recently than to the foreground.
     */
    private fun loadOpenApps(): List<OpenApp> {
        if (!SetupState.isUsageAccessGranted(this)) return emptyList()
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(now - WINDOW_MS, now)
        val lastUsed = LinkedHashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }
        return lastUsed.entries
            .filter { it.key != packageName }
            .sortedByDescending { it.value }
            .mapNotNull { (pkg, _) ->
                if (packageManager.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                try {
                    val info = packageManager.getApplicationInfo(pkg, 0)
                    OpenApp(pkg, packageManager.getApplicationLabel(info).toString(), packageManager.getApplicationIcon(info))
                } catch (e: Exception) {
                    null
                }
            }
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
        private const val OPAQUE = 0xFF000000.toInt()
        private const val WINDOW_MS = 24L * 60 * 60 * 1000
    }
}
