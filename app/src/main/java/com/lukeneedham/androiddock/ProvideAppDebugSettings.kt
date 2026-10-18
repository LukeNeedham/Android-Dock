package com.lukeneedham.androiddock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.lukeneedham.common.ui.DebugTheme
import com.lukeneedham.common.ui.ProvideDebugSettings

/**
 * [ProvideDebugSettings] with the shared library's debug pages themed to match the app's Material
 * theme. Place it inside the app's theme, since it reads the colours and font from there.
 */
@Composable
fun ProvideAppDebugSettings(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ProvideDebugSettings(
        theme = DebugTheme(
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surfaceVariant,
            onSurface = colors.onSurfaceVariant,
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            outline = colors.outline,
            error = colors.error,
            warning = colors.tertiary,
            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily ?: FontFamily.Default,
        ),
        content = content,
    )
}
