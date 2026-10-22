package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The few looks a button can have. Every button of a style has the same colours, from [AppColors]. */
enum class AppButtonStyle { Primary, Secondary, Text }

/** The smallest accessible touch target: every button is at least this tall. */
private val BUTTON_MIN_SIZE = 48.dp

/**
 * A pill-shaped button. What it holds is drawn in the style's content colour, so [AppText] and
 * [AppIcon] inside need no colour of their own.
 */
@Composable
internal fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Secondary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AppTheme.current.colors.button(style)
    Row(
        modifier = modifier
            .heightIn(min = BUTTON_MIN_SIZE)
            .clip(CircleShape)
            .background(if (enabled) colors.background else colors.disabledBackground)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalAppContentColor provides if (enabled) colors.content else colors.disabledContent) {
            content()
        }
    }
}

/** A text label for an [AppButton]: the size and weight every button label shares. */
@Composable
internal fun RowScope.AppButtonLabel(text: String, modifier: Modifier = Modifier) {
    AppText(text, modifier = modifier, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
}

/** A round button that holds only an icon, [BUTTON_MIN_SIZE] square. */
@Composable
internal fun AppIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Text,
    enabled: Boolean = true,
) {
    val colors = AppTheme.current.colors.button(style)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(BUTTON_MIN_SIZE)
            .clip(CircleShape)
            .background(if (enabled) colors.background else colors.disabledBackground)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        AppIcon(
            painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) colors.content else colors.disabledContent,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppButtonsPreview() = Row {
    listOf(true, false).forEach { dark ->
        AppTheme(darkTheme = dark) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(AppTheme.current.colors.background).padding(16.dp),
            ) {
                AppButtonStyle.entries.forEach { style ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppButton(onClick = {}, style = style) { AppButtonLabel(style.name) }
                        AppButton(onClick = {}, style = style, enabled = false) { AppButtonLabel("Disabled") }
                        AppIconButton(R.drawable.ic_add, contentDescription = "Add", onClick = {}, style = style)
                    }
                }
            }
        }
    }
}
