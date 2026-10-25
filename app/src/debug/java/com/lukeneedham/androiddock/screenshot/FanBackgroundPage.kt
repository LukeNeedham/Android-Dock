package com.lukeneedham.androiddock.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.ColorStop
import com.lukeneedham.androiddock.DockPrefs
import com.lukeneedham.androiddock.FanBackgroundPageContent
import com.lukeneedham.androiddock.FanBackgroundUiState

/** Rendered by CI to docs/screenshots/<position in order.txt>-FanBackgroundPage.png: a two-stop blue gradient behind the fan. */
@Preview
@Composable
private fun FanBackgroundPageScreenshot() {
    val state = FanBackgroundUiState(
        stops = listOf(ColorStop(0f, 0xFF1565C0.toInt()), ColorStop(1f, 0x00000000)),
        corner = DockPrefs.Corner.BOTTOM_RIGHT,
    )
    AppTheme(darkTheme = false) {
        FanBackgroundPageContent(
            state = state,
            onBack = {},
            onStopsChange = {},
        )
    }
}
