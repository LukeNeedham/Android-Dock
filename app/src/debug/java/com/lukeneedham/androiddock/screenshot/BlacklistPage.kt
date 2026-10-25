package com.lukeneedham.androiddock.screenshot

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppEntry
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.BlacklistPageContent
import com.lukeneedham.androiddock.BlacklistUiState

/** Rendered by CI to docs/screenshots/<position in order.txt>-BlacklistPage.png: three hidden apps, each with a plain coloured icon. */
@Preview
@Composable
private fun BlacklistPageScreenshot() {
    fun icon(color: Int): ImageBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }.asImageBitmap()
    val state = BlacklistUiState(
        hidden = listOf(
            AppEntry("com.example.calculator", "Calculator", icon(0xFF4CAF50.toInt())),
            AppEntry("com.example.maps", "Maps", icon(0xFF2196F3.toInt())),
            AppEntry("com.example.news", "News", icon(0xFFFF9800.toInt())),
        ),
    )
    AppTheme(darkTheme = false) {
        BlacklistPageContent(
            state = state,
            onBack = {},
            onRemove = {},
            onAdd = {},
            onPickerOpen = {},
        )
    }
}
