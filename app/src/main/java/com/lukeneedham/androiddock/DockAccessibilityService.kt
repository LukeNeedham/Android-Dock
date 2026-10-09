package com.lukeneedham.androiddock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Places a [CornerTouchView] over the bottom-right corner of the screen, on top of the
 * navigation bar. Its position and size come from [DockPrefs]. Touching it opens the sheet, and a long press opens the settings.
 */
class DockAccessibilityService : AccessibilityService() {

    private var cornerView: CornerTouchView? = null
    private var cornerParams: WindowManager.LayoutParams? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (DockPrefs.isSettingKey(key)) applyLayout()
        if (DockPrefs.isColorKey(key)) cornerView?.applyColor()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val navMode = Settings.Secure.getInt(contentResolver, "navigation_mode", -1)
        DockLog.log(
            this,
            "service connected sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL} " +
                "navigationMode=$navMode (0=3-button 1=2-button 2=gesture)",
        )
        addCornerView()
        DockPrefs.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun addCornerView() {
        if (cornerView != null) return
        val params = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
        }
        fillLayout(params)
        val view = CornerTouchView(this).apply {
            onPress = ::onCornerPress
            onLongPress = ::openSettings
        }
        getSystemService(WindowManager::class.java).addView(view, params)
        cornerView = view
        cornerParams = params
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    /** Sets the window's size and offsets, measured from the bottom-right corner, from the prefs. */
    private fun fillLayout(params: WindowManager.LayoutParams) {
        params.width = dpToPx(DockPrefs.get(this, DockPrefs.Setting.WIDTH))
        params.height = dpToPx(DockPrefs.get(this, DockPrefs.Setting.HEIGHT))
        params.x = dpToPx(DockPrefs.get(this, DockPrefs.Setting.RIGHT_OFFSET))
        params.y = dpToPx(DockPrefs.get(this, DockPrefs.Setting.BOTTOM_OFFSET))
    }

    private fun applyLayout() {
        val view = cornerView ?: return
        val params = cornerParams ?: return
        fillLayout(params)
        getSystemService(WindowManager::class.java).updateViewLayout(view, params)
        DockLog.log(
            this,
            "touch target set to " + DockPrefs.Setting.entries.joinToString(" ") {
                "${it.name.lowercase()}=${DockPrefs.get(this, it)}dp"
            },
        )
    }

    private var lastPressAt = 0L

    /**
     * A press opens or closes the sheet. A second press within the double-tap time instead
     * switches to the app used before the current one.
     */
    private fun onCornerPress() {
        val now = SystemClock.uptimeMillis()
        val isDouble = now - lastPressAt <= ViewConfiguration.getDoubleTapTimeout()
        // A third press starts a new tap rather than a second double tap.
        lastPressAt = if (isDouble) 0L else now
        if (isDouble) switchToPreviousApp() else openSheet()
    }

    private fun switchToPreviousApp() {
        DockActivity.current?.closeSheet()
        Thread {
            val app = RecentApps.load(this, 1).firstOrNull()
            if (app == null) {
                DockLog.log(this, "double tap: no previous app")
                return@Thread
            }
            try {
                RecentApps.launch(this, app)
                DockLog.log(this, "double tap: switching to ${app.packageName}")
            } catch (e: RuntimeException) {
                DockLog.log(this, "could not switch to ${app.packageName}: $e")
            }
        }.start()
    }

    private fun openSheet() {
        // A press on the button while the sheet is up closes it.
        DockActivity.current?.let {
            it.closeSheet()
            DockLog.log(this, "closing sheet")
            return
        }
        try {
            startActivity(Intent(this, DockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            DockLog.log(this, "opening sheet")
        } catch (e: RuntimeException) {
            DockLog.log(this, "could not open sheet: $e")
        }
    }

    /** A long press on the button opens the dock's settings, and closes the sheet it also opened. */
    private fun openSettings() {
        DockActivity.current?.closeSheet()
        try {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            DockLog.log(this, "long press: opening settings")
        } catch (e: RuntimeException) {
            DockLog.log(this, "could not open settings: $e")
        }
    }

    private fun removeCornerView() {
        cornerView?.let { getSystemService(WindowManager::class.java).removeView(it) }
        cornerView = null
        cornerParams = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        DockLog.log(this, "service unbound")
        DockPrefs.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        removeCornerView()
        return super.onUnbind(intent)
    }
}
