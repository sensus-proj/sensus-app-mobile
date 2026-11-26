package com.example.sensus.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.sensus.R

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun interStyle(size: TextUnit, height: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = Inter,
    fontSize = size,
    lineHeight = height,
    fontWeight = weight
)

private val Display = interStyle(34.sp, 42.sp, FontWeight.Bold)
private val Title = interStyle(24.sp, 32.sp, FontWeight.Bold)
private val Subtitle = interStyle(20.sp, 28.sp, FontWeight.SemiBold)
private val Body = interStyle(15.sp, 22.sp, FontWeight.Normal)
private val Button = interStyle(16.sp, 24.sp, FontWeight.SemiBold)
private val Detail = interStyle(13.sp, 18.sp, FontWeight.Medium)
private val Minimum = interStyle(11.sp, 16.sp, FontWeight.Medium)

val Typography = Typography(
    displayLarge = Display,
    displayMedium = Display,
    displaySmall = Display,
    headlineLarge = Title,
    headlineMedium = Title,
    headlineSmall = Title,
    titleLarge = Title,
    titleMedium = Subtitle,
    titleSmall = Subtitle,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Detail,
    labelLarge = Button,
    labelMedium = Detail,
    labelSmall = Minimum
)
