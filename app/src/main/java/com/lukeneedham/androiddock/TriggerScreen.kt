package com.lukeneedham.androiddock

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** The trigger on a page of its own: where it sits, how big it is, and its colour. */
@Composable
internal fun TriggerScreen(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    val corner = DockPrefs.getCorner(context)
    SubPage(R.string.position_title, onBack, footer, header) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.position_description), style = MaterialTheme.typography.bodyMedium)
            DockPrefs.Setting.entries.forEach { setting ->
                val label: @Composable (Int) -> String = when (setting) {
                    DockPrefs.Setting.HORIZONTAL_OFFSET -> { v -> stringResource(R.string.slider_edge, stringResource(corner.horizontalEdge), v) }
                    DockPrefs.Setting.VERTICAL_OFFSET -> { v -> stringResource(R.string.slider_edge, stringResource(corner.verticalEdge), v) }
                    DockPrefs.Setting.WIDTH -> { v -> stringResource(R.string.slider_width, v) }
                    DockPrefs.Setting.HEIGHT -> { v -> stringResource(R.string.slider_height, v) }
                }
                SettingSlider(context, setting, label)
            }
            ColorSettingRow(context, DockPrefs.ColorSetting.BUTTON, R.string.color_button)
            NavBarSpacer()
        }
    }
}
