package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads and saves the blacklist. Names and icons come from the package manager, off the main thread. */
internal class BlacklistViewModel(
    private val settings: DockSettings,
    private val installedApps: InstalledApps,
) : ViewModel() {
    private var packages = settings.getBlacklist().sorted()

    var uiState by mutableStateOf(BlacklistUiState(hidden = emptyList()))
        private set

    init {
        reloadHidden()
    }

    fun onRemove(packageName: String) {
        packages = packages - packageName
        settings.setBlacklist(packages)
        reloadHidden()
    }

    fun onAdd(packageName: String) {
        packages = packages + packageName
        settings.setBlacklist(packages)
        reloadHidden()
    }

    /** The picker opens: load the apps it lists. */
    fun onPickerOpen() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { installedApps.loadLaunchableApps() }
            uiState = uiState.copy(pickableApps = apps)
        }
    }

    private fun reloadHidden() {
        val shown = packages
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { shown.map { installedApps.loadAppEntry(it) } }
            // A newer change wins over a load that was started before it
            if (shown == packages) uiState = uiState.copy(hidden = entries)
        }
    }
}
