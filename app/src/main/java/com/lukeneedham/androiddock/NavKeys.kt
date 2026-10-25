package com.lukeneedham.androiddock

import androidx.navigation3.runtime.NavKey

/** The pages of the settings app, as navigation keys: [SettingsNavKey] is the page of `SettingsPage`, and so on. [name] is what is saved. */
internal sealed class AppNavKey(val name: String) : NavKey {
    companion object {
        private val all: List<AppNavKey> by lazy {
            listOf(SettingsNavKey, TriggerNavKey, FanLayoutNavKey, FanBackgroundNavKey, BlacklistNavKey, DebugNavKey, LogsNavKey, CrashesNavKey)
        }

        fun of(name: String): AppNavKey = all.first { it.name == name }
    }
}

internal data object SettingsNavKey : AppNavKey("Settings")
internal data object TriggerNavKey : AppNavKey("Trigger")
internal data object FanLayoutNavKey : AppNavKey("FanLayout")
internal data object FanBackgroundNavKey : AppNavKey("FanBackground")
internal data object BlacklistNavKey : AppNavKey("Blacklist")
internal data object DebugNavKey : AppNavKey("Debug")
internal data object LogsNavKey : AppNavKey("Logs")
internal data object CrashesNavKey : AppNavKey("Crashes")
