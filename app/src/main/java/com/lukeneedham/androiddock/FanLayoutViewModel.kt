package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Reads and saves the fan's layout; the running service applies every change live. */
internal class FanLayoutViewModel(private val settings: DockSettings) : ViewModel() {
    var uiState by mutableStateOf(
        FanLayoutUiState(
            corner = settings.getCorner(),
            innerWidth = settings.getCurrentWidth(),
            showCurrent = settings.getShowCurrent(),
            bottomPad = settings.getPadding(DockPrefs.Padding.BOTTOM),
            sidePad = settings.getPadding(DockPrefs.Padding.SIDE),
            rows = settings.getRows(),
            // The background is edited on its own page; here it only shows behind the fan.
            stops = settings.getSheetStops(),
            selected = FanPart.Ring(0),
        ),
    )
        private set

    fun onSelect(part: FanPart) {
        uiState = uiState.copy(selected = part)
    }

    fun onAddRow() {
        val rows = uiState.rows + DockPrefs.newRow()
        settings.setRows(rows)
        uiState = uiState.copy(rows = rows, selected = FanPart.Ring(rows.lastIndex))
    }

    fun onDeleteRow(index: Int) {
        val rows = uiState.rows.filterIndexed { i, _ -> i != index }
        settings.setRows(rows)
        uiState = uiState.copy(rows = rows, selected = FanPart.Ring(index.coerceAtMost(rows.lastIndex)))
    }

    fun onRowCountChange(index: Int, count: Int) = changeRow(index) { it.copy(count = count) }

    fun onRowWidthChange(index: Int, widthDp: Int) = changeRow(index) { it.copy(widthDp = widthDp) }

    fun onBottomPadChange(value: Int) {
        settings.setPadding(DockPrefs.Padding.BOTTOM, value)
        uiState = uiState.copy(bottomPad = value)
    }

    fun onSidePadChange(value: Int) {
        settings.setPadding(DockPrefs.Padding.SIDE, value)
        uiState = uiState.copy(sidePad = value)
    }

    fun onCurrentWidthChange(value: Int) {
        settings.setCurrentWidth(value)
        uiState = uiState.copy(innerWidth = value)
    }

    fun onShowCurrentChange(value: Boolean) {
        settings.setShowCurrent(value)
        uiState = uiState.copy(showCurrent = value)
    }

    private fun changeRow(index: Int, change: (DockPrefs.Row) -> DockPrefs.Row) {
        val rows = uiState.rows.mapIndexed { i, row -> if (i == index) change(row) else row }
        settings.setRows(rows)
        uiState = uiState.copy(rows = rows)
    }
}
