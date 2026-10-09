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
        openCount++
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    SettingsScreen(resumes)
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

@Composable
private fun SettingsScreen(resumes: Int) {
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
                SheetState.want(true)
                context.startActivity(Intent(context, DockActivity::class.java))
            },
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

        SectionHeader(R.string.sheet_title_section, R.string.rows_description)
        var inner by remember { mutableIntStateOf(DockPrefs.getInnerOffset(context)) }
        Text(stringResource(R.string.slider_inner_offset, inner), modifier = Modifier.padding(top = 16.dp))
        Slider(
            value = inner.toFloat(),
            onValueChange = {
                inner = it.toInt()
                DockPrefs.setInnerOffset(context, inner)
            },
            valueRange = 0f..DockPrefs.INNER_MAX.toFloat(),
        )
        DockPrefs.Padding.entries.forEach { padding ->
            var value by remember { mutableIntStateOf(DockPrefs.getPadding(context, padding)) }
            val label = when (padding) {
                DockPrefs.Padding.BOTTOM -> R.string.slider_padding_bottom
                DockPrefs.Padding.SIDE -> R.string.slider_padding_side
            }
            Text(stringResource(label, value), modifier = Modifier.padding(top = 16.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    value = it.toInt()
                    DockPrefs.setPadding(context, padding, value)
                },
                valueRange = 0f..DockPrefs.PADDING_MAX.toFloat(),
            )
        }
        RowsEditor(context)

        SectionHeader(R.string.colors_title, null)
        ColorSettingRow(context, DockPrefs.ColorSetting.BUTTON, R.string.color_button)
        ColorSettingRow(context, DockPrefs.ColorSetting.SHEET, R.string.color_sheet_corner)
        ColorSettingRow(context, DockPrefs.ColorSetting.SHEET_EDGE, R.string.color_sheet_edge)

        SectionHeader(R.string.blacklist_title, R.string.blacklist_description)
        BlacklistEditor(context)

        TextButton(
            onClick = { context.startActivity(Intent(context, LogActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.onboarding_view_log)) }
    }
}

/** Edits the sheet's rows: add, delete, reorder, and each row's item count and icon size. */
@Composable
private fun RowsEditor(context: Context) {
    val rows = remember { mutableStateListOf<DockPrefs.Row>().apply { addAll(DockPrefs.getRows(context)) } }
    val haptic = LocalHapticFeedback.current
    var pickerRow by remember { mutableStateOf<Int?>(null) }
    val moveUp = stringResource(R.string.row_move_up)
    val moveDown = stringResource(R.string.row_move_down)
    val delete = stringResource(R.string.row_delete)
    fun save() = DockPrefs.setRows(context, rows.toList())
    fun move(from: Int, to: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        rows.add(to, rows.removeAt(from))
        save()
    }

    rows.forEachIndexed { index, row ->
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.row_title, index + 1),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { move(index, index - 1) }, enabled = index > 0) {
                        Text("↑", modifier = Modifier.semantics { contentDescription = moveUp })
                    }
                    IconButton(onClick = { move(index, index + 1) }, enabled = index < rows.lastIndex) {
                        Text("↓", modifier = Modifier.semantics { contentDescription = moveDown })
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rows.removeAt(index)
                            save()
                        },
                        enabled = rows.size > 1,
                    ) { Text("✕", modifier = Modifier.semantics { contentDescription = delete }) }
                }
                Text(stringResource(R.string.row_count, row.count))
                Slider(
                    value = row.count.toFloat(),
                    onValueChange = {
                        if (it.toInt() != row.count) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        rows[index] = row.copy(count = it.toInt())
                        save()
                    },
                    valueRange = DockPrefs.ROW_COUNT_MIN.toFloat()..DockPrefs.ROW_COUNT_MAX.toFloat(),
                    steps = DockPrefs.ROW_COUNT_MAX - DockPrefs.ROW_COUNT_MIN - 1,
                )
                Text(stringResource(R.string.row_width, row.widthDp))
                Slider(
                    value = row.widthDp.toFloat(),
                    onValueChange = {
                        rows[index] = row.copy(widthDp = it.toInt())
                        save()
                    },
                    valueRange = DockPrefs.ROW_WIDTH_MIN.toFloat()..DockPrefs.ROW_WIDTH_MAX.toFloat(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = row.showColor,
                        onCheckedChange = { show ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            // Switching on a clear colour would show nothing, so give it one.
                            val color = if (show && row.color ushr 24 == 0) DockPrefs.ROW_COLOR_DEFAULT else row.color
                            rows[index] = row.copy(showColor = show, color = color)
                            save()
                        },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pickerRow = index
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.row_color), modifier = Modifier.weight(1f))
                        ColorSwatch(row.color, Modifier.width(72.dp).height(32.dp))
                    }
                }
            }
        }
    }
    pickerRow?.let { index ->
        val row = rows.getOrNull(index)
        if (row == null) {
            pickerRow = null
        } else {
            ColorPickerSheet(
                title = R.string.row_color,
                argb = row.color,
                onChange = {
                    rows[index] = row.copy(color = it, showColor = true)
                    save()
                },
                onDismiss = { pickerRow = null },
            )
        }
    }
    if (rows.size < DockPrefs.ROWS_MAX) {
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                rows.add(DockPrefs.newRow())
                save()
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text(stringResource(R.string.row_add)) }
    }
}

/** An app's name and icon, for the lists of apps. */
private class AppEntry(val packageName: String, val label: String, val icon: ImageBitmap?)

private const val APP_ICON_PX = 96

private fun loadAppEntry(context: Context, packageName: String): AppEntry {
    val packages = context.packageManager
    return try {
        val info = packages.getApplicationInfo(packageName, 0)
        AppEntry(
            packageName,
            packages.getApplicationLabel(info).toString(),
            packages.getApplicationIcon(info).toBitmap(APP_ICON_PX, APP_ICON_PX).asImageBitmap(),
        )
    } catch (e: Exception) {
        AppEntry(packageName, packageName, null)
    }
}

/** Every app with a launcher icon, by name. Slow: run it off the main thread. */
private fun loadLaunchableApps(context: Context): List<AppEntry> =
    context.packageManager
        .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.activityInfo.packageName }
        .distinct()
        .map { loadAppEntry(context, it) }
        .sortedBy { it.label.lowercase() }

@Composable
private fun AppRow(app: AppEntry, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (app.icon != null) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Text(app.label, modifier = Modifier.weight(1f).padding(start = 12.dp))
        trailing()
    }
}

/** The blacklist: the apps that never show in the sheet, with a way to add and remove them. */
@Composable
private fun BlacklistEditor(context: Context) {
    val haptic = LocalHapticFeedback.current
    val hidden = remember { mutableStateListOf<String>().apply { addAll(DockPrefs.getBlacklist(context).sorted()) } }
    var picking by remember { mutableStateOf(false) }
    fun save() = DockPrefs.setBlacklist(context, hidden.toList())

    val entries by produceState(emptyList<AppEntry>(), hidden.toList()) {
        value = withContext(Dispatchers.IO) { hidden.toList().map { loadAppEntry(context, it) } }
    }
    if (hidden.isEmpty()) {
        Text(stringResource(R.string.blacklist_empty), modifier = Modifier.padding(top = 8.dp))
    }
    entries.forEach { app ->
        AppRow(app) {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                hidden.remove(app.packageName)
                save()
            }) { Text(stringResource(R.string.blacklist_remove)) }
        }
    }
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            picking = true
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text(stringResource(R.string.blacklist_add)) }

    if (picking) {
        AppPickerSheet(
            context = context,
            exclude = hidden.toSet(),
            onPick = {
                hidden.add(it)
                save()
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** A bottom sheet listing the installed apps, for choosing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(context: Context, exclude: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.blacklist_pick_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        var query by remember { mutableStateOf("") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.blacklist_search)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val list = apps
        if (list == null) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                items(
                    list.filter {
                        it.packageName !in exclude && it.label.contains(query.trim(), ignoreCase = true)
                    },
                    key = { it.packageName }) { app ->
                    AppRow(app, Modifier.clickable { onPick(app.packageName) })
                }
            }
        }
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

/** A colour setting shown as a swatch; tapping it opens [ColorPickerSheet]. */
@Composable
private fun ColorSettingRow(context: Context, setting: DockPrefs.ColorSetting, label: Int) {
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
            },
            onDismiss = { open = false },
        )
    }
}

/** Drawn over a mid-grey so a translucent colour can be judged. */
@Composable
private fun ColorSwatch(argb: Int, modifier: Modifier) {
    Box(modifier.background(Color.Gray)) {
        Box(Modifier.fillMaxSize().background(Color(argb)))
    }
}

/** The bottom sheet shared by every colour setting: opacity, red, green and blue sliders. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerSheet(title: Int, argb: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
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
