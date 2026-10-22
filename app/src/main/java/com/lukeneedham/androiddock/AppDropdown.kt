package com.lukeneedham.androiddock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * A button showing the [selected] option, with a drop arrow; pressing it lists every one of
 * [options] below it, as wide as the button. Choosing one calls [onSelect] and closes the list.
 */
@Composable
internal fun <T> AppDropdown(
    selected: T,
    options: List<T>,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.current.colors
    var open by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }
    Box(modifier = modifier.onSizeChanged { widthPx = it.width; heightPx = it.height }) {
        AppButton(
            style = AppButtonStyle.Secondary,
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppButtonLabel(labelOf(selected), modifier = Modifier.weight(1f))
            AppIcon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null)
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, heightPx),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { widthPx.toDp() })
                        .padding(top = 4.dp)
                        .shadow(3.dp, shape)
                        .clip(shape)
                        .background(colors.menu),
                ) {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) {
                                    onSelect(option)
                                    open = false
                                }
                                .padding(horizontal = 16.dp),
                        ) { AppText(labelOf(option)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDropdownPreview() = AppTheme {
    AppDropdown(selected = "Most recently used", options = listOf("Most recently used", "Most used in past week"), labelOf = { it }, onSelect = {})
}
