package com.lukeneedham.androiddock

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable

/**
 * The colour for text and icons that do not name their own: set by a button for what it holds, so
 * its label follows the button's style.
 */
internal val LocalAppContentColor = compositionLocalOf { Color.Unspecified }

/**
 * Text in the app's typeface. Size and line height are set where the text is used; only the
 * typeface and the colours come from the theme.
 */
@Composable
internal fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 24.sp,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
) {
    val theme = AppTheme.current
    val resolved = color.takeOrElse { LocalAppContentColor.current.takeOrElse { theme.colors.text } }
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(
            color = resolved,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontFamily = theme.fontFamily,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        maxLines = maxLines,
        softWrap = softWrap,
    )
}
