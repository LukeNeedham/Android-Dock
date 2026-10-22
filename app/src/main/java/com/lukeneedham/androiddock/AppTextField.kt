package com.lukeneedham.androiddock

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth

/** A single-line text field with its [label] above it, so the label stays in view while typing. */
@Composable
internal fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val theme = AppTheme.current
    val colors = theme.colors
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        AppText(label, color = colors.fieldLabel, fontSize = 14.sp, lineHeight = 20.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp, lineHeight = 24.sp, fontFamily = theme.fontFamily),
            cursorBrush = SolidColor(colors.fieldFocusedBorder),
            modifier = Modifier
                .padding(top = 4.dp)
                .onFocusChanged { focused = it.isFocused }
                .fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .border(
                            if (focused) 2.dp else 1.dp,
                            if (focused) colors.fieldFocusedBorder else colors.fieldBorder,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp),
                ) { inner() }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTextFieldPreview() = AppTheme {
    AppTextField(value = "fan", onValueChange = {}, label = "Search")
}
