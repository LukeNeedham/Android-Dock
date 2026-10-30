package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The gradient behind the fan, as colour [stops], and the [corner] the fan opens in. */
internal data class FanBackgroundUiState(val stops: List<ColorStop>, val corner: DockPrefs.Corner)
