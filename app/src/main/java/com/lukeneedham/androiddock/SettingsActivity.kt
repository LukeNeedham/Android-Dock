package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding

/**
 * What the user sees when they open the app: a checklist that walks them through the setup the
 * dock needs, plus the touch target and colour settings. The setup steps are re-checked every
 * time the user comes back from system settings.
 */
class SettingsActivity : ComponentActivity() {

    /** Bumped on every resume so the setup checks re-run. */
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    SettingsScreen(resumes)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }
}

@Composable
private fun SettingsScreen(resumes: Int) {
    val context = LocalContext.current
    // Reading [resumes] here makes the checks below re-run after returning from system settings.
    val accessibilityDone = resumes >= 0 && SetupState.isAccessibilityEnabled(context)
    val usageDone = resumes >= 0 && SetupState.isUsageAccessGranted(context)

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.onboarding_intro),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepCard(
                title = R.string.step_accessibility_title,
                description = R.string.step_accessibility_description,
                hint = R.string.step_accessibility_hint,
                done = accessibilityDone,
                action = R.string.step_accessibility_action,
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                secondaryAction = R.string.step_app_info_action,
                onSecondaryAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            )
            StepCard(
                title = R.string.step_usage_title,
                description = R.string.step_usage_description,
                done = usageDone,
                action = R.string.step_usage_action,
                onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            )
        }
        if (accessibilityDone && usageDone) {
            Text(
                stringResource(R.string.onboarding_all_set),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Button(
            onClick = { context.startActivity(Intent(context, DockActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.onboarding_try_it)) }

        SectionHeader(R.string.position_title, R.string.position_description)
        DockPrefs.Setting.entries.forEach { setting ->
            val label = when (setting) {
                DockPrefs.Setting.RIGHT_OFFSET -> R.string.slider_right
                DockPrefs.Setting.BOTTOM_OFFSET -> R.string.slider_bottom
                DockPrefs.Setting.WIDTH -> R.string.slider_width
                DockPrefs.Setting.HEIGHT -> R.string.slider_height
            }
            SettingSlider(context, setting, label)
        }

        SectionHeader(R.string.sheet_title_section, null)
        var maxItems by remember { mutableIntStateOf(DockPrefs.getMaxItems(context)) }
        Text(stringResource(R.string.slider_max_items, maxItems), modifier = Modifier.padding(top = 16.dp))
        Slider(
            value = maxItems.toFloat(),
            onValueChange = {
                maxItems = it.toInt()
                DockPrefs.setMaxItems(context, maxItems)
            },
            valueRange = DockPrefs.MAX_ITEMS_MIN.toFloat()..DockPrefs.MAX_ITEMS_MAX.toFloat(),
            steps = DockPrefs.MAX_ITEMS_MAX - DockPrefs.MAX_ITEMS_MIN - 1,
        )
        var alignRight by remember { mutableStateOf(DockPrefs.isAlignRight(context)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.align_right), modifier = Modifier.weight(1f))
            Switch(
                checked = alignRight,
                onCheckedChange = {
                    alignRight = it
                    DockPrefs.setAlignRight(context, it)
                },
            )
        }

        SectionHeader(R.string.colors_title, null)
        ColorPicker(context, DockPrefs.ColorSetting.BUTTON, R.string.color_button, showAlpha = true)
        ColorPicker(context, DockPrefs.ColorSetting.SHEET, R.string.color_sheet, showAlpha = false)

        TextButton(
            onClick = { context.startActivity(Intent(context, LogActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.onboarding_view_log)) }
    }
}

@Composable
private fun SectionHeader(title: Int, description: Int?) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp),
    )
    if (description != null) {
        Text(
            stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StepCard(
    title: Int,
    description: Int,
    done: Boolean,
    action: Int,
    onAction: () -> Unit,
    hint: Int? = null,
    secondaryAction: Int? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val text = stringResource(title)
            Text(if (done) "$text  ✓" else text, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (done) return@Column
            if (hint != null) {
                Text(
                    stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(action))
            }
            if (secondaryAction != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) { Text(stringResource(secondaryAction)) }
            }
        }
    }
}

/** A labelled slider that saves its value as it moves; the running service applies it live. */
@Composable
private fun SettingSlider(context: Context, setting: DockPrefs.Setting, label: Int) {
    var value by remember { mutableIntStateOf(DockPrefs.get(context, setting)) }
    Text(
        stringResource(label, value),
        modifier = Modifier.padding(top = 16.dp),
    )
    Slider(
        value = value.toFloat(),
        onValueChange = {
            value = it.toInt()
            DockPrefs.set(context, setting, value)
        },
        valueRange = setting.min.toFloat()..setting.max(context).toFloat(),
    )
}

/** Red, green, blue (and optionally opacity) sliders with a swatch; saves as they move. */
@Composable
private fun ColorPicker(context: Context, setting: DockPrefs.ColorSetting, label: Int, showAlpha: Boolean) {
    var argb by remember { mutableIntStateOf(DockPrefs.getColor(context, setting)) }
    fun update(newArgb: Int) {
        argb = newArgb
        DockPrefs.setColor(context, setting, newArgb)
    }

    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    // Drawn over a mid-grey so a translucent colour can be judged.
    Box(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp).background(Color.Gray)) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(argb)))
    }
    fun channel(shift: Int) = (argb ushr shift) and 0xFF
    fun with(shift: Int, v: Int) = (argb and (0xFF shl shift).inv()) or (v shl shift)

    val channels = buildList {
        if (showAlpha) add(Triple(R.string.color_alpha, 24, 0))
        add(Triple(R.string.color_red, 16, 0))
        add(Triple(R.string.color_green, 8, 0))
        add(Triple(R.string.color_blue, 0, 0))
    }
    channels.forEach { (name, shift, _) ->
        Text(stringResource(name, channel(shift)))
        Slider(
            value = channel(shift).toFloat(),
            onValueChange = { update(with(shift, it.toInt())) },
            valueRange = 0f..255f,
        )
    }
}
