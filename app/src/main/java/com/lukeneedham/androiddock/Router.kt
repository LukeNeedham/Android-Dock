package com.lukeneedham.androiddock

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lukeneedham.common.ui.CrashesPage
import com.lukeneedham.common.ui.DebugPage
import com.lukeneedham.common.ui.LogsPage

/** Calls the page of [route], with the navigation as lambdas. The only place that calls a page. */
@Composable
internal fun Router(backStack: NavBackStack<NavKey>, route: Route, resumes: Int) {
    // The common actions are defined once here, never repeated in each branch
    fun navigateTo(target: Route) { backStack.add(target) }
    fun onBack() { if (backStack.size > 1) backStack.removeLastOrNull() }

    when (route) { // exhaustive: no `else`
        Route.Settings -> SettingsPage(resumes) { navigateTo(it) }
        Route.Trigger -> TriggerPage(::onBack)
        Route.FanLayout -> FanLayoutPage(::onBack)
        Route.FanBackground -> FanBackgroundPage(::onBack)
        Route.Blacklist -> BlacklistPage(::onBack)
        Route.Debug -> DebugPage(
            onBack = ::onBack,
            onOpenLogs = { navigateTo(Route.Logs) },
            onOpenCrashes = { navigateTo(Route.Crashes) },
        )
        Route.Logs -> LogsPage(onBack = ::onBack)
        Route.Crashes -> CrashesPage(onBack = ::onBack)
    }
}
