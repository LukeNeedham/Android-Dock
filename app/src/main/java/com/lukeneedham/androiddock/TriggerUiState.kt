package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The triggers and the settings of the [selected] one (an index into the [count] triggers). */
internal data class TriggerUiState(
    val count: Int,
    val selected: Int,
    val corner: DockPrefs.Corner,
    val sliders: List<TriggerSlider>,
    val buttonColor: Int,
)
