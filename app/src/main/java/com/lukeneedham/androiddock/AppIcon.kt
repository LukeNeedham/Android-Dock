package com.lukeneedham.androiddock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

/** An icon drawn in [tint], or in the colour of what holds it (a button's label colour) when none is given. */
@Composable
internal fun AppIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolved = tint.takeOrElse { LocalAppContentColor.current.takeOrElse { AppTheme.current.colors.text } }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = ColorFilter.tint(resolved),
    )
}
