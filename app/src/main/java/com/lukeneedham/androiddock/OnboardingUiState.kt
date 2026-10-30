package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The [step] to show, or null when onboarding is over and the app can open, and the [corner] the fan opens in. */
internal data class OnboardingUiState(val step: OnboardingStep?, val corner: DockPrefs.Corner)
