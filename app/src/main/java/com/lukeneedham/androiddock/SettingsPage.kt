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

/** The home page of the settings app: it gets [SettingsViewModel], opens the system pages and the fan, and only calls [SettingsPageContent]. */
@Composable
internal fun SettingsPage(resumes: Int, navigate: (Route) -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    // Read again on every return to this page
    LaunchedEffect(resumes) { viewModel.refresh() }
    // The fan opens over this page, so its state is polled; nothing else announces it.
    val fanOpen by produceState(DockActivity.isOpen) {
        while (true) {
            value = DockActivity.isOpen
            delay(150)
        }
    }
    val haptic = LocalHapticFeedback.current
    SettingsPageContent(
        state = viewModel.uiState,
        fanOpen = fanOpen,
        onOpenAccessibilitySettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        onOpenUsageAccessSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        onToggleFan = {
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
        onSortTypeSelect = viewModel::onSortTypeSelect,
        onNavigate = navigate,
    )
}
