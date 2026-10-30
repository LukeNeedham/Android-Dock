package com.lukeneedham.androiddock

import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The onboarding steps, in order. The intro is shown once. The two permission steps are checked
 * against the system every time, so they come back if a permission is lost. After them the user
 * tries the fan once: [OpenFan] waits for it to open, [CloseFan] for it to close, and [Done] ends it.
 */
internal enum class OnboardingStep {
    Intro,
    Accessibility,
    UsageAccess,
    OpenFan,
    CloseFan,
    Done,
    ;

    companion object {
        /**
         * The step to show now, or null when onboarding is over and the app can open. [tryStep] is
         * how far through the fan trial the user is: the [OpenFan], [CloseFan] or [Done] step.
         */
        fun current(
            introSeen: Boolean,
            finished: Boolean,
            accessibilityOk: Boolean,
            usageAccessOk: Boolean,
            tryStep: OnboardingStep,
        ): OnboardingStep? = when {
            !introSeen -> Intro
            !accessibilityOk -> Accessibility
            !usageAccessOk -> UsageAccess
            finished -> null
            else -> tryStep
        }
    }
}

/**
 * The page for [step], sliding in from the right and the old one out to the left. [onNext] moves
 * on from the step.
 */
@Composable
internal fun OnboardingPageContent(
    step: OnboardingStep,
    corner: DockPrefs.Corner,
    onNext: () -> Unit,
    onSkipTrial: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width })
                .using(SizeTransform(clip = false))
        },
        label = "onboarding",
    ) { shown ->
        OnboardingStepContent(shown, corner, onNext, onSkipTrial, onOpenAccessibilitySettings, onOpenUsageAccessSettings, onOpenAppInfo)
    }
}

@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    corner: DockPrefs.Corner,
    onNext: () -> Unit,
    onSkipTrial: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    when (step) {
        OnboardingStep.Intro -> IntroPage(onNext)
        OnboardingStep.Accessibility -> StepPage(step, R.string.step_accessibility_title) {
            Text(stringResource(R.string.step_accessibility_description), fontSize = 16.sp, lineHeight = 24.sp, color = AppTheme.current.colors.text)
            AppButton(
                text = stringResource(R.string.step_accessibility_action),
                style = AppButtonStyle.Primary,
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            Text(
                stringResource(R.string.step_accessibility_hint),
                fontSize = 14.sp, lineHeight = 20.sp,
                color = AppTheme.current.colors.secondaryText,
                modifier = Modifier.padding(top = 24.dp),
            )
            AppButton(
                text = stringResource(R.string.step_app_info_action),
                style = AppButtonStyle.Text,
                onClick = onOpenAppInfo,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OnboardingStep.UsageAccess -> StepPage(step, R.string.step_usage_title) {
            Text(stringResource(R.string.step_usage_description), fontSize = 16.sp, lineHeight = 24.sp, color = AppTheme.current.colors.text)
            AppButton(
                text = stringResource(R.string.step_usage_action),
                style = AppButtonStyle.Primary,
                onClick = onOpenUsageAccessSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
        OnboardingStep.OpenFan -> {
            StepPage(step, R.string.step_open_title) {
                val cornerName = stringResource(corner.label).lowercase()
                Text(stringResource(R.string.step_open_description, cornerName), fontSize = 16.sp, lineHeight = 24.sp, color = AppTheme.current.colors.text)
                AppButton(text = stringResource(R.string.onboarding_skip), style = AppButtonStyle.Text, onClick = onSkipTrial, modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
            }
        }
        OnboardingStep.CloseFan -> {
            StepPage(step, R.string.step_close_title) {
                Text(stringResource(R.string.step_close_description), fontSize = 16.sp, lineHeight = 24.sp, color = AppTheme.current.colors.text)
            }
        }
        OnboardingStep.Done -> StepPage(step, R.string.step_done_title) {
            Text(stringResource(R.string.step_done_description), fontSize = 16.sp, lineHeight = 24.sp, color = AppTheme.current.colors.text)
            listOf(
                R.string.position_title,
                R.string.fan_layout_title,
                R.string.bg_title,
                R.string.blacklist_title,
            ).forEach { Text("•  " + stringResource(it), modifier = Modifier.padding(top = 8.dp, start = 8.dp), color = AppTheme.current.colors.text) }
            AppButton(text = stringResource(R.string.onboarding_finish), style = AppButtonStyle.Primary, onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
        }
    }
}

/** The banner on every onboarding page after the intro, so it is clear the user is setting the app up. */
@Composable
internal fun OnboardingHeader(step: OnboardingStep) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.current.colors.banner)
            .topAndSideInsets()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(R.string.onboarding_header),
            fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium,
            color = AppTheme.current.colors.bannerText,
        )
        Text(
            stepCount(step),
            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
            color = AppTheme.current.colors.bannerText,
        )
    }
}

/** The page of one step: the banner, the title and the step's own [content]. */
@Composable
private fun StepPage(step: OnboardingStep, title: Int, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader(step)
        Column(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                stringResource(title),
                fontSize = 24.sp, lineHeight = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = AppTheme.current.colors.text,
            )
            content()
            NavBarSpacer()
        }
    }
}

@Composable
private fun stepCount(step: OnboardingStep) =
    stringResource(R.string.onboarding_step_count, step.ordinal, OnboardingStep.entries.size - 1)

/** The first page: what Fan is and how it works. Room is left between the points for demo GIFs later. */
@Composable
private fun IntroPage(onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().topAndSideInsets()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(stringResource(R.string.intro_title), fontSize = 32.sp, lineHeight = 40.sp, color = AppTheme.current.colors.text)
            Text(
                stringResource(R.string.intro_summary),
                fontSize = 16.sp, lineHeight = 24.sp,
                modifier = Modifier.padding(top = 12.dp),
                color = AppTheme.current.colors.text,
            )
            IntroPoint(R.string.intro_point_tap_title, R.string.intro_point_tap_body)
            IntroPoint(R.string.intro_point_pick_title, R.string.intro_point_pick_body)
            IntroPoint(R.string.intro_point_hold_title, R.string.intro_point_hold_body)
        }
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                stringResource(R.string.intro_setup_note),
                fontSize = 14.sp, lineHeight = 20.sp,
                color = AppTheme.current.colors.secondaryText,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            AppButton(text = stringResource(R.string.intro_start), style = AppButtonStyle.Primary, onClick = onStart, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun IntroPoint(title: Int, body: Int) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        Text(stringResource(title), fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = AppTheme.current.colors.text)
        Text(
            stringResource(body),
            fontSize = 14.sp, lineHeight = 20.sp,
            color = AppTheme.current.colors.secondaryText,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
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
