package com.lukeneedham.androiddock

import android.content.Context
import android.content.SharedPreferences

/** User-adjustable settings. */
object DockPrefs {

    private const val FILE = "dock_prefs"
    const val KEY_BOTTOM_OFFSET_DP = "bottom_offset_dp"
    const val DEFAULT_BOTTOM_OFFSET_DP = 0
    const val MAX_BOTTOM_OFFSET_DP = 160

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * How far above the bottom of the screen the touch target sits. In gesture navigation the
     * system claims a strip along the bottom edge for its swipe-up gesture, so a target inside
     * that strip loses the touch as soon as the finger moves up.
     */
    fun bottomOffsetDp(context: Context): Int =
        prefs(context).getInt(KEY_BOTTOM_OFFSET_DP, DEFAULT_BOTTOM_OFFSET_DP)

    fun setBottomOffsetDp(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_BOTTOM_OFFSET_DP, value).apply()
    }
}
