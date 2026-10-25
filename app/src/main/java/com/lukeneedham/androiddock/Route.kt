package com.lukeneedham.androiddock

import androidx.navigation3.runtime.NavKey

/** The pages of the settings app, as routes: [Route.Settings] is the page of `SettingsPage`, and so on. [name] is what is saved. */
internal sealed interface Route : NavKey {
    val name: String

    data object Settings : Route { override val name = "Settings" }
    data object Trigger : Route { override val name = "Trigger" }
    data object FanLayout : Route { override val name = "FanLayout" }
    data object FanBackground : Route { override val name = "FanBackground" }
    data object Blacklist : Route { override val name = "Blacklist" }
    data object Debug : Route { override val name = "Debug" }
    data object Logs : Route { override val name = "Logs" }
    data object Crashes : Route { override val name = "Crashes" }

    companion object {
        private val all: List<Route> by lazy { listOf(Settings, Trigger, FanLayout, FanBackground, Blacklist, Debug, Logs, Crashes) }

        fun of(name: String): Route = all.first { it.name == name }
    }
}
