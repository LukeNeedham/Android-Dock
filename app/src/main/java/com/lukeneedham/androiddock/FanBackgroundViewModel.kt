package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Reads and saves the background gradient of the fan. */
internal class FanBackgroundViewModel(private val settings: DockSettings) : ViewModel() {
    var uiState by mutableStateOf(
        FanBackgroundUiState(
            stops = settings.getSheetStops(),
            corner = settings.getCorner(),
        ),
    )
        private set

    fun onStopsChange(stops: List<ColorStop>) {
        settings.setSheetStops(stops)
        uiState = uiState.copy(stops = stops)
    }
}
