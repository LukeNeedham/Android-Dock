package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The onboarding steps, in order. The intro comes first and is shown once. The next two are required and are checked against the system
 * every time, so they come back if a permission is lost. The rest are optional: the user moves
 * past each with Next or Skip, and that progress is remembered.
 */
internal enum class OnboardingStep(val required: Boolean) {
    Intro(false),
    Accessibility(true),
    UsageAccess(true),
    FanLayout(false),
    Trigger(false),
    Blacklist(false),
    ;

    companion object {
        /** The step to show now, or null when all setup is done and the app can open. */
        fun current(context: Context, introSeen: Boolean, optionalDone: Int): OnboardingStep? = when {
            !introSeen -> Intro
            !SetupState.isAccessibilityEnabled(context) -> Accessibility
            !SetupState.isUsageAccessGranted(context) -> UsageAccess
            optionalDone < DockPrefs.OPTIONAL_STEPS -> entries[FanLayout.ordinal + optionalDone]
            else -> null
        }
    }
}

/**
 * The page for [step], sliding in from the right (and the old one out to the left) when moving
 * forwards, and the other way round when moving back. [onStepDone] moves past an optional step;
 * [onStepBack] returns from one to the optional step before it.
 */
@Composable
internal fun OnboardingFlow(step: OnboardingStep, onStepDone: () -> Unit, onStepBack: () -> Unit) {
    val canGoBack = step.ordinal > OnboardingStep.FanLayout.ordinal
    BackHandler(enabled = canGoBack, onBack = onStepBack)
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forwards = targetState.ordinal > initialState.ordinal
            val direction = if (forwards) 1 else -1
            (slideInHorizontally { width -> width * direction } togetherWith
                slideOutHorizontally { width -> -width * direction })
                .using(SizeTransform(clip = false))
        },
        label = "onboarding",
    ) { page ->
        OnboardingPage(page, onStepDone, if (page.ordinal > OnboardingStep.FanLayout.ordinal) onStepBack else null)
    }
}

@Composable
private fun OnboardingPage(step: OnboardingStep, onStepDone: () -> Unit, onBack: (() -> Unit)?) {
    val header: @Composable () -> Unit = { OnboardingHeader(step) }
    val footer: @Composable () -> Unit = { OptionalFooter(step, onStepDone) }
    when (step) {
        OnboardingStep.Intro -> IntroPage(onStepDone)
        OnboardingStep.Accessibility -> PermissionPage(step, R.string.step_accessibility_title) {
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
        OnboardingStep.UsageAccess -> PermissionPage(step, R.string.step_usage_title) {
            val context = LocalContext.current
            Text(stringResource(R.string.step_usage_description), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.step_usage_action)) }
        }
        OnboardingStep.FanLayout -> FanLayoutScreen(onBack, footer, header)
        OnboardingStep.Trigger -> TriggerScreen(onBack, footer, header)
        OnboardingStep.Blacklist -> BlacklistScreen(onBack, footer, header)
    }
}

/** The banner on every onboarding page, so it is clear the user is setting the app up. */
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
            stepCount(step) + if (step.required) "" else " · " + stringResource(R.string.onboarding_optional),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** The page of a required permission step: the banner, the explanation and the button. */
@Composable
private fun PermissionPage(step: OnboardingStep, title: Int, content: @Composable () -> Unit) {
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

/** Pinned under an optional step's own page: the Next button (Finish on the last). */
@Composable
private fun OptionalFooter(step: OnboardingStep, onStepDone: () -> Unit) {
    val last = step == OnboardingStep.Blacklist
    Button(onClick = onStepDone, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(stringResource(if (last) R.string.onboarding_finish else R.string.onboarding_next))
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
