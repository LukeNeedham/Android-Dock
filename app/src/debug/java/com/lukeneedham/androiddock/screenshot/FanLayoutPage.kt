package com.lukeneedham.androiddock.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.ColorStop
import com.lukeneedham.androiddock.DockPrefs
import com.lukeneedham.androiddock.FanLayoutPageContent
import com.lukeneedham.androiddock.FanLayoutUiState
import com.lukeneedham.androiddock.FanPart

/** Rendered by CI to docs/screenshots/<position in order.txt>-FanLayoutPage.png: the fan with its three rows, the first selected. */
@Preview
@Composable
private fun FanLayoutPageScreenshot() {
    val state = FanLayoutUiState(
        corner = DockPrefs.Corner.BOTTOM_RIGHT,
        innerWidth = 100,
        showCurrent = true,
        bottomPad = 24,
        sidePad = 8,
        rows = listOf(DockPrefs.Row(3, 58), DockPrefs.Row(5, 59), DockPrefs.Row(8, 39)),
        stops = listOf(ColorStop(0f, 0xCC1565C0.toInt()), ColorStop(1f, 0x00000000)),
        selected = FanPart.Ring(0),
    )
    AppTheme(darkTheme = false) {
        FanLayoutPageContent(
            state = state,
            onBack = {},
            onSelect = {},
            onAddRow = {},
            onDeleteRow = {},
            onRowCountChange = { _, _ -> },
            onRowWidthChange = { _, _ -> },
            onBottomPadChange = {},
            onSidePadChange = {},
            onCurrentWidthChange = {},
            onShowCurrentChange = {},
        )
    }
}
