package com.example.sensus.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = SensusWhite,
    primaryContainer = BrandVioletSoft,
    onPrimaryContainer = BrandVioletDark,
    secondary = BrandVioletDark,
    onSecondary = SensusWhite,
    secondaryContainer = BrandVioletSoft,
    onSecondaryContainer = BrandVioletDark,
    tertiary = BrandOrange,
    onTertiary = Ink,
    tertiaryContainer = BrandOrangeSoft,
    onTertiaryContainer = Ink,
    background = LightBackground,
    surface = SensusWhite,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = InkSecondary,
    surfaceVariant = BrandVioletSoft,
    surfaceContainerLowest = SensusWhite,
    surfaceContainerLow = LightBackground,
    surfaceContainer = SensusWhite,
    surfaceContainerHigh = BrandVioletSoft,
    surfaceContainerHighest = BrandVioletSoft,
    outline = InkSecondary,
    outlineVariant = LightBorder,
    error = StatusRed,
    onError = SensusWhite,
    errorContainer = StatusRedSoft,
    onErrorContainer = Ink,
    inverseSurface = Ink,
    inverseOnSurface = LightBackground,
    inversePrimary = BrandVioletSoft,
    surfaceTint = Color.Transparent
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandViolet,
    onPrimary = SensusWhite,
    primaryContainer = DarkElevated,
    onPrimaryContainer = BrandVioletSoft,
    secondary = BrandVioletSoft,
    onSecondary = Ink,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = BrandVioletSoft,
    tertiary = BrandOrange,
    onTertiary = Ink,
    tertiaryContainer = DarkElevated,
    onTertiaryContainer = BrandOrange,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = LightBackground,
    onSurface = LightBackground,
    onSurfaceVariant = DarkSecondary,
    surfaceVariant = DarkElevated,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkElevated,
    outline = DarkSecondary,
    outlineVariant = DarkBorder,
    error = StatusRedSoft,
    onError = Ink,
    errorContainer = DarkElevated,
    onErrorContainer = StatusRedSoft,
    inverseSurface = LightBackground,
    inverseOnSurface = Ink,
    inversePrimary = BrandVioletDark,
    surfaceTint = Color.Transparent
)

internal val SensusButtonShape = RoundedCornerShape(14.dp)

private val SensusShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = SensusButtonShape,
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun SensusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSensusDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = SensusShapes,
            content = content
        )
    }
}

@Composable
fun sensusOutlinedTextFieldColors(
    containerColor: Color = Color.Transparent,
    focusedBorderColor: Color = SensusVioletInk,
    unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SensusTextPrimary,
    unfocusedTextColor = SensusTextPrimary,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = SensusVioletInk,
    unfocusedLabelColor = SensusTextSecondary,
    cursorColor = SensusVioletInk,
    errorTextColor = SensusTextPrimary,
    errorBorderColor = SensusRed,
    errorCursorColor = SensusRed,
    errorLabelColor = SensusRed,
    focusedPlaceholderColor = SensusTextMuted,
    unfocusedPlaceholderColor = SensusTextMuted,
    focusedLeadingIconColor = SensusVioletInk,
    unfocusedLeadingIconColor = SensusTextSecondary,
    focusedTrailingIconColor = SensusVioletInk,
    unfocusedTrailingIconColor = SensusTextSecondary
)

@Composable
internal fun sensusTextButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(contentColor = SensusVioletInk)

@Composable
internal fun sensusOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = SensusVioletInk)
