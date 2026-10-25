package com.lukeneedham.androiddock.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.ColorStop
import com.lukeneedham.androiddock.DockPrefs
import com.lukeneedham.androiddock.FanPreviewState
import com.lukeneedham.androiddock.SettingsPageContent
import com.lukeneedham.androiddock.SettingsUiState

/** Rendered by CI to docs/screenshots/<position in order.txt>-SettingsPage.png: the home page, set up and with the fan closed. */
@Preview
@Composable
private fun SettingsPageScreenshot() {
    val state = SettingsUiState(
        accessibilityOk = true,
        usageAccessOk = true,
        fan = FanPreviewState(
            inner = 100,
            bottomPad = 24,
            sidePad = 8,
            rows = listOf(DockPrefs.Row(3, 58), DockPrefs.Row(5, 59), DockPrefs.Row(8, 39)),
            showCurrent = true,
            stops = listOf(ColorStop(0f, 0xCC1565C0.toInt()), ColorStop(1f, 0x00000000)),
            corner = DockPrefs.Corner.BOTTOM_RIGHT,
        ),
        sortType = DockPrefs.SortType.entries.first(),
        hiddenCount = 2,
    )
    AppTheme(darkTheme = false) {
        SettingsPageContent(
            state = state,
            fanOpen = false,
            onOpenAccessibilitySettings = {},
            onOpenUsageAccessSettings = {},
            onToggleFan = {},
            onSortTypeSelect = {},
            onNavigate = {},
        )
    }
}
