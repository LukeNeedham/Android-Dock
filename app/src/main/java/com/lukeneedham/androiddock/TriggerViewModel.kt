package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** One of the numbers that place a trigger, with the largest it can be. */
internal data class TriggerSlider(val setting: DockPrefs.Setting, val value: Int, val max: Int)

/** Reads and saves the settings of the triggers; the running service applies every change live. */
internal class TriggerViewModel(private val settings: DockSettings) : ViewModel() {
    var uiState by mutableStateOf(load(selected = 0))
        private set

    fun onSelect(index: Int) {
        uiState = load(index)
    }

    fun onAdd() {
        uiState = load(settings.addTrigger())
    }

    fun onRemove() {
        settings.removeTrigger(uiState.selected)
        uiState = load(uiState.selected.coerceAtMost(settings.getTriggerCount() - 1))
    }

    fun onCornerChange(corner: DockPrefs.Corner) {
        settings.setCorner(corner, uiState.selected)
        uiState = load(uiState.selected)
    }

    fun onSliderChange(setting: DockPrefs.Setting, value: Int) {
        settings.set(setting, value, uiState.selected)
        uiState = uiState.copy(sliders = uiState.sliders.map { if (it.setting == setting) it.copy(value = value) else it })
    }

    fun onButtonColorChange(argb: Int) {
        settings.setColor(DockPrefs.ColorSetting.BUTTON, argb, uiState.selected)
        uiState = uiState.copy(buttonColor = argb)
    }

    private fun load(selected: Int) = TriggerUiState(
        count = settings.getTriggerCount(),
        selected = selected,
        corner = settings.getCorner(selected),
        sliders = DockPrefs.Setting.entries.map { TriggerSlider(it, settings.get(it, selected), settings.max(it)) },
        buttonColor = settings.getColor(DockPrefs.ColorSetting.BUTTON, selected),
    )
}
