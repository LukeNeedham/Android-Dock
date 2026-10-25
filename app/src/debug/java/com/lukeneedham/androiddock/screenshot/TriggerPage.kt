package com.lukeneedham.androiddock.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.DockPrefs
import com.lukeneedham.androiddock.TriggerPageContent
import com.lukeneedham.androiddock.TriggerSlider
import com.lukeneedham.androiddock.TriggerUiState

/** Rendered by CI to docs/screenshots/<position in order.txt>-TriggerPage.png: two triggers, the first one's position and size. */
@Preview
@Composable
private fun TriggerPageScreenshot() {
    val state = TriggerUiState(
        count = 2,
        selected = 0,
        corner = DockPrefs.Corner.BOTTOM_RIGHT,
        sliders = listOf(
            TriggerSlider(DockPrefs.Setting.HORIZONTAL_OFFSET, 0, 360),
            TriggerSlider(DockPrefs.Setting.VERTICAL_OFFSET, 24, 160),
            TriggerSlider(DockPrefs.Setting.WIDTH, 31, 240),
            TriggerSlider(DockPrefs.Setting.HEIGHT, 24, 240),
        ),
        buttonColor = 0x661565C0,
    )
    AppTheme(darkTheme = false) {
        TriggerPageContent(
            state = state,
            onBack = {},
            onSelect = {},
            onAdd = {},
            onRemove = {},
            onCornerChange = {},
            onSliderChange = { _, _ -> },
            onButtonColorChange = {},
        )
    }
}
