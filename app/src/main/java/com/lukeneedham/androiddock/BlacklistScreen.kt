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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The apps that never show in the sheet, on a page of their own. */
@Composable
internal fun BlacklistScreen(onBack: (() -> Unit)?, footer: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    SubPage(R.string.blacklist_title, onBack, footer) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.blacklist_description), style = MaterialTheme.typography.bodyMedium)
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

