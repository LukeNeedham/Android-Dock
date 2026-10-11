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
 * The onboarding steps, in order. The first two are required and are checked against the system
 * every time, so they come back if a permission is lost. The rest are optional: the user moves
 * past each with Next or Skip, and that progress is remembered.
 */
internal enum class OnboardingStep(val required: Boolean) {
    Accessibility(true),
    UsageAccess(true),
    FanLayout(false),
    Trigger(false),
    Blacklist(false),
    ;

    companion object {
        /** The step to show now, or null when all setup is done and the app can open. */
        fun current(context: Context, optionalDone: Int): OnboardingStep? = when {
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
        OnboardingStep.Accessibility -> PermissionPage(step, R.string.step_accessibility_title) {
            val context = LocalContext.current
            Text(stringResource(R.string.step_accessibility_description), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.step_accessibility_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text(stringResource(R.string.step_accessibility_action)) }
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
            if (step == OnboardingStep.Accessibility) {
                Text(stringResource(R.string.onboarding_welcome), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
            )
            content()
            Text(
                stringResource(R.string.onboarding_required_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 24.dp),
            )
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
    stringResource(R.string.onboarding_step_count, step.ordinal + 1, OnboardingStep.entries.size)
