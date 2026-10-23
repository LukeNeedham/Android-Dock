package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * A pill-shaped button with a [text] label, and optionally an icon before it or after it. With a
 * [trailingIcon] the label fills the space before it, so the icon sits at the button's end.
 */
@Composable
internal fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Secondary,
    enabled: Boolean = true,
    leadingIcon: Int? = null,
    trailingIcon: Int? = null,
) {
    val colors = AppTheme.current.colors.button(style)
    val content = if (enabled) colors.content else colors.disabledContent
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
        if (leadingIcon != null) Icon(painterResource(leadingIcon), contentDescription = null, tint = content)
        Text(
            text,
            color = content,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = if (trailingIcon != null) Modifier.weight(1f) else Modifier,
        )
        if (trailingIcon != null) Icon(painterResource(trailingIcon), contentDescription = null, tint = content)
    }
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
        Icon(
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
                        AppButton(text = style.name, onClick = {}, style = style)
                        AppButton(text = "Disabled", onClick = {}, style = style, enabled = false)
                        AppIconButton(R.drawable.ic_add, contentDescription = "Add", onClick = {}, style = style)
                    }
                }
            }
        }
    }
}
