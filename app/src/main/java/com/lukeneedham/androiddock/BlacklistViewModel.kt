package com.lukeneedham.androiddock

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An app's name and icon, for the lists of apps. */
internal class AppEntry(val packageName: String, val label: String, val icon: ImageBitmap?)

private const val APP_ICON_PX = 96

/** Reads and saves the blacklist. Names and icons come from the package manager, off the main thread. */
internal class BlacklistViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    private var packages = DockPrefs.getBlacklist(application).sorted()

    var uiState by mutableStateOf(BlacklistUiState(hidden = emptyList()))
        private set

    init {
        reloadHidden()
    }

    fun onRemove(packageName: String) {
        packages = packages - packageName
        DockPrefs.setBlacklist(context, packages)
        reloadHidden()
    }

    fun onAdd(packageName: String) {
        packages = packages + packageName
        DockPrefs.setBlacklist(context, packages)
        reloadHidden()
    }

    /** The picker opens: load the apps it lists. */
    fun onPickerOpen() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadLaunchableApps() }
            uiState = uiState.copy(pickableApps = apps)
        }
    }

    private fun reloadHidden() {
        val shown = packages
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { shown.map { loadAppEntry(it) } }
            // A newer change wins over a load that was started before it
            if (shown == packages) uiState = uiState.copy(hidden = entries)
        }
    }

    private fun loadAppEntry(packageName: String): AppEntry {
        val packageManager = context.packageManager
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            AppEntry(
                packageName,
                packageManager.getApplicationLabel(info).toString(),
                packageManager.getApplicationIcon(info).toBitmap(APP_ICON_PX, APP_ICON_PX).asImageBitmap(),
            )
        } catch (e: Exception) {
            AppEntry(packageName, packageName, null)
        }
    }

    /** Every app with a launcher icon, by name. Slow: run it off the main thread. */
    private fun loadLaunchableApps(): List<AppEntry> =
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .map { loadAppEntry(it) }
            .sortedBy { it.label.lowercase() }
}
