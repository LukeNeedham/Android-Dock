package com.lukeneedham.androiddock

import android.content.Context
import android.content.SharedPreferences

/** User-adjustable settings. */
object DockPrefs {

    private const val FILE = "dock_prefs"

    /**
     * The numbers that place the touch target, all in dp. In gesture navigation the system
     * claims a strip along the bottom edge for its swipe-up gesture, so a target inside that
     * strip loses the touch as soon as the finger moves up. Raising it with [BOTTOM_OFFSET]
     * gets it clear.
     */
    enum class Setting(val key: String, val default: Int, val min: Int) {
        RIGHT_OFFSET("right_offset_dp", 0, 0),
        BOTTOM_OFFSET("bottom_offset_dp", 0, 0),
        WIDTH("width_dp", 64, 16),
        HEIGHT("height_dp", 64, 16),
        ;

        fun max(context: Context): Int = when (this) {
            RIGHT_OFFSET -> context.resources.displayMetrics.run { (widthPixels / density).toInt() }
            BOTTOM_OFFSET -> 160
            WIDTH -> 240
            HEIGHT -> 240
        }
    }

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(context: Context, setting: Setting): Int =
        prefs(context).getInt(setting.key, setting.default)

    fun set(context: Context, setting: Setting, value: Int) {
        prefs(context).edit().putInt(setting.key, value).apply()
    }

    fun isSettingKey(key: String?) = Setting.entries.any { it.key == key }
}
