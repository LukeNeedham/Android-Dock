package com.lukeneedham.androiddock

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The pages of the settings app, as routes: [Route.Settings] is the page of `SettingsPage`, and so on. */
@Serializable
internal sealed interface Route : NavKey {
    @Serializable
    data object Settings : Route

    @Serializable
    data object Trigger : Route

    @Serializable
    data object FanLayout : Route

    @Serializable
    data object FanBackground : Route

    @Serializable
    data object Blacklist : Route

    @Serializable
    data object Debug : Route

    @Serializable
    data object Logs : Route

    @Serializable
    data object Crashes : Route
}
