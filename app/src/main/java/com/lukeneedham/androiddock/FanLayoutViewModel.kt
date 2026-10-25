package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** Reads and saves the fan's layout; the running service applies every change live. */
internal class FanLayoutViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    var uiState by mutableStateOf(
        FanLayoutUiState(
            corner = DockPrefs.getCorner(application),
            innerWidth = DockPrefs.getCurrentWidth(application),
            showCurrent = DockPrefs.getShowCurrent(application),
            bottomPad = DockPrefs.getPadding(application, DockPrefs.Padding.BOTTOM),
            sidePad = DockPrefs.getPadding(application, DockPrefs.Padding.SIDE),
            rows = DockPrefs.getRows(application),
            // The background is edited on its own page; here it only shows behind the fan.
            stops = DockPrefs.getSheetStops(application),
            selected = FanPart.Ring(0),
        ),
    )
        private set

    fun onSelect(part: FanPart) {
        uiState = uiState.copy(selected = part)
    }

    fun onAddRow() {
        val rows = uiState.rows + DockPrefs.newRow()
        DockPrefs.setRows(context, rows)
        uiState = uiState.copy(rows = rows, selected = FanPart.Ring(rows.lastIndex))
    }

    fun onDeleteRow(index: Int) {
        val rows = uiState.rows.filterIndexed { i, _ -> i != index }
        DockPrefs.setRows(context, rows)
        uiState = uiState.copy(rows = rows, selected = FanPart.Ring(index.coerceAtMost(rows.lastIndex)))
    }

    fun onRowCountChange(index: Int, count: Int) = changeRow(index) { it.copy(count = count) }

    fun onRowWidthChange(index: Int, widthDp: Int) = changeRow(index) { it.copy(widthDp = widthDp) }

    fun onBottomPadChange(value: Int) {
        DockPrefs.setPadding(context, DockPrefs.Padding.BOTTOM, value)
        uiState = uiState.copy(bottomPad = value)
    }

    fun onSidePadChange(value: Int) {
        DockPrefs.setPadding(context, DockPrefs.Padding.SIDE, value)
        uiState = uiState.copy(sidePad = value)
    }

    fun onCurrentWidthChange(value: Int) {
        DockPrefs.setCurrentWidth(context, value)
        uiState = uiState.copy(innerWidth = value)
    }

    fun onShowCurrentChange(value: Boolean) {
        DockPrefs.setShowCurrent(context, value)
        uiState = uiState.copy(showCurrent = value)
    }

    private fun changeRow(index: Int, change: (DockPrefs.Row) -> DockPrefs.Row) {
        val rows = uiState.rows.mapIndexed { i, row -> if (i == index) change(row) else row }
        DockPrefs.setRows(context, rows)
        uiState = uiState.copy(rows = rows)
    }
}
