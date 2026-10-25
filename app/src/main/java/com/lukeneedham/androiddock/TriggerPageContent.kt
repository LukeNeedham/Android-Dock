package com.lukeneedham.androiddock

import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
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
internal fun TriggerPageContent(
    state: TriggerUiState,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onCornerChange: (DockPrefs.Corner) -> Unit,
    onSliderChange: (DockPrefs.Setting, Int) -> Unit,
    onButtonColorChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val corner = state.corner
    SubPage(R.string.position_title, onBack, footer, header) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.position_description), fontSize = 14.sp, lineHeight = 20.sp, color = AppTheme.current.colors.text)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                (0 until state.count).forEach { i ->
                    val on = i == state.selected
                    AppButton(
                        text = stringResource(R.string.trigger_name, i + 1),
                        onClick = { onSelect(i) },
                        style = if (on) AppButtonStyle.Primary else AppButtonStyle.Secondary,
                    )
                }
                if (state.count < DockPrefs.TRIGGERS_MAX) {
                    AppIconButton(
                        iconRes = R.drawable.ic_add,
                        contentDescription = stringResource(R.string.trigger_add),
                        style = AppButtonStyle.Secondary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAdd()
                        },
                    )
                }
            }
            AppButton(
                text = stringResource(R.string.trigger_remove, state.selected + 1),
                style = AppButtonStyle.Secondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRemove()
                },
                enabled = state.count > 1,
                modifier = Modifier.padding(top = 20.dp),
            )
            SectionHeader(R.string.corner_title, R.string.corner_description)
            CornerPicker(corner, onCornerChange)
            state.sliders.forEach { slider ->
                val label = when (slider.setting) {
                    DockPrefs.Setting.HORIZONTAL_OFFSET -> stringResource(R.string.slider_edge, stringResource(corner.horizontalEdge), slider.value)
                    DockPrefs.Setting.VERTICAL_OFFSET -> stringResource(R.string.slider_edge, stringResource(corner.verticalEdge), slider.value)
                    DockPrefs.Setting.WIDTH -> stringResource(R.string.slider_width, slider.value)
                    DockPrefs.Setting.HEIGHT -> stringResource(R.string.slider_height, slider.value)
                }
                SettingSlider(label, slider.value, slider.setting.min, slider.max) { onSliderChange(slider.setting, it) }
            }
            ColorSettingRow(state.buttonColor, R.string.color_button, onButtonColorChange)
            NavBarSpacer()
        }
    }
}

/** A labelled slider for a whole number from [min] to [max]. */
@Composable
internal fun SettingSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Text(
        label,
        modifier = Modifier.padding(top = 16.dp),
        color = AppTheme.current.colors.text,
    )
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = min.toFloat()..max.toFloat(),
        colors = appSliderColors(),
    )
}

/** A colour setting shown as a swatch; tapping it opens [ColorPickerSheet]. */
@Composable
internal fun ColorSettingRow(argb: Int, label: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(top = 16.dp),
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f), color = AppTheme.current.colors.text)
        ColorSwatch(argb, Modifier.width(72.dp).height(32.dp))
    }
    if (open) {
        ColorPickerSheet(title = label, argb = argb, onChange = onChange, onDismiss = { open = false })
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        TriggerPageContent(
            state = TriggerUiState(
                count = 2,
                selected = 0,
                corner = DockPrefs.Corner.BOTTOM_RIGHT,
                sliders = DockPrefs.Setting.entries.map { TriggerSlider(it, it.default, 200) },
                buttonColor = 0x66FFFFFF,
            ),
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
