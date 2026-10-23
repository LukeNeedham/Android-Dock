package com.lukeneedham.androiddock

import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable

/** The slider colours of the app theme. Every slider passes these, so none falls back to Material's. */
@Composable
internal fun appSliderColors(): SliderColors {
    val colors = AppTheme.current.colors
    return SliderDefaults.colors(
        thumbColor = colors.sliderThumb,
        activeTrackColor = colors.sliderActiveTrack,
        inactiveTrackColor = colors.sliderInactiveTrack,
        activeTickColor = colors.sliderActiveTick,
        inactiveTickColor = colors.sliderInactiveTick,
    )
}
