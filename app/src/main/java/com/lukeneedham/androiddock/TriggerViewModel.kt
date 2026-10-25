package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** One of the numbers that place a trigger, with the largest it can be. */
internal data class TriggerSlider(val setting: DockPrefs.Setting, val value: Int, val max: Int)

/** The triggers and the settings of the [selected] one (an index into the [count] triggers). */
internal data class TriggerUiState(
    val count: Int,
    val selected: Int,
    val corner: DockPrefs.Corner,
    val sliders: List<TriggerSlider>,
    val buttonColor: Int,
)

/** Reads and saves the settings of the triggers; the running service applies every change live. */
internal class TriggerViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(load(selected = 0))
        private set

    private val context get() = getApplication<Application>()

    fun onSelect(index: Int) {
        uiState = load(index)
    }

    fun onAdd() {
        uiState = load(DockPrefs.addTrigger(context))
    }

    fun onRemove() {
        DockPrefs.removeTrigger(context, uiState.selected)
        uiState = load(uiState.selected.coerceAtMost(DockPrefs.getTriggerCount(context) - 1))
    }

    fun onCornerChange(corner: DockPrefs.Corner) {
        DockPrefs.setCorner(context, corner, uiState.selected)
        uiState = load(uiState.selected)
    }

    fun onSliderChange(setting: DockPrefs.Setting, value: Int) {
        DockPrefs.set(context, setting, value, uiState.selected)
        uiState = uiState.copy(sliders = uiState.sliders.map { if (it.setting == setting) it.copy(value = value) else it })
    }

    fun onButtonColorChange(argb: Int) {
        DockPrefs.setColor(context, DockPrefs.ColorSetting.BUTTON, argb, uiState.selected)
        uiState = uiState.copy(buttonColor = argb)
    }

    private fun load(selected: Int) = TriggerUiState(
        count = DockPrefs.getTriggerCount(context),
        selected = selected,
        corner = DockPrefs.getCorner(context, selected),
        sliders = DockPrefs.Setting.entries.map { TriggerSlider(it, DockPrefs.get(context, it, selected), it.max(context)) },
        buttonColor = DockPrefs.getColor(context, DockPrefs.ColorSetting.BUTTON, selected),
    )
}
