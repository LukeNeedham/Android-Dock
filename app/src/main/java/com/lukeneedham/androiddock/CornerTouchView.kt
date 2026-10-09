package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

/**
 * The touch target over the bottom-right corner of the navigation bar.
 *
 * The window that receives ACTION_DOWN also receives every later event of that gesture, so this
 * view is where the whole press-drag-release gesture is observed, in raw screen coordinates.
 *
 * PROTOTYPE: only records the gesture in [DockLog], to check what the overlay can see on the
 * real nav bar.
 */
@SuppressLint("ViewConstructor")
class CornerTouchView(context: Context) : View(context) {

    /** Called when a finger goes down on the view. */
    var onPress: (() -> Unit)? = null

    private var lastMoveLoggedAt = 0L
    private var downAt = 0L
    private var downX = 0f
    private var downY = 0f

    init {
        applyColor()
    }

    /** Paints the touch target in the colour the user picked. */
    fun applyColor() {
        setBackgroundColor(DockPrefs.getColor(context, DockPrefs.ColorSetting.BUTTON))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // The target sits against the right edge, where the system's back gesture would otherwise
        // take a sideways swipe. (This cannot exclude the bottom swipe-up gesture.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, right - left, bottom - top))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post {
            val location = IntArray(2)
            getLocationOnScreen(location)
            val screen = resources.displayMetrics
            DockLog.log(
                context,
                "corner view at x=${location[0]} y=${location[1]} size=${width}x$height " +
                    "screen=${screen.widthPixels}x${screen.heightPixels}",
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val name = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            else -> return false
        }
        if (name == "MOVE") {
            // A drag produces many events a second. Keep the log readable.
            val now = SystemClock.uptimeMillis()
            if (now - lastMoveLoggedAt < MOVE_LOG_INTERVAL_MS) return true
            lastMoveLoggedAt = now
        }
        if (name == "DOWN") {
            downAt = SystemClock.uptimeMillis()
            downX = event.rawX
            downY = event.rawY
            onPress?.invoke()
        }
        val inside = event.x >= 0 && event.y >= 0 && event.x < width && event.y < height
        DockLog.log(
            context,
            "$name raw=(${event.rawX.toInt()}, ${event.rawY.toInt()}) " +
                "local=(${event.x.toInt()}, ${event.y.toInt()}) ${if (inside) "inside" else "outside"}",
        )
        if (name == "CANCEL" || name == "UP") {
            val dx = (event.rawX - downX).toInt()
            val dy = (event.rawY - downY).toInt()
            DockLog.log(
                context,
                "$name after ${SystemClock.uptimeMillis() - downAt}ms, moved dx=$dx dy=$dy since DOWN",
            )
        }
        return true
    }

    private companion object {
        const val MOVE_LOG_INTERVAL_MS = 50L
    }
}
