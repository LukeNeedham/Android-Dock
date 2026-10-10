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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The pages of the settings app. */
internal enum class Route { Settings, Trigger, FanLayout, Blacklist, Log }

/**
 * What the user sees when they open the app: a checklist that walks them through the setup the
 * dock needs, plus links to the trigger and fan layout pages. The setup steps are re-checked every
 * time the user comes back from system settings.
 */
class SettingsActivity : ComponentActivity() {

    /** Bumped on every resume so the setup checks re-run. */
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openCount++
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(resumes)
                }
            }
        }
    }

    override fun onDestroy() {
        openCount--
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }

    companion object {
        @Volatile
        private var openCount = 0

        /** Whether the settings app is open now, so the sheet lists it like any other app. */
        val isOpen get() = openCount > 0
    }
}

/** The settings app's navigation: a back stack of [Route]s, shown by Navigation 3. */
@Composable
private fun AppNavigation(resumes: Int) {
    // Saved as route names, so the stack survives rotation and the process being recreated.
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Route>, String>(
            save = { stack -> stack.map { it.name } },
            restore = { names -> names.map { Route.valueOf(it) }.toMutableStateList() },
        ),
    ) { mutableStateListOf(Route.Settings) }
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    Route.Settings -> SettingsScreen(resumes) { backStack.add(it) }
                    Route.Trigger -> TriggerScreen(back)
                    Route.FanLayout -> FanLayoutScreen(back)
                    Route.Blacklist -> BlacklistScreen(back)
                    Route.Log -> LogScreen(back)
                }
            }
        },
    )
}

@Composable
private fun SettingsScreen(resumes: Int, navigate: (Route) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                DockActivity.starting = true
                context.startActivity(Intent(context, DockActivity::class.java))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.onboarding_try_it)) }

        SectionHeader(R.string.position_title, R.string.trigger_summary)
        FilledTonalButton(
            onClick = { navigate(Route.Trigger) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.trigger_open)) }

        SectionHeader(R.string.sheet_title_section, R.string.fan_layout_summary)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            FilledTonalButton(
                onClick = { navigate(Route.FanLayout) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.fan_layout_open)) }
            // A small, read-only picture of the fan as it is now, refreshed on every return here.
            val sheetColor = if (resumes >= 0) DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET) else 0
            val edgeColor = if (resumes >= 0) DockPrefs.getColor(context, DockPrefs.ColorSetting.SHEET_EDGE) else 0
            FanPreview(
                inner = if (resumes >= 0) DockPrefs.getInnerOffset(context) else 0,
                bottomPad = DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM),
                sidePad = DockPrefs.getPadding(context, DockPrefs.Padding.SIDE),
                rows = if (resumes >= 0) DockPrefs.getRows(context) else emptyList(),
                editingRow = null,
                sheetColor = sheetColor,
                edgeColor = edgeColor,
                thumbnailDp = 96,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        SectionHeader(R.string.blacklist_title, R.string.blacklist_description)
        FilledTonalButton(
            onClick = { navigate(Route.Blacklist) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.blacklist_open)) }

        TextButton(
            onClick = { navigate(Route.Log) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.onboarding_view_log)) }
    }
}

@Composable
internal fun SectionHeader(title: Int, description: Int?) {
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
internal fun SettingSlider(context: Context, setting: DockPrefs.Setting, label: Int) {
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

/** A colour setting shown as a swatch; tapping it opens [ColorPickerSheet]. */
@Composable
internal fun ColorSettingRow(
    context: Context,
    setting: DockPrefs.ColorSetting,
    label: Int,
    onChange: (Int) -> Unit = {},
) {
    var argb by remember { mutableIntStateOf(DockPrefs.getColor(context, setting)) }
    var open by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(top = 16.dp),
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        ColorSwatch(argb, Modifier.width(72.dp).height(32.dp))
    }
    if (open) {
        ColorPickerSheet(
            title = label,
            argb = argb,
            onChange = {
                argb = it
                DockPrefs.setColor(context, setting, it)
                onChange(it)
            },
            onDismiss = { open = false },
        )
    }
}

/** Drawn over a mid-grey so a translucent colour can be judged. */
@Composable
internal fun ColorSwatch(argb: Int, modifier: Modifier) {
    Box(modifier.background(Color.Gray)) {
        Box(Modifier.fillMaxSize().background(Color(argb)))
    }
}

/** The bottom sheet shared by every colour setting: opacity, red, green and blue sliders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerSheet(title: Int, argb: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            ColorSwatch(argb, Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp))
            listOf(
                R.string.color_alpha to 24,
                R.string.color_red to 16,
                R.string.color_green to 8,
                R.string.color_blue to 0,
            ).forEach { (name, shift) ->
                val value = (argb ushr shift) and 0xFF
                Text(stringResource(name, value), modifier = Modifier.padding(top = 8.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onChange((argb and (0xFF shl shift).inv()) or (it.toInt() shl shift)) },
                    valueRange = 0f..255f,
                )
            }
        }
    }
}
