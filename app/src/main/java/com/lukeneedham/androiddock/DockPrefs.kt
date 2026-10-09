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

    /** The colours the user can change, stored as ARGB ints. */
    enum class ColorSetting(val key: String, val default: Int) {
        /** The touch target over the navigation bar. Translucent red by default. */
        BUTTON("button_color", 0x3CFF0000),
        SHEET("sheet_color", 0xFF2B2B2B.toInt()),
    }

    fun getColor(context: Context, setting: ColorSetting): Int =
        prefs(context).getInt(setting.key, setting.default)

    fun setColor(context: Context, setting: ColorSetting, value: Int) {
        prefs(context).edit().putInt(setting.key, value).apply()
    }

    /** How many apps the sheet lists at most. The sheet's height is fixed from this. */
    const val MAX_ITEMS_MIN = 1
    const val MAX_ITEMS_MAX = 12
    private const val MAX_ITEMS_KEY = "max_items"
    private const val RADIUS_KEY = "sheet_radius_dp"
    private const val ALIGN_RIGHT_KEY = "align_right"

    fun getMaxItems(context: Context): Int =
        prefs(context).getInt(MAX_ITEMS_KEY, 6).coerceIn(MAX_ITEMS_MIN, MAX_ITEMS_MAX)

    fun setMaxItems(context: Context, value: Int) {
        prefs(context).edit().putInt(MAX_ITEMS_KEY, value).apply()
    }

    /** The radius of the sheet's quarter circle, in dp. */
    const val RADIUS_MIN = 120
    const val RADIUS_MAX = 500
    const val RADIUS_DEFAULT = 300

    fun getRadius(context: Context): Int =
        prefs(context).getInt(RADIUS_KEY, RADIUS_DEFAULT).coerceIn(RADIUS_MIN, RADIUS_MAX)

    fun setRadius(context: Context, value: Int) {
        prefs(context).edit().putInt(RADIUS_KEY, value).apply()
    }

    /** Whether the sheet's items sit against the right edge, with the icon on the right. */
    fun isAlignRight(context: Context): Boolean =
        prefs(context).getBoolean(ALIGN_RIGHT_KEY, false)

    fun setAlignRight(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(ALIGN_RIGHT_KEY, value).apply()
    }

    fun isSettingKey(key: String?) = Setting.entries.any { it.key == key }

    fun isColorKey(key: String?) = ColorSetting.entries.any { it.key == key }
}
