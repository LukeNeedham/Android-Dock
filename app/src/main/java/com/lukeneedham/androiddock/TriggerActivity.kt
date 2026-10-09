package com.lukeneedham.androiddock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
class TriggerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { TriggerScreen() }
            }
        }
    }
}

@Composable
private fun TriggerScreen() {
    val context = LocalContext.current
    SubPage(R.string.position_title) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.position_description), style = MaterialTheme.typography.bodyMedium)
            DockPrefs.Setting.entries.forEach { setting ->
                val label = when (setting) {
                    DockPrefs.Setting.RIGHT_OFFSET -> R.string.slider_right
                    DockPrefs.Setting.BOTTOM_OFFSET -> R.string.slider_bottom
                    DockPrefs.Setting.WIDTH -> R.string.slider_width
                    DockPrefs.Setting.HEIGHT -> R.string.slider_height
                }
                SettingSlider(context, setting, label)
            }
            ColorSettingRow(context, DockPrefs.ColorSetting.BUTTON, R.string.color_button)
        }
    }
}
