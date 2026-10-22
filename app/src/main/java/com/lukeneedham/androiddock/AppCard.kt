package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** A rounded panel that groups a set of controls. */
@Composable
internal fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.current.colors.card),
        content = content,
    )
}
