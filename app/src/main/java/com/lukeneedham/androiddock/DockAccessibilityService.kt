package com.lukeneedham.androiddock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Places a [CornerTouchView] over the bottom-right corner of the screen, on top of the
 * navigation bar. Opening the sheet from it is not implemented yet.
 */
class DockAccessibilityService : AccessibilityService() {

    private var cornerView: CornerTouchView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        addCornerView()
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
        }
        val view = CornerTouchView(this)
        windowManager.addView(view, params)
        cornerView = view
    }

    private fun removeCornerView() {
        cornerView?.let { getSystemService(WindowManager::class.java).removeView(it) }
        cornerView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        removeCornerView()
        return super.onUnbind(intent)
    }

    private companion object {
        const val CORNER_SIZE_DP = 64
    }
}
