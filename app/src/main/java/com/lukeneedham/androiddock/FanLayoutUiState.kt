package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The fan's layout as the page edits it. [selected] is the part of the fan the controls edit, and [currentWidth] the
 * width of the ring for the current app, never narrower than its smallest.
 */
internal data class FanLayoutUiState(
    val corner: DockPrefs.Corner,
    val innerWidth: Int,
    val showCurrent: Boolean,
    val bottomPad: Int,
    val sidePad: Int,
    val rows: List<DockPrefs.Row>,
    val stops: List<ColorStop>,
    val selected: FanPart,
) {
    val currentWidth: Int get() = innerWidth.coerceAtLeast(DockPrefs.currentWidthMin(showCurrent))
}
