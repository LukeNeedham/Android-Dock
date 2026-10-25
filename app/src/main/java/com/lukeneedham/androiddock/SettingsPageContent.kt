package com.lukeneedham.androiddock

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

/** All the home page's UI: the permission checks, the fan, and a way into each settings page. */
@Composable
internal fun SettingsPageContent(
    state: SettingsUiState,
    fanOpen: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onToggleFan: () -> Unit,
    onSortTypeSelect: (DockPrefs.SortType) -> Unit,
    onNavigate: (Route) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .topAndSideInsets()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.settings_title), fontSize = 28.sp, lineHeight = 36.sp, color = AppTheme.current.colors.text)
        PermissionStatus(
            title = R.string.status_accessibility_title,
            ok = state.accessibilityOk,
            onOpen = onOpenAccessibilitySettings,
        )
        PermissionStatus(
            title = R.string.status_usage_title,
            ok = state.usageAccessOk,
            onOpen = onOpenUsageAccessSettings,
        )
        AppButton(
            text = stringResource(if (fanOpen) R.string.fan_close else R.string.fan_open),
            style = AppButtonStyle.Primary,
            onClick = onToggleFan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        )

        SectionHeader(R.string.position_title, R.string.trigger_summary)
        AppButton(
            text = stringResource(R.string.trigger_open),
            style = AppButtonStyle.Secondary,
            onClick = { onNavigate(Route.Trigger) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        SectionHeader(R.string.sheet_title_section, R.string.fan_layout_summary)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = stringResource(R.string.fan_layout_open),
                    style = AppButtonStyle.Secondary,
                    onClick = { onNavigate(Route.FanLayout) },
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    text = stringResource(R.string.bg_edit),
                    style = AppButtonStyle.Secondary,
                    onClick = { onNavigate(Route.FanBackground) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // A small, read-only picture of the fan as it is now, background and layout together,
            // refreshed on every return here.
            FanPreview(
                inner = state.fan.inner,
                bottomPad = state.fan.bottomPad,
                sidePad = state.fan.sidePad,
                rows = state.fan.rows,
                selected = null,
                showCurrent = state.fan.showCurrent,
                stops = state.fan.stops,
                corner = state.fan.corner,
                thumbnailDp = 112,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        SectionHeader(R.string.order_title, R.string.order_description)
        var sortMenuOpen by remember { mutableStateOf(false) }
        Box(modifier = Modifier.padding(top = 8.dp)) {
            AppButton(
                text = stringResource(state.sortType.label),
                style = AppButtonStyle.Secondary,
                onClick = { sortMenuOpen = true },
                trailingIcon = R.drawable.ic_arrow_drop_down,
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = AppTheme.current.colors.menu,
                tonalElevation = 0.dp,
            ) {
                DockPrefs.SortType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label), color = AppTheme.current.colors.text) },
                        onClick = {
                            onSortTypeSelect(option)
                            sortMenuOpen = false
                        },
                    )
                }
            }
        }

        SectionHeader(R.string.blacklist_title, R.string.blacklist_description)
        AppButton(
            text = stringResource(R.string.blacklist_open, state.hiddenCount),
            style = AppButtonStyle.Secondary,
            onClick = { onNavigate(Route.Blacklist) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (BuildConfig.DEBUG) {
            AppButton(
                text = stringResource(R.string.debug_open),
                style = AppButtonStyle.Secondary,
                onClick = { onNavigate(Route.Debug) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )
        }
        NavBarSpacer()
    }
}

/**
 * One check on the home page: its [title], then whether it is set up (a tick and "Completed", or a
 * cross and "Not completed"), with a link to its system page at the right edge.
 */
@Composable
private fun PermissionStatus(title: Int, ok: Boolean, onOpen: () -> Unit) {
    Text(
        stringResource(title),
        fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp),
        color = AppTheme.current.colors.text,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            painterResource(if (ok) R.drawable.ic_check else R.drawable.ic_close),
            contentDescription = null,
            tint = if (ok) AppTheme.current.colors.statusOk else AppTheme.current.colors.statusError,
        )
        Text(
            stringResource(if (ok) R.string.status_completed else R.string.status_not_completed),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color = AppTheme.current.colors.text,
        )
        AppButton(text = stringResource(R.string.status_open), style = AppButtonStyle.Text, onClick = onOpen)
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SettingsPageContent(
            state = SettingsUiState(
                accessibilityOk = true,
                usageAccessOk = false,
                fan = FanPreviewState(100, 24, 8, DockPrefs.DEFAULT_ROWS, true, SheetGradient.DEFAULT, DockPrefs.Corner.BOTTOM_RIGHT),
                sortType = DockPrefs.SortType.entries.first(),
                hiddenCount = 2,
            ),
            fanOpen = false,
            onOpenAccessibilitySettings = {},
            onOpenUsageAccessSettings = {},
            onToggleFan = {},
            onSortTypeSelect = {},
            onNavigate = {},
        )
    }
}
