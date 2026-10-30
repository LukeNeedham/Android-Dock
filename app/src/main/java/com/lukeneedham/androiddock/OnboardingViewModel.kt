package com.lukeneedham.androiddock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Where the user is in the onboarding. The permission steps are checked against the system, so [refresh] runs when the
 * user comes back from the system settings.
 */
internal class OnboardingViewModel(
    private val settings: DockSettings,
    private val setupStatus: SetupStatus,
) : ViewModel() {
    private var introSeen = settings.getIntroSeen()
    private var finished = settings.getOnboardingFinished()

    // How far through trying the fan the user is
    private var tryStep = OnboardingStep.OpenFan

    var uiState by mutableStateOf(compute())
        private set

    fun refresh() {
        uiState = compute()
    }

    /** The user moves on from the current step; the steps that wait for a permission only move on when it is granted. */
    fun onNext() {
        when (uiState.step) {
            OnboardingStep.Intro -> {
                introSeen = true
                settings.setIntroSeen()
            }
            OnboardingStep.OpenFan -> tryStep = OnboardingStep.CloseFan
            OnboardingStep.CloseFan -> tryStep = OnboardingStep.Done
            OnboardingStep.Done -> {
                finished = true
                settings.setOnboardingFinished()
            }
            else -> {}
        }
        refresh()
    }

    fun onSkipTrial() {
        tryStep = OnboardingStep.Done
        refresh()
    }

    private fun compute() = OnboardingUiState(
        step = OnboardingStep.current(
            introSeen = introSeen,
            finished = finished,
            accessibilityOk = setupStatus.isAccessibilityEnabled(),
            usageAccessOk = setupStatus.isUsageAccessGranted(),
            tryStep = tryStep,
        ),
        corner = settings.getCorner(),
    )
}
