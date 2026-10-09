package com.lukeneedham.androiddock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Places a [CornerTouchView] over the bottom-right corner of the screen, on top of the
 * navigation bar, lifted by the user's bottom offset setting. Opening the sheet from it is not implemented yet.
 */
class DockAccessibilityService : AccessibilityService() {

    private var cornerView: CornerTouchView? = null
    private var cornerParams: WindowManager.LayoutParams? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == DockPrefs.KEY_BOTTOM_OFFSET_DP) applyBottomOffset()
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
        val windowManager = getSystemService(WindowManager::class.java)
        val size = (CORNER_SIZE_DP * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            y = bottomOffsetPx()
        }
        val view = CornerTouchView(this)
        windowManager.addView(view, params)
        cornerView = view
        cornerParams = params
    }

    private fun bottomOffsetPx() =
        (DockPrefs.bottomOffsetDp(this) * resources.displayMetrics.density).toInt()

    private fun applyBottomOffset() {
        val view = cornerView ?: return
        val params = cornerParams ?: return
        params.y = bottomOffsetPx()
        getSystemService(WindowManager::class.java).updateViewLayout(view, params)
        DockLog.log(this, "bottom offset set to ${DockPrefs.bottomOffsetDp(this)}dp")
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

    private companion object {
        const val CORNER_SIZE_DP = 64
    }
}
