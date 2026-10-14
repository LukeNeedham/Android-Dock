package com.lukeneedham.androiddock

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        fun current(context: Context, introSeen: Boolean, finished: Boolean, tryStep: OnboardingStep): OnboardingStep? = when {
            !introSeen -> Intro
            !SetupState.isAccessibilityEnabled(context) -> Accessibility
            !SetupState.isUsageAccessGranted(context) -> UsageAccess
            finished -> null
            else -> tryStep
        }
    }
}

/**
 * The page for [step], sliding in from the right and the old one out to the left. [onNext] moves
 * on from the step; the pages that wait for the fan to open or close call it themselves.
 */
@Composable
internal fun OnboardingFlow(step: OnboardingStep, onNext: () -> Unit, onSkipTrial: () -> Unit) {
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width })
                .using(SizeTransform(clip = false))
        },
        label = "onboarding",
    ) { page ->
        OnboardingPage(page, onNext, onSkipTrial)
    }
}

@Composable
private fun OnboardingPage(step: OnboardingStep, onNext: () -> Unit, onSkipTrial: () -> Unit) {
    when (step) {
        OnboardingStep.Intro -> IntroPage(onNext)
        OnboardingStep.Accessibility -> StepPage(step, R.string.step_accessibility_title) {
            val context = LocalContext.current
            Text(stringResource(R.string.step_accessibility_description), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.step_accessibility_action)) }
            Text(
                stringResource(R.string.step_accessibility_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.step_app_info_action)) }
        }
        OnboardingStep.UsageAccess -> StepPage(step, R.string.step_usage_title) {
            val context = LocalContext.current
            Text(stringResource(R.string.step_usage_description), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.step_usage_action)) }
        }
        OnboardingStep.OpenFan -> {
            WhenFan(open = true, onNext)
            StepPage(step, R.string.step_open_title) {
                val corner = stringResource(DockPrefs.getCorner(LocalContext.current).label).lowercase()
                Text(stringResource(R.string.step_open_description, corner), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onSkipTrial, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }
        OnboardingStep.CloseFan -> {
            WhenFan(open = false, onNext)
            StepPage(step, R.string.step_close_title) {
                Text(stringResource(R.string.step_close_description), style = MaterialTheme.typography.bodyLarge)
            }
        }
        OnboardingStep.Done -> StepPage(step, R.string.step_done_title) {
            Text(stringResource(R.string.step_done_description), style = MaterialTheme.typography.bodyLarge)
            listOf(
                R.string.position_title,
                R.string.fan_layout_title,
                R.string.bg_title,
                R.string.blacklist_title,
            ).forEach { Text("•  " + stringResource(it), modifier = Modifier.padding(top = 8.dp, start = 8.dp)) }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                Text(stringResource(R.string.onboarding_finish))
            }
        }
    }
}

/** Calls [onReached] once the fan is open ([open]) or closed ([open] false). The fan opens over this app, so it is polled. */
@Composable
private fun WhenFan(open: Boolean, onReached: () -> Unit) {
    val reached by rememberUpdatedState(onReached)
    LaunchedEffect(open) {
        while (DockActivity.isOpen != open) delay(100)
        reached()
    }
}

/** The banner on every onboarding page after the intro, so it is clear the user is setting the app up. */
@Composable
internal fun OnboardingHeader(step: OnboardingStep) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .topAndSideInsets()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(R.string.onboarding_header),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            stepCount(step),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
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
            Text(stringResource(R.string.intro_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.intro_summary),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            IntroPoint(R.string.intro_point_tap_title, R.string.intro_point_tap_body)
            IntroPoint(R.string.intro_point_pick_title, R.string.intro_point_pick_body)
            IntroPoint(R.string.intro_point_hold_title, R.string.intro_point_hold_body)
        }
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                stringResource(R.string.intro_setup_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.intro_start))
            }
        }
    }
}

@Composable
private fun IntroPoint(title: Int, body: Int) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
