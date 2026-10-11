package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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

/** One page for [step]; [onStepDone] is called when an optional step is finished or skipped. */
@Composable
internal fun OnboardingFlow(step: OnboardingStep, onStepDone: () -> Unit) {
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
        OnboardingStep.FanLayout -> FanLayoutScreen(null, footer)
        OnboardingStep.Trigger -> TriggerScreen(null, footer)
        OnboardingStep.Blacklist -> BlacklistScreen(null, footer)
    }
}

/** The page of a required permission step: the step count, the explanation and the button. */
@Composable
private fun PermissionPage(step: OnboardingStep, title: Int, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .topAndSideInsets()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stepCount(step), style = MaterialTheme.typography.labelLarge)
        if (step == OnboardingStep.Accessibility) {
            Text(
                stringResource(R.string.onboarding_welcome),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
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

/** Pinned under an optional step's own page: the step count, and Skip beside Next. */
@Composable
private fun OptionalFooter(step: OnboardingStep, onStepDone: () -> Unit) {
    val last = step == OnboardingStep.Blacklist
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            stepCount(step) + " · " + stringResource(R.string.onboarding_optional),
            style = MaterialTheme.typography.labelLarge,
        )
        Button(onClick = onStepDone, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(if (last) R.string.onboarding_finish else R.string.onboarding_next))
        }
    }
}

@Composable
private fun stepCount(step: OnboardingStep) =
    stringResource(R.string.onboarding_step_count, step.ordinal + 1, OnboardingStep.entries.size)
