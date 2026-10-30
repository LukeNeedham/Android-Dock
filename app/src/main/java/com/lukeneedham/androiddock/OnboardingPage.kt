package com.lukeneedham.androiddock

import androidx.compose.material3.Text
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** The onboarding: gets [OnboardingViewModel], opens the system pages the steps point to, and waits for the fan to open and close. */
@Composable
internal fun OnboardingPage() {
    val context = LocalContext.current
    val viewModel = koinViewModel<OnboardingViewModel>()
    val state = viewModel.uiState
    val step = state.step ?: return
    // The fan opens over this app, so the trial steps poll for it.
    LaunchedEffect(step) {
        if (step == OnboardingStep.OpenFan || step == OnboardingStep.CloseFan) {
            val open = step == OnboardingStep.OpenFan
            while (DockActivity.isOpen != open) delay(100)
            viewModel.onNext()
        }
    }
    OnboardingPageContent(
        step = step,
        corner = state.corner,
        onNext = viewModel::onNext,
        onSkipTrial = viewModel::onSkipTrial,
        onOpenAccessibilitySettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        onOpenUsageAccessSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        onOpenAppInfo = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
    )
}
