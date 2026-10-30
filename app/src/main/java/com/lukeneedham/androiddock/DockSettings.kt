package com.lukeneedham.androiddock

import android.content.Context

/** The dock's saved settings, for the ViewModels: [DockPrefs] with the [Context] held here instead of passed in. */
internal class DockSettings(private val context: Context) {
    fun getCorner(trigger: Int = 0) = DockPrefs.getCorner(context, trigger)
    fun setCorner(corner: DockPrefs.Corner, trigger: Int = 0) = DockPrefs.setCorner(context, corner, trigger)

    fun getTriggerCount() = DockPrefs.getTriggerCount(context)
    fun addTrigger() = DockPrefs.addTrigger(context)
    fun removeTrigger(index: Int) = DockPrefs.removeTrigger(context, index)

    fun get(setting: DockPrefs.Setting, trigger: Int = 0) = DockPrefs.get(context, setting, trigger)
    fun set(setting: DockPrefs.Setting, value: Int, trigger: Int = 0) = DockPrefs.set(context, setting, value, trigger)
    fun max(setting: DockPrefs.Setting) = setting.max(context)

    fun getColor(setting: DockPrefs.ColorSetting, trigger: Int = 0) = DockPrefs.getColor(context, setting, trigger)
    fun setColor(setting: DockPrefs.ColorSetting, value: Int, trigger: Int = 0) =
        DockPrefs.setColor(context, setting, value, trigger)

    fun getSheetStops() = DockPrefs.getSheetStops(context)
    fun setSheetStops(stops: List<ColorStop>) = DockPrefs.setSheetStops(context, stops)

    fun getRows() = DockPrefs.getRows(context)
    fun setRows(rows: List<DockPrefs.Row>) = DockPrefs.setRows(context, rows)

    fun getCurrentWidth() = DockPrefs.getCurrentWidth(context)
    fun setCurrentWidth(value: Int) = DockPrefs.setCurrentWidth(context, value)

    fun getShowCurrent() = DockPrefs.getShowCurrent(context)
    fun setShowCurrent(value: Boolean) = DockPrefs.setShowCurrent(context, value)

    fun getPadding(padding: DockPrefs.Padding) = DockPrefs.getPadding(context, padding)
    fun setPadding(padding: DockPrefs.Padding, value: Int) = DockPrefs.setPadding(context, padding, value)

    fun getSortType() = DockPrefs.getSortType(context)
    fun setSortType(sortType: DockPrefs.SortType) = DockPrefs.setSortType(context, sortType)

    fun getBlacklist() = DockPrefs.getBlacklist(context)
    fun setBlacklist(packages: Collection<String>) = DockPrefs.setBlacklist(context, packages)

    fun getIntroSeen() = DockPrefs.getIntroSeen(context)
    fun setIntroSeen() = DockPrefs.setIntroSeen(context)
    fun getOnboardingFinished() = DockPrefs.getOnboardingFinished(context)
    fun setOnboardingFinished() = DockPrefs.setOnboardingFinished(context)
}
