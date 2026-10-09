package com.lukeneedham.androiddock

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * The dock's haptic feedback. It goes through the view, so it follows the system's "touch
 * feedback" setting: someone who has turned that off gets none of it.
 */
object Haptics {

    /** A light tap: a press on something. */
    fun tap(view: View?) = view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

    /** A lighter tick, for a thing going away. */
    fun tick(view: View?) = view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    /** A firmer click that something was done, such as switching to an app. */
    fun confirm(view: View?) = view?.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        },
    )

    fun longPress(view: View?) = view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
