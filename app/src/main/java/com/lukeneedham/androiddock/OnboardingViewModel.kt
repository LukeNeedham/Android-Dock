package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** The [step] to show, or null when onboarding is over and the app can open, and the [corner] the fan opens in. */
internal data class OnboardingUiState(val step: OnboardingStep?, val corner: DockPrefs.Corner)

/**
 * Where the user is in the onboarding. The permission steps are checked against the system, so [refresh] runs when the
 * user comes back from the system settings.
 */
internal class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    private var introSeen = DockPrefs.getIntroSeen(application)
    private var finished = DockPrefs.getOnboardingFinished(application)

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
                DockPrefs.setIntroSeen(context)
            }
            OnboardingStep.OpenFan -> tryStep = OnboardingStep.CloseFan
            OnboardingStep.CloseFan -> tryStep = OnboardingStep.Done
            OnboardingStep.Done -> {
                finished = true
                DockPrefs.setOnboardingFinished(context)
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
        step = OnboardingStep.current(context, introSeen, finished, tryStep),
        corner = DockPrefs.getCorner(context),
    )
}
