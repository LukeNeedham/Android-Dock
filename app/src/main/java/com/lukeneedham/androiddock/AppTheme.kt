package com.lukeneedham.androiddock

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.lukeneedham.common.ui.DebugTheme

/** The colours of one button style; the same four apply to every button of that style. */
@Immutable
class AppButtonColors(
    val background: Color,
    val content: Color,
    val disabledBackground: Color,
    val disabledContent: Color,
)

/**
 * Every colour the app uses, named for where it is used. Nothing reads a generic slot such as
 * "primary" or "surface variant": a new use gets a new name here.
 */
@Immutable
class AppColors(
    /** The page behind everything, and the text on it. */
    val background: Color,
    val text: Color,
    /** Descriptions and hints. */
    val secondaryText: Color,
    /** Borders and dividers. */
    val outline: Color,

    val statusOk: Color,
    val statusError: Color,
    val warning: Color,

    /** The panels that hold the fan layout controls. */
    val card: Color,
    /** The onboarding banner, and its text. */
    val banner: Color,
    val bannerText: Color,
    val bottomSheet: Color,
    val menu: Color,
    /** Dims the page behind a bottom sheet. */
    val scrim: Color,

    val fieldBorder: Color,
    val fieldFocusedBorder: Color,
    val fieldLabel: Color,

    val sliderThumb: Color,
    val sliderActiveTrack: Color,
    val sliderInactiveTrack: Color,
    val sliderActiveTick: Color,
    val sliderInactiveTick: Color,

    val checkboxChecked: Color,
    val checkboxCheckmark: Color,
    val checkboxUnchecked: Color,

    /** Behind the fan in the layout preview and its thumbnail. */
    val previewBackground: Color,
    /** The handles and spinner that mark what is selected or loading. */
    val accent: Color,

    /** The main action on a page, and a selected choice. */
    val primaryButton: AppButtonColors,
    /** Everything else that can be pressed, and an unselected choice. */
    val secondaryButton: AppButtonColors,
    /** A quiet action with no fill. */
    val textButton: AppButtonColors,
) {
    fun button(style: AppButtonStyle): AppButtonColors = when (style) {
        AppButtonStyle.Primary -> primaryButton
        AppButtonStyle.Secondary -> secondaryButton
        AppButtonStyle.Text -> textButton
    }
}

/** The colours and typeface the whole UI is drawn with. Light and dark are decided in [AppTheme]. */
@Immutable
class AppThemeValues(
    val colors: AppColors,
    val fontFamily: FontFamily,
)

private val Ink = Color(0xFF1D1B20)
private val Paper = Color(0xFFE6E0E9)

private fun button(background: Color, content: Color, disabledContentAlpha: Float, disabledBackgroundAlpha: Float = 0.3f) =
    AppButtonColors(
        background = background,
        content = content,
        disabledBackground = background.copy(alpha = background.alpha * disabledBackgroundAlpha),
        disabledContent = content.copy(alpha = disabledContentAlpha),
    )

private val DarkColors = AppColors(
    background = Color(0xFF141218),
    text = Paper,
    secondaryText = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    statusOk = Color(0xFF81C784),
    statusError = Color(0xFFF2B8B5),
    warning = Color(0xFFFFB74D),
    card = Color(0xFF211F26),
    banner = Color(0xFF4A4458),
    bannerText = Color(0xFFE8DEF8),
    bottomSheet = Color(0xFF211F26),
    menu = Color(0xFF2B2930),
    scrim = Color.Black.copy(alpha = 0.32f),
    fieldBorder = Color(0xFF938F99),
    fieldFocusedBorder = Color.White,
    fieldLabel = Color(0xFFCAC4D0),
    sliderThumb = Color.White,
    sliderActiveTrack = Color.White,
    sliderInactiveTrack = Color.White.copy(alpha = 0.3f),
    sliderActiveTick = Color.Black.copy(alpha = 0.4f),
    sliderInactiveTick = Color.White.copy(alpha = 0.5f),
    checkboxChecked = Color.White,
    checkboxCheckmark = Color.Black,
    checkboxUnchecked = Color(0xFF938F99),
    previewBackground = Color(0xFF49454F),
    accent = Color.White,
    primaryButton = button(Color.White, Color.Black, disabledContentAlpha = 0.5f),
    secondaryButton = button(Color(0xFF49454F), Paper, disabledContentAlpha = 0.38f),
    textButton = button(Color.Transparent, Paper, disabledContentAlpha = 0.38f),
)

private val LightColors = AppColors(
    background = Color(0xFFFEF7FF),
    text = Ink,
    secondaryText = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    statusOk = Color(0xFF2E7D32),
    statusError = Color(0xFFB3261C),
    warning = Color(0xFF8D6E00),
    card = Color(0xFFF3EDF7),
    banner = Color(0xFFE8DEF8),
    bannerText = Color(0xFF1D192B),
    bottomSheet = Color(0xFFF3EDF7),
    menu = Color(0xFFF3EDF7),
    scrim = Color.Black.copy(alpha = 0.32f),
    fieldBorder = Color(0xFF79747E),
    fieldFocusedBorder = Ink,
    fieldLabel = Color(0xFF49454F),
    sliderThumb = Ink,
    sliderActiveTrack = Ink,
    sliderInactiveTrack = Ink.copy(alpha = 0.3f),
    sliderActiveTick = Color.White.copy(alpha = 0.4f),
    sliderInactiveTick = Ink.copy(alpha = 0.5f),
    checkboxChecked = Ink,
    checkboxCheckmark = Color.White,
    checkboxUnchecked = Color(0xFF79747E),
    previewBackground = Color(0xFFE7E0EC),
    accent = Ink,
    primaryButton = button(Ink, Color.White, disabledContentAlpha = 0.5f),
    secondaryButton = button(Color(0xFFE7E0EC), Ink, disabledContentAlpha = 0.38f),
    textButton = button(Color.Transparent, Ink, disabledContentAlpha = 0.38f),
)

val LocalAppTheme = staticCompositionLocalOf<AppThemeValues> { error("No AppTheme provided") }

/** Provides the app's colours and typeface, light or dark as the system is. Nothing in the app uses `MaterialTheme`. */
@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val values = AppThemeValues(
        colors = if (darkTheme) DarkColors else LightColors,
        fontFamily = FontFamily.Default,
    )
    CompositionLocalProvider(LocalAppTheme provides values, content = content)
}

object AppTheme {
    val current: AppThemeValues @Composable get() = LocalAppTheme.current
}

/** What the shared library's debug pages are drawn with, taken from the app's theme. */
fun AppThemeValues.toDebugTheme() = DebugTheme(
    background = colors.background,
    onBackground = colors.text,
    surface = colors.card,
    onSurface = colors.text,
    primary = colors.primaryButton.background,
    onPrimary = colors.primaryButton.content,
    outline = colors.outline,
    error = colors.statusError,
    warning = colors.warning,
    fontFamily = fontFamily,
)
