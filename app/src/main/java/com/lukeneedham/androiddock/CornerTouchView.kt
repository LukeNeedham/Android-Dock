package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * The touch target over the bottom-right corner of the navigation bar.
 *
 * The window that receives ACTION_DOWN also receives every later event of that gesture, so this
 * view is where the whole press-drag-release gesture is observed, in raw screen coordinates.
 *
 * PROTOTYPE: only logs the gesture, to check what the overlay can see on the real nav bar.
 */
@SuppressLint("ViewConstructor")
class CornerTouchView(context: Context) : View(context) {

    init {
        // Faintly visible while prototyping, so the touch target can be seen.
        setBackgroundColor(Color.argb(60, 255, 0, 0))
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
        Log.d(TAG, "$name raw=(${event.rawX}, ${event.rawY})")
        return true
    }

    private companion object {
        const val TAG = "DockCorner"
    }
}
