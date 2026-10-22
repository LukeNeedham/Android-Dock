package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            AppText(stringResource(R.string.position_description), fontSize = 14.sp, lineHeight = 20.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                (0 until count).forEach { i ->
                    val on = i == selected
                    AppButton(
                        onClick = { selected = i },
                        style = if (on) AppButtonStyle.Primary else AppButtonStyle.Secondary,
                    ) { AppButtonLabel(stringResource(R.string.trigger_name, i + 1)) }
                }
                if (count < DockPrefs.TRIGGERS_MAX) {
                    AppIconButton(
                        iconRes = R.drawable.ic_add,
                        contentDescription = stringResource(R.string.trigger_add),
                        style = AppButtonStyle.Secondary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selected = DockPrefs.addTrigger(context)
                            count = DockPrefs.getTriggerCount(context)
                        },
                    )
                }
            }
            AppButton(
                style = AppButtonStyle.Secondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    DockPrefs.removeTrigger(context, selected)
                    count = DockPrefs.getTriggerCount(context)
                    selected = selected.coerceAtMost(count - 1)
                    generation++
                },
                enabled = count > 1,
                modifier = Modifier.padding(top = 20.dp),
            ) { AppButtonLabel(stringResource(R.string.trigger_remove, selected + 1)) }
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
