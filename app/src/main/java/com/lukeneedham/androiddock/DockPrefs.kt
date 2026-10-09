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

    /**
     * One circular row of icons: how many it holds, and its width in dp, measured along the
     * bottom edge. The rows' widths add up to the sheet's radius, and the icons in a row are
     * sized to the room it has.
     */
    data class Row(val count: Int, val widthDp: Int, val color: Int = ROW_COLOR_DEFAULT, val showColor: Boolean = false)

    const val ROW_COUNT_MIN = 1
    const val ROW_COUNT_MAX = 12
    const val ROW_WIDTH_MIN = 32
    const val ROW_WIDTH_MAX = 150
    private const val ROW_WIDTH_DEFAULT = 64
    const val ROWS_MAX = 6
    private const val ROWS_KEY = "rows"
    private const val OLD_MAX_ITEMS_KEY = "max_items"
    /** The background colour a row starts with, and gets when it is switched on while clear. */
    const val ROW_COLOR_DEFAULT = 0xFF3A3A3A.toInt()

    /**
     * The rows of the sheet, innermost (closest to the corner) first; never empty. Stored as
     * "count:width:color:show,..."; the colour is ARGB, and only drawn when show is set. Falls back to one row sized from the old "maximum apps" setting.
     */
    fun getRows(context: Context): List<Row> {
        val prefs = prefs(context)
        val parsed = prefs.getString(ROWS_KEY, null)?.split(',')?.mapNotNull { part ->
            val count = part.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val width = part.split(':').getOrNull(1)?.toIntOrNull() ?: ROW_WIDTH_DEFAULT
            val color = part.split(':').getOrNull(2)?.toIntOrNull() ?: ROW_COLOR_DEFAULT
            // Rows saved before the switch existed showed their colour unless it was clear.
            val show = part.split(':').getOrNull(3)?.let { it == "1" } ?: (color ushr 24 != 0)
            Row(count.coerceIn(ROW_COUNT_MIN, ROW_COUNT_MAX), width.coerceIn(ROW_WIDTH_MIN, ROW_WIDTH_MAX), color, show)
        }?.take(ROWS_MAX)
        if (!parsed.isNullOrEmpty()) return parsed
        val old = prefs.getInt(OLD_MAX_ITEMS_KEY, 6).coerceIn(ROW_COUNT_MIN, ROW_COUNT_MAX)
        return listOf(Row(old, ROW_WIDTH_MAX))
    }

    fun setRows(context: Context, rows: List<Row>) {
        prefs(context).edit()
            .putString(ROWS_KEY, rows.joinToString(",") { "${it.count}:${it.widthDp}:${it.color}:${if (it.showColor) 1 else 0}" })
            .apply()
    }

    fun newRow() = Row(6, ROW_WIDTH_DEFAULT)

    /** How far from the corner, in dp, the first row starts. */
    const val INNER_MAX = 150
    private const val INNER_KEY = "inner_offset_dp"

    fun getInnerOffset(context: Context): Int =
        prefs(context).getInt(INNER_KEY, 0).coerceIn(0, INNER_MAX)

    fun setInnerOffset(context: Context, value: Int) {
        prefs(context).edit().putInt(INNER_KEY, value).apply()
    }

    /**
     * The clear space, in dp, between the first or last icon of a row and the screen edge it is
     * nearest: the bottom edge for the first, the side edge for the last.
     */
    enum class Padding(val key: String) {
        BOTTOM("padding_bottom_dp"),
        SIDE("padding_side_dp"),
    }

    const val PADDING_MAX = 80
    private const val PADDING_DEFAULT = 8

    fun getPadding(context: Context, padding: Padding): Int =
        prefs(context).getInt(padding.key, PADDING_DEFAULT).coerceIn(0, PADDING_MAX)

    fun setPadding(context: Context, padding: Padding, value: Int) {
        prefs(context).edit().putInt(padding.key, value).apply()
    }

    /**
     * Apps the user has dismissed from the sheet, with the time they did. A dismissed app stays
     * off the sheet until it has been used again after that time.
     */
    private const val DISMISSED_KEY = "dismissed_apps"
    private const val DISMISS_KEEP_MS = 24L * 60 * 60 * 1000

    fun getDismissed(context: Context): Map<String, Long> =
        prefs(context).getStringSet(DISMISSED_KEY, null).orEmpty().mapNotNull { entry ->
            val time = entry.substringAfterLast('|').toLongOrNull() ?: return@mapNotNull null
            entry.substringBeforeLast('|') to time
        }.toMap()

    fun dismissApp(context: Context, packageName: String) {
        val now = System.currentTimeMillis()
        // The sheet only looks a day back, so older dismissals can no longer matter.
        val kept = getDismissed(context).filter { it.value > now - DISMISS_KEEP_MS && it.key != packageName }
        val entries = kept.map { "${it.key}|${it.value}" }.toSet() + "$packageName|$now"
        prefs(context).edit().putStringSet(DISMISSED_KEY, entries).apply()
    }

    fun isSettingKey(key: String?) = Setting.entries.any { it.key == key }

    fun isColorKey(key: String?) = ColorSetting.entries.any { it.key == key }
}
