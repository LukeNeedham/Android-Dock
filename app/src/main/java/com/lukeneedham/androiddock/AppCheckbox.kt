package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** A tick box, with a 48dp touch target round the 20dp box. */
@Composable
internal fun AppCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.current.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(shape)
                .background(if (checked) colors.checkboxChecked else Color.Transparent)
                .border(2.dp, if (checked) colors.checkboxChecked else colors.checkboxUnchecked, shape),
        ) {
            if (checked) {
                AppIcon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.checkboxCheckmark, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppCheckboxPreview() = AppTheme {
    Row {
        AppCheckbox(checked = true, onCheckedChange = {})
        AppCheckbox(checked = false, onCheckedChange = {})
    }
}
