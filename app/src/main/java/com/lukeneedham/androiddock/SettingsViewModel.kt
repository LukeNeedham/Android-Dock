package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

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

/** What the home page shows. */
internal data class SettingsUiState(
    val accessibilityOk: Boolean,
    val usageAccessOk: Boolean,
    val fan: FanPreviewState,
    val sortType: DockPrefs.SortType,
    val hiddenCount: Int,
)

/** Reads the settings the home page shows, again on every [refresh] (when the user comes back to it). */
internal class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(load())
        private set

    fun refresh() {
        uiState = load()
    }

    fun onSortTypeSelect(sortType: DockPrefs.SortType) {
        DockPrefs.setSortType(getApplication(), sortType)
        uiState = uiState.copy(sortType = sortType)
    }

    private fun load(): SettingsUiState {
        val context = getApplication<Application>()
        return SettingsUiState(
            accessibilityOk = SetupState.isAccessibilityEnabled(context),
            usageAccessOk = SetupState.isUsageAccessGranted(context),
            fan = FanPreviewState(
                inner = DockPrefs.getCurrentWidth(context),
                bottomPad = DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM),
                sidePad = DockPrefs.getPadding(context, DockPrefs.Padding.SIDE),
                rows = DockPrefs.getRows(context),
                showCurrent = DockPrefs.getShowCurrent(context),
                stops = DockPrefs.getSheetStops(context),
                corner = DockPrefs.getCorner(context),
            ),
            sortType = DockPrefs.getSortType(context),
            hiddenCount = DockPrefs.getBlacklist(context).size,
        )
    }
}
