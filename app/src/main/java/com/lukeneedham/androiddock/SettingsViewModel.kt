package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** The fan as the small preview on the home page draws it: the settings it is made of. */
internal data class FanPreviewState(
    val inner: Int,
    val bottomPad: Int,
    val sidePad: Int,
    val rows: List<DockPrefs.Row>,
    val showCurrent: Boolean,
    val stops: List<ColorStop>,
    val corner: DockPrefs.Corner,
)

/** Reads the settings the home page shows, again on every [refresh] (when the user comes back to it). */
internal class SettingsViewModel(
    private val settings: DockSettings,
    private val setupStatus: SetupStatus,
) : ViewModel() {
    var uiState by mutableStateOf(load())
        private set

    fun refresh() {
        uiState = load()
    }

    fun onSortTypeSelect(sortType: DockPrefs.SortType) {
        settings.setSortType(sortType)
        uiState = uiState.copy(sortType = sortType)
    }

    private fun load(): SettingsUiState {
        return SettingsUiState(
            accessibilityOk = setupStatus.isAccessibilityEnabled(),
            usageAccessOk = setupStatus.isUsageAccessGranted(),
            fan = FanPreviewState(
                inner = settings.getCurrentWidth(),
                bottomPad = settings.getPadding(DockPrefs.Padding.BOTTOM),
                sidePad = settings.getPadding(DockPrefs.Padding.SIDE),
                rows = settings.getRows(),
                showCurrent = settings.getShowCurrent(),
                stops = settings.getSheetStops(),
                corner = settings.getCorner(),
            ),
            sortType = settings.getSortType(),
            hiddenCount = settings.getBlacklist().size,
        )
    }
}
