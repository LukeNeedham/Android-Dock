package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lukeneedham.common.ui.ProvideDebugSettings

/**
 * The one composable [MainActivity] calls: everything the UI sits in, then the navigation. Every app wraps its content in
 * [ProvideDebugSettings], even without a debug setting to read.
 *
 * @param resumes bumped by the activity on every resume, so the setup checks re-run.
 */
@Composable
internal fun Root(resumes: Int) {
    AppTheme {
        ProvideDebugSettings(theme = AppTheme.current.toDebugTheme()) {
            Box(modifier = Modifier.fillMaxSize().background(AppTheme.current.colors.background)) {
                AppNavigation(resumes)
            }
        }
    }
}
