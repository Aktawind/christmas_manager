package com.aktawind.monnoel.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = FirGreen,
    onPrimary = Ivory,
    primaryContainer = SageLight,
    onPrimaryContainer = FirGreenDark,
    secondary = Sage,
    onSecondary = Ink,
    secondaryContainer = SageLight,
    onSecondaryContainer = FirGreenDark,
    tertiary = Champagne,
    onTertiary = Ink,
    tertiaryContainer = ChampagneLight,
    onTertiaryContainer = Ink,
    error = Burgundy,
    onError = Ivory,
    errorContainer = BurgundyLight,
    onErrorContainer = Burgundy,
    background = Ivory,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Hazelnut,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F1E4),
    surfaceContainer = Cream,
    surfaceContainerHigh = Color(0xFFEDE3D2),
    surfaceContainerHighest = Color(0xFFE7DCC8),
    outline = Color(0xFFB9A58F),
    outlineVariant = Color(0xFFE2D5C0),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = PineLight,
    onPrimary = FirGreenDark,
    primaryContainer = FirGreen,
    onPrimaryContainer = Cream,
    secondary = Sage,
    onSecondary = NightGreen,
    secondaryContainer = SageNight,
    onSecondaryContainer = Cream,
    tertiary = ChampagneNight,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF5A4721),
    onTertiaryContainer = ChampagneLight,
    error = BurgundyNight,
    onError = Color(0xFF4A0E17),
    errorContainer = Color(0xFF5E1621),
    onErrorContainer = BurgundyLight,
    background = NightGreen,
    onBackground = Cream,
    surface = NightGreen,
    onSurface = Cream,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = HazelnutNight,
    surfaceContainerLowest = Color(0xFF0B1812),
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF263F31),
    outline = Color(0xFF6E8574),
    outlineVariant = Color(0xFF2E4636),
)

/** Couleurs « magiques » hors du schéma Material. */
@Immutable
data class MonNoelExtraColors(val gold: Color, val burgundy: Color, val secondaryText: Color)

val LocalMonNoelColors = staticCompositionLocalOf {
    MonNoelExtraColors(gold = Champagne, burgundy = Burgundy, secondaryText = Hazelnut)
}

@Composable
fun MonNoelTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (darkTheme) {
        MonNoelExtraColors(gold = ChampagneNight, burgundy = BurgundyNight, secondaryText = HazelnutNight)
    } else {
        MonNoelExtraColors(gold = Champagne, burgundy = Burgundy, secondaryText = Hazelnut)
    }
    CompositionLocalProvider(LocalMonNoelColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = MonNoelTypography,
            shapes = MonNoelShapes,
            content = content,
        )
    }
}
