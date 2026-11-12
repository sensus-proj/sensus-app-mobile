package com.example.sensus.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// LightColorScheme enforces Sensus brand tokens and ensures high-contrast readability across all devices
private val LightColorScheme = lightColorScheme(
    primary = SensusPurple,
    onPrimary = SensusWhite,
    secondary = SensusPurpleLight,
    onSecondary = SensusPurpleDark,
    tertiary = SensusBeige,
    background = SensusBackground,
    surface = SensusSurface,
    onBackground = SensusTextPrimary,
    onSurface = SensusTextPrimary
)

@Composable
fun SensusTheme(content: @Composable () -> Unit) {
    // Sensus UI strictly implements a light design system with white cards and purple accents.
    MaterialTheme(
        colorScheme = LightColorScheme,
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
    focusedBorderColor: Color = SensusPurple,
    unfocusedBorderColor: Color = SensusCardBorder
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SensusTextPrimary,
    unfocusedTextColor = SensusTextPrimary,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = SensusPurple,
    unfocusedLabelColor = SensusTextSecondary,
    cursorColor = SensusPurple,
    errorTextColor = SensusTextPrimary,
    errorBorderColor = SensusRed,
    errorCursorColor = SensusRed,
    errorLabelColor = SensusRed,
    focusedPlaceholderColor = SensusTextMuted,
    unfocusedPlaceholderColor = SensusTextMuted,
    focusedLeadingIconColor = SensusPurple,
    unfocusedLeadingIconColor = SensusTextSecondary,
    focusedTrailingIconColor = SensusPurple,
    unfocusedTrailingIconColor = SensusTextSecondary
)
