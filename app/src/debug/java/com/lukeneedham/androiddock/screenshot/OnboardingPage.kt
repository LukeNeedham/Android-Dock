package com.lukeneedham.androiddock.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lukeneedham.androiddock.AppTheme
import com.lukeneedham.androiddock.DockPrefs
import com.lukeneedham.androiddock.OnboardingPageContent
import com.lukeneedham.androiddock.OnboardingStep

/** Rendered by CI to docs/screenshots/<position in order.txt>-OnboardingPage.png: the intro, the first page of the setup. */
@Preview
@Composable
private fun OnboardingPageScreenshot() {
    AppTheme(darkTheme = false) {
        OnboardingPageContent(
            step = OnboardingStep.Intro,
            corner = DockPrefs.Corner.BOTTOM_RIGHT,
            onNext = {},
            onSkipTrial = {},
            onOpenAccessibilitySettings = {},
            onOpenUsageAccessSettings = {},
            onOpenAppInfo = {},
        )
    }
}
