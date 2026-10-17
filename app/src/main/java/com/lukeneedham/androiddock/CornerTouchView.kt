package com.lukeneedham.androiddock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.lukeneedham.common.logger.Logger
import kotlin.math.abs

/**
 * Trigger number [trigger] over the chosen corner of the screen, on top of the navigation bar.
 *
 * The window that receives ACTION_DOWN also receives every later event of that gesture, so this
 * view is where the whole press-drag-release gesture is observed, in raw screen coordinates.
 */
@SuppressLint("ViewConstructor")
class CornerTouchView(context: Context, private val trigger: Int) : View(context) {

    /** Called when a finger goes down on the view. */
    var onPress: ((CornerTouchView) -> Unit)? = null

    /** Called when the finger has stayed down on the view for the system's long-press time. */
    var onLongPress: ((CornerTouchView) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPress = Runnable { onLongPress?.invoke(this) }

    private var downX = 0f
    private var downY = 0f

    init {
        applyColor()
    }

    /** Paints the trigger in the colour the user picked. */
    fun applyColor() {
        setBackgroundColor(DockPrefs.getColor(context, DockPrefs.ColorSetting.BUTTON, trigger))
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
            Logger.i(
                "corner view at x=${location[0]} y=${location[1]} size=${width}x$height " +
                    "screen=${screen.widthPixels}x${screen.heightPixels}",
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                onPress?.invoke(this)
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(event.rawX - downX) > touchSlop || abs(event.rawY - downY) > touchSlop) {
                    removeCallbacks(longPress)
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(longPress)
            else -> return false
        }
        return true
    }
}
