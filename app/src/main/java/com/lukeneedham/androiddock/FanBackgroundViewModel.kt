package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** Reads and saves the background gradient of the fan. */
internal class FanBackgroundViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(
        FanBackgroundUiState(
            stops = DockPrefs.getSheetStops(application),
            corner = DockPrefs.getCorner(application),
        ),
    )
        private set

    fun onStopsChange(stops: List<ColorStop>) {
        DockPrefs.setSheetStops(getApplication(), stops)
        uiState = uiState.copy(stops = stops)
    }
}
