package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** A modal bottom sheet in the app's colours; the sheet itself is Material's, which is given every colour and shape. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.current.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bottomSheet,
        contentColor = colors.text,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 22.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.outline),
            )
        },
        content = content,
    )
}
