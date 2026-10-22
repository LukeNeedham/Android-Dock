package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Whether a footer already clears the navigation bar, so scrolling content needs no [NavBarSpacer]. */
private val LocalFooterShown = compositionLocalOf { false }

/** Insets for the top and sides only: the bottom is left to the content, so it scrolls under the nav bar. */
@Composable
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
 * as in onboarding) and [title], then [content], then [footer] pinned to the bottom. A [header] goes above the toolbar.
 */
@Composable
internal fun SubPage(
    title: Int,
    onBack: (() -> Unit)?,
    footer: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasFooter = footer != null
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
        header?.invoke()
        // The header, when there is one, already sits under the status bar.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(if (header == null) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .fillMaxWidth()
                .height(64.dp),
        ) {
            if (onBack != null) {
                AppIconButton(
                    iconRes = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                )
            }
            AppText(stringResource(title), fontSize = 22.sp, lineHeight = 28.sp)
        }
        CompositionLocalProvider(LocalFooterShown provides hasFooter) { content() }
        if (footer != null) Column(modifier = Modifier.navigationBarsPadding()) { footer() }
    }
}
