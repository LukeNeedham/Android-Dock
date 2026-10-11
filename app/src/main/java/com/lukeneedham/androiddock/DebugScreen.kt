package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Tools for debugging, on a page of their own. Only reachable from debug builds. */
@Composable
internal fun DebugScreen(onBack: () -> Unit, navigate: (Route) -> Unit) {
    SubPage(R.string.debug_title, onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            FilledTonalButton(onClick = { navigate(Route.Log) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_view_log))
            }
            NavBarSpacer()
        }
    }
}
