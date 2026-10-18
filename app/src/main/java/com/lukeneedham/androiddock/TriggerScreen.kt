package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

/**
 * The triggers on a page of their own: pick one from the list, then set where it sits, how big
 * it is, and its colour. Triggers can be added and removed, but one always remains.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TriggerScreen(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var count by remember { mutableIntStateOf(DockPrefs.getTriggerCount(context)) }
    var selected by remember { mutableIntStateOf(0) }
    // Bumped when a trigger is removed, as the one after it then takes its index.
    var generation by remember { mutableIntStateOf(0) }
    SubPage(R.string.position_title, onBack, footer, header) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.position_description), style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                (0 until count).forEach { i ->
                    val on = i == selected
                    Text(
                        stringResource(R.string.trigger_name, i + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) Color.White else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selected = i }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = DockPrefs.addTrigger(context)
                        count = DockPrefs.getTriggerCount(context)
                    },
                    enabled = count < DockPrefs.TRIGGERS_MAX,
                ) { Text(stringResource(R.string.trigger_add)) }
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        DockPrefs.removeTrigger(context, selected)
                        count = DockPrefs.getTriggerCount(context)
                        selected = selected.coerceAtMost(count - 1)
                        generation++
                    },
                    enabled = count > 1,
                ) { Text(stringResource(R.string.trigger_remove, selected + 1)) }
            }
            key(selected, generation) {
                var corner by remember { mutableStateOf(DockPrefs.getCorner(context, selected)) }
                SectionHeader(R.string.corner_title, R.string.corner_description)
                CornerPicker(corner) {
                    corner = it
                    DockPrefs.setCorner(context, it, selected)
                }
                DockPrefs.Setting.entries.forEach { setting ->
                    val label: @Composable (Int) -> String = when (setting) {
                        DockPrefs.Setting.HORIZONTAL_OFFSET -> { v -> stringResource(R.string.slider_edge, stringResource(corner.horizontalEdge), v) }
                        DockPrefs.Setting.VERTICAL_OFFSET -> { v -> stringResource(R.string.slider_edge, stringResource(corner.verticalEdge), v) }
                        DockPrefs.Setting.WIDTH -> { v -> stringResource(R.string.slider_width, v) }
                        DockPrefs.Setting.HEIGHT -> { v -> stringResource(R.string.slider_height, v) }
                    }
                    SettingSlider(context, setting, label, selected)
                }
                ColorSettingRow(context, DockPrefs.ColorSetting.BUTTON, R.string.color_button, selected)
            }
            NavBarSpacer()
        }
    }
}
