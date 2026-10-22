package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The apps that never show in the sheet, on a page of their own. */
@Composable
internal fun BlacklistScreen(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null, header: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    SubPage(R.string.blacklist_title, onBack, footer, header) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            AppText(
                stringResource(R.string.blacklist_description),
                fontSize = 14.sp, lineHeight = 20.sp,
                color = AppTheme.current.colors.secondaryText,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BlacklistEditor(context)
            NavBarSpacer()
        }
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
        AppText(app.label, modifier = Modifier.weight(1f).padding(start = 12.dp))
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
        AppText(
            stringResource(R.string.blacklist_empty),
            fontSize = 14.sp, lineHeight = 20.sp,
            color = AppTheme.current.colors.secondaryText,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    entries.forEach { app ->
        AppRow(app) {
            AppButton(style = AppButtonStyle.Text, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                hidden.remove(app.packageName)
                save()
            }) { AppButtonLabel(stringResource(R.string.blacklist_remove)) }
        }
    }
    AppButton(
        style = AppButtonStyle.Secondary,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            picking = true
        },
        modifier = Modifier.padding(top = 16.dp),
    ) { AppButtonLabel(stringResource(R.string.blacklist_add)) }

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
@Composable
private fun AppPickerSheet(context: Context, exclude: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    AppBottomSheet(onDismiss) {
        AppText(
            stringResource(R.string.blacklist_pick_title),
            fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        var query by remember { mutableStateOf("") }
        AppTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.blacklist_search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val list = apps
        if (list == null) {
            AppProgress(modifier = Modifier.padding(24.dp))
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

