package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.lukeneedham.common.ui.CrashesPage
import com.lukeneedham.common.ui.DebugPage
import com.lukeneedham.common.ui.LogsPage
import com.lukeneedham.common.ui.ProvideDebugSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The pages of the settings app. */
internal enum class Route { Settings, Trigger, FanLayout, Blacklist, Debug, Logs, Crashes, FanBackground }

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
        enableEdgeToEdge()
        openCount++
        setContent {
            AppTheme {
                // Every app wraps its content in ProvideDebugSettings, even without a debug setting to read.
                ProvideDebugSettings(theme = AppTheme.current.toDebugTheme()) {
                    Box(modifier = Modifier.fillMaxSize().background(AppTheme.current.colors.background)) {
                        AppNavigation(resumes)
                    }
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

    // Until setup is done the app is only the onboarding flow, one page per step.
    val context = LocalContext.current
    var introSeen by remember { mutableStateOf(DockPrefs.getIntroSeen(context)) }
    var finished by remember { mutableStateOf(DockPrefs.getOnboardingFinished(context)) }
    // How far through trying the fan the user is; the permission steps are re-checked on every resume.
    var tryStep by rememberSaveable { mutableStateOf(OnboardingStep.OpenFan) }
    val step = if (resumes >= 0) OnboardingStep.current(context, introSeen, finished, tryStep) else null
    if (step != null) {
        OnboardingFlow(
            step,
            onNext = {
                when (step) {
                    OnboardingStep.Intro -> {
                        introSeen = true
                        DockPrefs.setIntroSeen(context)
                    }
                    OnboardingStep.OpenFan -> tryStep = OnboardingStep.CloseFan
                    OnboardingStep.CloseFan -> tryStep = OnboardingStep.Done
                    OnboardingStep.Done -> {
                        finished = true
                        DockPrefs.setOnboardingFinished(context)
                    }
                    else -> {}
                }
            },
            onSkipTrial = { tryStep = OnboardingStep.Done },
        )
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        // Slide, never fade: a new page enters from the right, and going back reverses it.
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    Route.Settings -> SettingsScreen(resumes) { backStack.add(it) }
                    Route.Trigger -> TriggerScreen(back)
                    Route.FanLayout -> FanLayoutScreen(back)
                    Route.FanBackground -> FanBackgroundScreen(back)
                    Route.Blacklist -> BlacklistScreen(back)
                    Route.Debug -> DebugPage(
                        onBack = back,
                        onOpenLogs = { backStack.add(Route.Logs) },
                        onOpenCrashes = { backStack.add(Route.Crashes) },
                    )
                    Route.Logs -> LogsPage(onBack = back)
                    Route.Crashes -> CrashesPage(onBack = back)
                }
            }
        },
    )
}

@Composable
private fun SettingsScreen(resumes: Int, navigate: (Route) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // The fan opens over this page, so its state is polled; nothing else announces it.
    val fanOpen by produceState(DockActivity.isOpen) {
        while (true) {
            value = DockActivity.isOpen
            delay(150)
        }
    }

    Column(
        modifier = Modifier
            .topAndSideInsets()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        AppText(stringResource(R.string.settings_title), fontSize = 28.sp, lineHeight = 36.sp)
        PermissionStatus(
            title = R.string.status_accessibility_title,
            ok = SetupState.isAccessibilityEnabled(context),
            onOpen = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
        PermissionStatus(
            title = R.string.status_usage_title,
            ok = SetupState.isUsageAccessGranted(context),
            onOpen = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )
        AppButton(
            style = AppButtonStyle.Primary,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (fanOpen) {
                    DockActivity.close(haptic = false)
                } else {
                    DockActivity.altRequested = false
                    DockActivity.requestedTrigger = 0
                    DockActivity.starting = true
                    context.startActivity(Intent(context, DockActivity::class.java))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) { AppButtonLabel(stringResource(if (fanOpen) R.string.fan_close else R.string.fan_open)) }

        SectionHeader(R.string.position_title, R.string.trigger_summary)
        AppButton(
            style = AppButtonStyle.Secondary,
            onClick = { navigate(Route.Trigger) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { AppButtonLabel(stringResource(R.string.trigger_open)) }

        SectionHeader(R.string.sheet_title_section, R.string.fan_layout_summary)
        // Read again on every return here.
        val stops = if (resumes >= 0) DockPrefs.getSheetStops(context) else SheetGradient.DEFAULT
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    style = AppButtonStyle.Secondary,
                    onClick = { navigate(Route.FanLayout) },
                    modifier = Modifier.fillMaxWidth(),
                ) { AppButtonLabel(stringResource(R.string.fan_layout_open)) }
                AppButton(
                    style = AppButtonStyle.Secondary,
                    onClick = { navigate(Route.FanBackground) },
                    modifier = Modifier.fillMaxWidth(),
                ) { AppButtonLabel(stringResource(R.string.bg_edit)) }
            }
            // A small, read-only picture of the fan as it is now, background and layout together,
            // refreshed on every return here.
            FanPreview(
                inner = if (resumes >= 0) DockPrefs.getCurrentWidth(context) else 0,
                bottomPad = DockPrefs.getPadding(context, DockPrefs.Padding.BOTTOM),
                sidePad = DockPrefs.getPadding(context, DockPrefs.Padding.SIDE),
                rows = if (resumes >= 0) DockPrefs.getRows(context) else emptyList(),
                selected = null,
                showCurrent = DockPrefs.getShowCurrent(context),
                stops = stops,
                corner = if (resumes >= 0) DockPrefs.getCorner(context) else DockPrefs.Corner.BOTTOM_RIGHT,
                thumbnailDp = 112,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        SectionHeader(R.string.order_title, R.string.order_description)
        var sortType by remember { mutableStateOf(DockPrefs.getSortType(context)) }
        AppDropdown(
            selected = sortType,
            options = DockPrefs.SortType.entries,
            labelOf = { stringResource(it.label) },
            onSelect = {
                sortType = it
                DockPrefs.setSortType(context, it)
            },
            modifier = Modifier.padding(top = 8.dp),
        )

        SectionHeader(R.string.blacklist_title, R.string.blacklist_description)
        // Read again on every return here.
        val hiddenCount = if (resumes >= 0) DockPrefs.getBlacklist(context).size else 0
        AppButton(
            style = AppButtonStyle.Secondary,
            onClick = { navigate(Route.Blacklist) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { AppButtonLabel(stringResource(R.string.blacklist_open, hiddenCount)) }

        if (BuildConfig.DEBUG) {
            AppButton(
                style = AppButtonStyle.Secondary,
                onClick = { navigate(Route.Debug) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) { AppButtonLabel(stringResource(R.string.debug_open)) }
        }
        NavBarSpacer()
    }
}

/**
 * The four screen corners as a box with a labelled button in each of its corners. The selected
 * corner is highlighted and carries a tick.
 */
@Composable
internal fun CornerPicker(selected: DockPrefs.Corner, onSelect: (DockPrefs.Corner) -> Unit) {
    val colors = AppTheme.current.colors
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(6.dp),
    ) {
        // Each corner's whole quarter of the box is the click area; the button drawn in it stays small.
        listOf(
            listOf(DockPrefs.Corner.TOP_LEFT, DockPrefs.Corner.TOP_RIGHT),
            listOf(DockPrefs.Corner.BOTTOM_LEFT, DockPrefs.Corner.BOTTOM_RIGHT),
        ).forEach { line ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                line.forEach { corner ->
                    val isSelected = corner == selected
                    val shape = RoundedCornerShape(8.dp)
                    val selectedColors = colors.primaryButton
                    val content = if (isSelected) selectedColors.content else colors.text
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(corner) }),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(
                                    when (corner) {
                                        DockPrefs.Corner.BOTTOM_RIGHT -> Alignment.BottomEnd
                                        DockPrefs.Corner.BOTTOM_LEFT -> Alignment.BottomStart
                                        DockPrefs.Corner.TOP_LEFT -> Alignment.TopStart
                                        DockPrefs.Corner.TOP_RIGHT -> Alignment.TopEnd
                                    },
                                )
                                .clip(shape)
                                .background(if (isSelected) selectedColors.background else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            AppText(stringResource(corner.label), color = content)
                            if (isSelected) {
                                AppIcon(
                                    painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = content,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One check on the home page: its [title], then whether it is set up (a tick and "Completed", or a
 * cross and "Not completed"), with a link to its system page at the right edge.
 */
@Composable
private fun PermissionStatus(title: Int, ok: Boolean, onOpen: () -> Unit) {
    AppText(
        stringResource(title),
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AppIcon(
            painterResource(if (ok) R.drawable.ic_check else R.drawable.ic_close),
            contentDescription = null,
            tint = if (ok) AppTheme.current.colors.statusOk else AppTheme.current.colors.statusError,
        )
        AppText(
            stringResource(if (ok) R.string.status_completed else R.string.status_not_completed),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        AppButton(style = AppButtonStyle.Text, onClick = onOpen) { AppButtonLabel(stringResource(R.string.status_open)) }
    }
}

@Composable
internal fun SectionHeader(title: Int, description: Int?) {
    AppText(
        stringResource(title),
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp),
    )
    if (description != null) {
        AppText(
            stringResource(description),
            fontSize = 14.sp, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** A labelled slider that saves its value as it moves; the running service applies it live. */
@Composable
internal fun SettingSlider(context: Context, setting: DockPrefs.Setting, label: @Composable (Int) -> String, trigger: Int = 0) {
    var value by remember(trigger) { mutableIntStateOf(DockPrefs.get(context, setting, trigger)) }
    AppText(
        label(value),
        modifier = Modifier.padding(top = 16.dp),
    )
    AppSlider(
        value = value.toFloat(),
        onValueChange = {
            value = it.toInt()
            DockPrefs.set(context, setting, value, trigger)
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
    trigger: Int = 0,
    onChange: (Int) -> Unit = {},
) {
    var argb by remember(trigger) { mutableIntStateOf(DockPrefs.getColor(context, setting, trigger)) }
    var open by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(top = 16.dp),
    ) {
        AppText(stringResource(label), modifier = Modifier.weight(1f))
        ColorSwatch(argb, Modifier.width(72.dp).height(32.dp))
    }
    if (open) {
        ColorPickerSheet(
            title = label,
            argb = argb,
            onChange = {
                argb = it
                DockPrefs.setColor(context, setting, it, trigger)
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
@Composable
internal fun ColorPickerSheet(title: Int, argb: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            AppText(stringResource(title), fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
            ColorSwatch(argb, Modifier.padding(top = 8.dp).fillMaxWidth().height(40.dp))
            ColorSliders(argb, onChange)
        }
    }
}

/** Opacity, red, green and blue sliders for [argb]. */
@Composable
internal fun ColorSliders(argb: Int, onChange: (Int) -> Unit) {
    listOf(
        R.string.color_alpha to 24,
        R.string.color_red to 16,
        R.string.color_green to 8,
        R.string.color_blue to 0,
    ).forEach { (name, shift) ->
        val value = (argb ushr shift) and 0xFF
        AppText(stringResource(name, value), modifier = Modifier.padding(top = 8.dp))
        AppSlider(
            value = value.toFloat(),
            onValueChange = { onChange((argb and (0xFF shl shift).inv()) or (it.toInt() shl shift)) },
            valueRange = 0f..255f,
        )
    }
}
