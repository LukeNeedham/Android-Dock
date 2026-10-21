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
     * A screen corner. Each trigger sits in one, and the fan opens in the corner of the trigger that
     * was pressed. Every distance (the trigger's offsets and the fan's padding) is measured from
     * the two edges that meet there.
     */
    enum class Corner(val bottom: Boolean, val end: Boolean, val label: Int, val verticalEdge: Int, val horizontalEdge: Int) {
        BOTTOM_RIGHT(true, true, R.string.corner_bottom_right, R.string.edge_bottom, R.string.edge_right),
        BOTTOM_LEFT(true, false, R.string.corner_bottom_left, R.string.edge_bottom, R.string.edge_left),
        TOP_LEFT(false, false, R.string.corner_top_left, R.string.edge_top, R.string.edge_left),
        TOP_RIGHT(false, true, R.string.corner_top_right, R.string.edge_top, R.string.edge_right),
    }

    const val CORNER_KEY = "corner"

    fun getCorner(context: Context, trigger: Int = 0): Corner =
        Corner.entries.firstOrNull { it.name == prefs(context).getString(triggerKey(CORNER_KEY, trigger), null) } ?: Corner.BOTTOM_RIGHT

    fun setCorner(context: Context, corner: Corner, trigger: Int = 0) {
        prefs(context).edit().putString(triggerKey(CORNER_KEY, trigger), corner.name).apply()
    }

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * The triggers: any number, at least one. The first keeps the keys the app always used, so
     * a user's existing trigger carries over; trigger `n` stores its settings under the same key
     * with `_n` added.
     */
    private const val TRIGGER_COUNT_KEY = "trigger_count"
    const val TRIGGERS_MAX = 6

    private fun triggerKey(key: String, index: Int) = if (index == 0) key else "${key}_$index"

    fun getTriggerCount(context: Context): Int =
        prefs(context).getInt(TRIGGER_COUNT_KEY, 1).coerceIn(1, TRIGGERS_MAX)

    /** Adds a trigger beside the last one, in its corner, so it does not start hidden under it. Returns its index. */
    fun addTrigger(context: Context): Int {
        val index = getTriggerCount(context)
        val last = index - 1
        val beside = (get(context, Setting.HORIZONTAL_OFFSET, last) + get(context, Setting.WIDTH, last))
            .coerceAtMost(Setting.HORIZONTAL_OFFSET.max(context))
        prefs(context).edit()
            .apply { Setting.entries.forEach { remove(triggerKey(it.key, index)) } }
            .apply { ColorSetting.entries.forEach { remove(triggerKey(it.key, index)) } }
            .remove(triggerKey(CORNER_KEY, index))
            .putInt(triggerKey(Setting.HORIZONTAL_OFFSET.key, index), beside)
            .putString(triggerKey(CORNER_KEY, index), getCorner(context, last).name)
            .putInt(TRIGGER_COUNT_KEY, index + 1)
            .apply()
        return index
    }

    /** Removes a trigger; the ones after it move up. The last trigger cannot be removed. */
    fun removeTrigger(context: Context, index: Int) {
        val count = getTriggerCount(context)
        if (count <= 1 || index !in 0 until count) return
        val prefs = prefs(context)
        val editor = prefs.edit()
        for (i in index until count - 1) {
            Setting.entries.forEach { editor.putInt(triggerKey(it.key, i), get(context, it, i + 1)) }
            ColorSetting.entries.forEach { editor.putInt(triggerKey(it.key, i), getColor(context, it, i + 1)) }
            editor.putString(triggerKey(CORNER_KEY, i), getCorner(context, i + 1).name)
        }
        editor.remove(triggerKey(CORNER_KEY, count - 1))
        Setting.entries.forEach { editor.remove(triggerKey(it.key, count - 1)) }
        ColorSetting.entries.forEach { editor.remove(triggerKey(it.key, count - 1)) }
        editor.putInt(TRIGGER_COUNT_KEY, count - 1).apply()
    }

    fun get(context: Context, setting: Setting, trigger: Int = 0): Int =
        prefs(context).getInt(triggerKey(setting.key, trigger), setting.default)

    fun set(context: Context, setting: Setting, value: Int, trigger: Int = 0) {
        prefs(context).edit().putInt(triggerKey(setting.key, trigger), value).apply()
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

    fun getColor(context: Context, setting: ColorSetting, trigger: Int = 0): Int =
        prefs(context).getInt(triggerKey(setting.key, trigger), setting.default)

    fun setColor(context: Context, setting: ColorSetting, value: Int, trigger: Int = 0) {
        prefs(context).edit().putInt(triggerKey(setting.key, trigger), value).apply()
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

    /** Whether the fan lists apps by weekly foreground time instead of most recent use. */
    private const val MOST_USED_KEY = "most_used"

    fun getMostUsed(context: Context): Boolean = prefs(context).getBoolean(MOST_USED_KEY, false)

    fun setMostUsed(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(MOST_USED_KEY, value).apply()
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

    /** The old optional onboarding steps (fan layout, trigger, blacklist), whose progress still tells us who has set up before. */
    private const val LEGACY_ONBOARDING_KEY = "onboarding_optional_done"
    private const val INTRO_KEY = "onboarding_intro_seen"
    private const val FINISHED_KEY = "onboarding_finished"

    private fun legacyStepsDone(context: Context) = prefs(context).getInt(LEGACY_ONBOARDING_KEY, 0)

    /** Whether the intro page has been passed. Anyone already part-way through the old setup counts as having seen it. */
    fun getIntroSeen(context: Context): Boolean =
        prefs(context).getBoolean(INTRO_KEY, legacyStepsDone(context) > 0)

    fun setIntroSeen(context: Context) {
        prefs(context).edit().putBoolean(INTRO_KEY, true).apply()
    }

    /** Whether onboarding is over. Anyone who finished the old setup has already been through it. */
    fun getOnboardingFinished(context: Context): Boolean =
        prefs(context).getBoolean(FINISHED_KEY, legacyStepsDone(context) >= 3)

    fun setOnboardingFinished(context: Context) {
        prefs(context).edit().putBoolean(FINISHED_KEY, true).apply()
    }

    /** Whether [key] is a trigger's setting (of any trigger), or the trigger count. */
    fun isTriggerKey(key: String?): Boolean {
        if (key == TRIGGER_COUNT_KEY) return true
        val base = key?.replace(Regex("_\\d+$"), "") ?: return false
        return base == CORNER_KEY || Setting.entries.any { it.key == base } || ColorSetting.entries.any { it.key == base }
    }
}
