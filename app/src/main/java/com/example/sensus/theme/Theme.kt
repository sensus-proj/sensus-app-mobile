package com.example.sensus.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = SensusPurpleDarkMode,
    onPrimary = SensusDarkOnPrimary,
    primaryContainer = SensusPurpleDark,
    onPrimaryContainer = SensusDarkOnPrimaryContainer,
    secondary = SensusPurpleLight,
    onSecondary = SensusDarkOnPrimary,
    background = SensusDarkBackground,
    onBackground = SensusDarkTextPrimary,
    surface = SensusDarkSurface,
    onSurface = SensusDarkTextPrimary,
    surfaceVariant = SensusDarkSurfaceVariant,
    onSurfaceVariant = SensusDarkTextSecondary,
    outline = SensusDarkCardBorder,
    error = SensusRed,
    onError = SensusWhite
)

val LightColorScheme = lightColorScheme(
    primary = SensusPurple,
    onPrimary = SensusWhite,
    primaryContainer = SensusVioletLight,
    onPrimaryContainer = SensusPurpleDark,
    secondary = SensusPurpleLight,
    onSecondary = SensusWhite,
    background = SensusBackground,
    onBackground = SensusTextPrimary,
    surface = SensusSurface,
    onSurface = SensusTextPrimary,
    surfaceVariant = SensusVioletBg,
    onSurfaceVariant = SensusTextSecondary,
    outline = SensusCardBorder,
    error = SensusRed,
    onError = SensusWhite
)

@Composable
fun SensusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Standard Sensus OutlinedTextField colors guaranteeing visible dark text, crisp borders, and purple focus.
 */
@Composable
fun sensusOutlinedTextFieldColors(
    containerColor: Color = Color.Transparent,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorCursorColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)
