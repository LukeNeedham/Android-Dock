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

    /** One circular row of icons: how many it holds. Its icon size follows from the room it has. */
    data class Row(val count: Int)

    const val ROW_COUNT_MIN = 1
    const val ROW_COUNT_MAX = 12
    const val ROWS_MAX = 6
    private const val ROWS_KEY = "rows"
    private const val OLD_MAX_ITEMS_KEY = "max_items"
    private const val ALIGN_RIGHT_KEY = "align_right"

    /**
     * The rows of the sheet, innermost (closest to the corner) first; never empty. Stored as
     * "count,count". Falls back to one row sized from the old "maximum apps" setting.
     */
    fun getRows(context: Context): List<Row> {
        val prefs = prefs(context)
        // An entry may carry a ":size" suffix from an earlier version; it is ignored.
        val parsed = prefs.getString(ROWS_KEY, null)?.split(',')?.mapNotNull { part ->
            part.substringBefore(':').toIntOrNull()?.coerceIn(ROW_COUNT_MIN, ROW_COUNT_MAX)?.let(::Row)
        }?.take(ROWS_MAX)
        if (!parsed.isNullOrEmpty()) return parsed
        val old = prefs.getInt(OLD_MAX_ITEMS_KEY, 6).coerceIn(ROW_COUNT_MIN, ROW_COUNT_MAX)
        return listOf(Row(old))
    }

    fun setRows(context: Context, rows: List<Row>) {
        prefs(context).edit()
            .putString(ROWS_KEY, rows.joinToString(",") { it.count.toString() })
            .apply()
    }

    fun newRow() = Row(6)

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
