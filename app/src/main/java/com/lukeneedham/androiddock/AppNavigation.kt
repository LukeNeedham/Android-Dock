package com.lukeneedham.androiddock

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

/** Owns the back stack and the [NavDisplay]. Called only by [Root]. */
@Composable
internal fun AppNavigation(resumes: Int) {
    val backStack = rememberNavBackStack(Route.Settings)

    // Until setup is done the app is only the onboarding flow, one page per step.
    val onboarding = viewModel<OnboardingViewModel>()
    LaunchedEffect(resumes) { onboarding.refresh() }
    if (onboarding.uiState.step != null) {
        OnboardingPage()
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        // Slide, never fade: a new page enters from the right, and going back reverses it.
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        entryProvider = { key -> routeEntryProvider(backStack, key, resumes) },
    )
}

private fun routeEntryProvider(backStack: NavBackStack<NavKey>, key: NavKey, resumes: Int): NavEntry<NavKey> {
    val route = key as Route
    return NavEntry(route) {
        Router(backStack, route, resumes)
    }
}
