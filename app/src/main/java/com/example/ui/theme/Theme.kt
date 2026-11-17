package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = CyanBright,
    onPrimary = OpticalBlack,
    primaryContainer = CyanDarkContainer,
    onPrimaryContainer = CyanIceLight,
    secondary = GreySilver,
    onSecondary = OpticalBlack,
    secondaryContainer = OpticalGreyHigh,
    onSecondaryContainer = OpticalWhite,
    tertiary = CyanVibrant,
    onTertiary = OpticalBlack,
    tertiaryContainer = CyanDarkContainer,
    onTertiaryContainer = CyanIceLight,
    background = OpticalBlack,
    onBackground = OpticalWhite,
    surface = OpticalDarkGrey,
    onSurface = OpticalWhite,
    surfaceVariant = OpticalGrey,
    onSurfaceVariant = OpticalLightGrey,
    surfaceContainerLowest = OpticalBlack,
    surfaceContainerLow = OpticalDarkGrey,
    surfaceContainer = OpticalGrey,
    surfaceContainerHigh = OpticalGreyHigh,
    outline = OpticalOutlineGreyDark,
    outlineVariant = OpticalOutlineSubtleDark
)

private val StudioLightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = StudioWhite,
    primaryContainer = CyanIceLight,
    onPrimaryContainer = CyanInkDark,
    secondary = GreyCharcoal,
    onSecondary = StudioWhite,
    secondaryContainer = StudioLightGreyContainer,
    onSecondaryContainer = StudioBlack,
    tertiary = CyanVibrant,
    onTertiary = StudioBlack,
    tertiaryContainer = CyanIceLight,
    onTertiaryContainer = CyanInkDark,
    background = StudioLightGreyBg,
    onBackground = StudioBlack,
    surface = StudioWhite,
    onSurface = StudioBlack,
    surfaceVariant = StudioLightGreyContainer,
    onSurfaceVariant = StudioMediumGrey,
    surfaceContainerLowest = StudioWhite,
    surfaceContainerLow = StudioLightGreyBg,
    surfaceContainer = StudioLightGreyContainer,
    surfaceContainerHigh = StudioLightGreyHigh,
    outline = StudioOutlineGreyLight,
    outlineVariant = StudioOutlineSubtleLight
)

val ColorClueShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ColorClueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ColorClueTypography,
        shapes = ColorClueShapes,
        content = content
    )
}
