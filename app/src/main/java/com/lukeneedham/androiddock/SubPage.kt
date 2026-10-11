package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/** Whether a footer already clears the navigation bar, so scrolling content needs no [NavBarSpacer]. */
private val LocalFooterShown = compositionLocalOf { false }

/** Insets for the top and sides only: the bottom is left to the content, so it scrolls under the nav bar. */
internal fun Modifier.topAndSideInsets(): Modifier = windowInsetsPadding(
    WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)

/** The last item of scrolling content: lets it scroll clear of the navigation bar. */
@Composable
internal fun NavBarSpacer() {
    if (!LocalFooterShown.current) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

/**
 * The frame of every settings sub page: a toolbar with a back button (none if [onBack] is null,
 * as in onboarding) and [title], then [content], then [footer] pinned to the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubPage(
    title: Int,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasFooter = footer != null
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
        TopAppBar(
            title = { Text(stringResource(title)) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                }
            },
        )
        CompositionLocalProvider(LocalFooterShown provides hasFooter) { content() }
        if (footer != null) Column(modifier = Modifier.navigationBarsPadding()) { footer() }
    }
}
