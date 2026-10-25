package com.lukeneedham.androiddock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

/**
 * The page that edits the background gradient: the gradient in a box with a handle for each stop
 * to drag along the diagonal, then the selected stop's colour, and buttons to add and remove
 * stops. One stop makes the background a single flat colour. Every change is saved as it is made.
 */
@Composable
internal fun FanBackgroundPageContent(
    state: FanBackgroundUiState,
    onBack: (() -> Unit)?,
    onStopsChange: (List<ColorStop>) -> Unit,
) {
    val stops = state.stops
    val corner = state.corner
    val onChange = onStopsChange
    var selected by remember { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, stops.lastIndex)
    val stop = stops[index]

    SubPage(R.string.bg_title, onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp),
        ) {
            GradientBox(
                stops, corner,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                selected = index,
                onSelect = { selected = it },
                onChange = onChange,
            )
            Text(
                stringResource(R.string.stop_title, index + 1, stops.size, (stop.position * 100).roundToInt()),
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp),
                color = AppTheme.current.colors.text,
            )
            ColorSliders(stop.color) { color ->
                onChange(stops.mapIndexed { i, s -> if (i == index) s.copy(color = color) else s })
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                AppButton(
                    text = stringResource(R.string.stop_add),
                    style = AppButtonStyle.Secondary,
                    onClick = {
                        val (added, at) = SheetGradient.added(stops)
                        selected = at
                        onChange(added)
                    },
                    enabled = stops.size < SheetGradient.STOPS_MAX,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = stringResource(R.string.stop_remove),
                    style = AppButtonStyle.Secondary,
                    onClick = {
                        selected = index.coerceAtMost(stops.size - 2).coerceAtLeast(0)
                        onChange(SheetGradient.removed(stops, index))
                    },
                    enabled = stops.size > 1,
                    modifier = Modifier.weight(1f),
                )
            }
            NavBarSpacer()
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        FanBackgroundPageContent(
            state = FanBackgroundUiState(
                stops = listOf(ColorStop(0f, 0xFF1565C0.toInt()), ColorStop(1f, 0x00000000)),
                corner = DockPrefs.Corner.BOTTOM_RIGHT,
            ),
            onBack = {},
            onStopsChange = {},
        )
    }
}
