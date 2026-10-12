package com.lukeneedham.androiddock

import android.content.Context
import android.content.SharedPreferences

/** User-adjustable settings. */
object DockPrefs {

    private const val FILE = "dock_prefs"

    /**
     * The numbers that place the trigger, all in dp. In gesture navigation the system
     * claims a strip along the bottom edge for its swipe-up gesture, so a target inside that
     * strip loses the touch as soon as the finger moves up. Raising it with [VERTICAL_OFFSET]
     * gets it clear. The offsets are measured from the screen edges of the chosen [Corner].
     */
    enum class Setting(val key: String, val default: Int, val min: Int) {
        HORIZONTAL_OFFSET("right_offset_dp", 0, 0),
        VERTICAL_OFFSET("bottom_offset_dp", 0, 0),
        WIDTH("width_dp", 31, 16),
        HEIGHT("height_dp", 24, 16),
        ;

        fun max(context: Context): Int = when (this) {
            HORIZONTAL_OFFSET -> context.resources.displayMetrics.run { (widthPixels / density).toInt() }
            VERTICAL_OFFSET -> 160
            WIDTH -> 240
            HEIGHT -> 240
        }
    }

    /**
     * The screen corner the trigger and the fan sit in. Every distance (the trigger's offsets and
     * the fan's padding) is measured from the two edges that meet there.
     */
    enum class Corner(val bottom: Boolean, val end: Boolean, val label: Int, val verticalEdge: Int, val horizontalEdge: Int) {
        BOTTOM_RIGHT(true, true, R.string.corner_bottom_right, R.string.edge_bottom, R.string.edge_right),
        BOTTOM_LEFT(true, false, R.string.corner_bottom_left, R.string.edge_bottom, R.string.edge_left),
        TOP_LEFT(false, false, R.string.corner_top_left, R.string.edge_top, R.string.edge_left),
        TOP_RIGHT(false, true, R.string.corner_top_right, R.string.edge_top, R.string.edge_right),
    }

    const val CORNER_KEY = "corner"

    fun getCorner(context: Context): Corner =
        Corner.entries.firstOrNull { it.name == prefs(context).getString(CORNER_KEY, null) } ?: Corner.BOTTOM_RIGHT

    fun setCorner(context: Context, corner: Corner) {
        prefs(context).edit().putString(CORNER_KEY, corner.name).apply()
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
        /** The trigger over the navigation bar. Translucent red by default. */
        BUTTON("button_color", 0x29FFFFFF),
    }

    private const val SHEET_STOPS_KEY = "sheet_stops"

    // The sheet used to have two colours, one at the corner and one at the edge.
    private const val LEGACY_SHEET_KEY = "sheet_color"
    private const val LEGACY_SHEET_EDGE_KEY = "sheet_edge_color"

    /** The colour stops of the sheet's background; never empty. See [SheetGradient]. */
    fun getSheetStops(context: Context): List<ColorStop> {
        val prefs = prefs(context)
        SheetGradient.decode(prefs.getString(SHEET_STOPS_KEY, null))?.let { return it }
        if (prefs.contains(LEGACY_SHEET_KEY) || prefs.contains(LEGACY_SHEET_EDGE_KEY)) {
            return listOf(
                ColorStop(0f, prefs.getInt(LEGACY_SHEET_KEY, SheetGradient.DEFAULT[0].color)),
                ColorStop(1f, prefs.getInt(LEGACY_SHEET_EDGE_KEY, SheetGradient.DEFAULT[1].color)),
            )
        }
        return SheetGradient.DEFAULT
    }

    fun setSheetStops(context: Context, stops: List<ColorStop>) {
        prefs(context).edit().putString(SHEET_STOPS_KEY, SheetGradient.encode(stops)).apply()
    }

    fun getColor(context: Context, setting: ColorSetting): Int =
        prefs(context).getInt(setting.key, setting.default)

    fun setColor(context: Context, setting: ColorSetting, value: Int) {
        prefs(context).edit().putInt(setting.key, value).apply()
    }

    /**
     * One circular row of icons: how many it holds, and its width in dp, measured along the
     * screen edge. The rows' widths add up to the sheet's radius, and the icons in a row are
     * sized to the room it has.
     */
    data class Row(val count: Int, val widthDp: Int)

    const val ROW_COUNT_MIN = 1
    const val ROW_COUNT_MAX = 12
    const val ROW_WIDTH_MIN = 32
    const val ROW_WIDTH_MAX = 150
    private const val ROW_WIDTH_DEFAULT = 64
    const val ROWS_MAX = 6
    private const val ROWS_KEY = "rows"

    /** The rows a fresh install has. */
    val DEFAULT_ROWS = listOf(Row(3, 58), Row(5, 59), Row(8, 39))

    /**
     * The rows of the sheet, innermost (closest to the corner) first; never empty. Stored as
     * "count:width,..." (older versions stored a colour after the width, which is ignored). Starts with [DEFAULT_ROWS].
     */
    fun getRows(context: Context): List<Row> {
        val prefs = prefs(context)
        val parsed = prefs.getString(ROWS_KEY, null)?.split(',')?.mapNotNull { part ->
            val count = part.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val width = part.split(':').getOrNull(1)?.toIntOrNull() ?: ROW_WIDTH_DEFAULT
            Row(count.coerceIn(ROW_COUNT_MIN, ROW_COUNT_MAX), width.coerceIn(ROW_WIDTH_MIN, ROW_WIDTH_MAX))
        }?.take(ROWS_MAX)
        if (!parsed.isNullOrEmpty()) return parsed
        return DEFAULT_ROWS
    }

    fun setRows(context: Context, rows: List<Row>) {
        prefs(context).edit()
            .putString(ROWS_KEY, rows.joinToString(",") { "${it.count}:${it.widthDp}" })
            .apply()
    }

    fun newRow() = Row(6, ROW_WIDTH_DEFAULT)

    /**
     * The width, in dp, of row 0: the ring against the corner that holds just the app on screen.
     * It is drawn like any other row, but the user can only change its width. The key is the
     * one the gap before the first row used to be stored under, so a user's setting carries over.
     */
    const val CURRENT_WIDTH_MAX = 150

    /** The smallest an app icon is drawn, in dp. */
    const val ICON_SIZE_MIN = 16

    /**
     * The narrowest row 0 can be, in dp: its icon sits flush in the corner, so its far corner is
     * the icon's diagonal from the centre, and the smallest icon must fit wholly inside the ring.
     */
    val CURRENT_WIDTH_MIN = kotlin.math.ceil(ICON_SIZE_MIN * kotlin.math.sqrt(2.0)).toInt()
    private const val CURRENT_WIDTH_DEFAULT = 100
    private const val CURRENT_WIDTH_KEY = "inner_offset_dp"

    /**
     * The narrowest row 0 can be, in dp: with its app hidden it is only a margin, but still wide
     * enough to tap in the layout preview.
     */
    private const val CURRENT_WIDTH_HIDDEN_MIN = 8

    fun currentWidthMin(showCurrent: Boolean): Int = if (showCurrent) CURRENT_WIDTH_MIN else CURRENT_WIDTH_HIDDEN_MIN

    fun getCurrentWidth(context: Context): Int =
        prefs(context).getInt(CURRENT_WIDTH_KEY, CURRENT_WIDTH_DEFAULT)
            .coerceIn(currentWidthMin(getShowCurrent(context)), CURRENT_WIDTH_MAX)

    fun setCurrentWidth(context: Context, value: Int) {
        prefs(context).edit().putInt(CURRENT_WIDTH_KEY, value).apply()
    }

    /** Whether row 0 shows the app on screen. When off, the row is left empty, as margin. */
    private const val SHOW_CURRENT_KEY = "show_current_app"

    fun getShowCurrent(context: Context): Boolean = prefs(context).getBoolean(SHOW_CURRENT_KEY, true)

    fun setShowCurrent(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(SHOW_CURRENT_KEY, value).apply()
    }

    /**
     * The clear space, in dp, between the screen edge and where the fan starts: the quarter circle
     * is centred on the corner moved in by these, so its curves only start after them.
     */
    enum class Padding(val key: String, val default: Int) {
        BOTTOM("padding_bottom_dp", 20),
        SIDE("padding_side_dp", 4),
    }

    const val PADDING_MAX = 80

    fun getPadding(context: Context, padding: Padding): Int =
        prefs(context).getInt(padding.key, padding.default).coerceIn(0, PADDING_MAX)

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

    /** Apps that never show on the sheet, or as the previous app of a double tap. */
    private const val BLACKLIST_KEY = "blacklist"

    fun getBlacklist(context: Context): Set<String> =
        prefs(context).getStringSet(BLACKLIST_KEY, null).orEmpty().toSet()

    fun setBlacklist(context: Context, packages: Collection<String>) {
        prefs(context).edit().putStringSet(BLACKLIST_KEY, packages.toSet()).apply()
    }

    /** How many of the optional onboarding steps (fan layout, trigger, blacklist) are finished. */
    const val OPTIONAL_STEPS = 3
    private const val ONBOARDING_KEY = "onboarding_optional_done"

    fun getOptionalStepsDone(context: Context): Int =
        prefs(context).getInt(ONBOARDING_KEY, 0).coerceIn(0, OPTIONAL_STEPS)

    fun setOptionalStepsDone(context: Context, count: Int) {
        prefs(context).edit().putInt(ONBOARDING_KEY, count).apply()
    }

    fun isSettingKey(key: String?) = key == CORNER_KEY || Setting.entries.any { it.key == key }

    fun isColorKey(key: String?) = ColorSetting.entries.any { it.key == key }
}
