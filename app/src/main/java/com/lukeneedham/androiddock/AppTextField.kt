package com.lukeneedham.androiddock

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A single-line text field in the app's colours; the field is Material's, which is given every colour, the shape and the text style. */
@Composable
internal fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val theme = AppTheme.current
    val colors = theme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { AppText(label, color = colors.fieldLabel, fontSize = 14.sp, lineHeight = 20.sp) },
        shape = RoundedCornerShape(12.dp),
        textStyle = TextStyle(color = colors.text, fontSize = 16.sp, lineHeight = 24.sp, fontFamily = theme.fontFamily),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedBorderColor = colors.fieldFocusedBorder,
            unfocusedBorderColor = colors.fieldBorder,
            focusedLabelColor = colors.fieldLabel,
            unfocusedLabelColor = colors.fieldLabel,
            cursorColor = colors.fieldFocusedBorder,
        ),
        modifier = modifier,
    )
}
