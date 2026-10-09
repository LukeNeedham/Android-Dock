package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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

    private var lastMoveLoggedAt = 0L

    init {
        // Faintly visible while prototyping, so the touch target can be seen.
        setBackgroundColor(Color.argb(60, 255, 0, 0))
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
        val inside = event.x >= 0 && event.y >= 0 && event.x < width && event.y < height
        DockLog.log(
            context,
            "$name raw=(${event.rawX.toInt()}, ${event.rawY.toInt()}) " +
                "local=(${event.x.toInt()}, ${event.y.toInt()}) ${if (inside) "inside" else "outside"}",
        )
        return true
    }

    private companion object {
        const val MOVE_LOG_INTERVAL_MS = 50L
    }
}
