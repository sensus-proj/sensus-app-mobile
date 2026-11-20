package com.example.sensus.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalSensusDark = staticCompositionLocalOf { false }

private const val BRAND_VIOLET_HEX = 0xFF7C3AED
private const val BRAND_VIOLET_DARK_HEX = 0xFF6D28D9
private const val BRAND_VIOLET_SOFT_HEX = 0xFFF2EBFE
private const val BRAND_ORANGE_HEX = 0xFFF97316
private const val BRAND_ORANGE_DARK_HEX = 0xFFEA580C
private const val BRAND_ORANGE_SOFT_HEX = 0xFFFFF1E6
private const val STATUS_GREEN_HEX = 0xFF238561
private const val STATUS_GREEN_SOFT_HEX = 0xFFEAF6EF
private const val STATUS_RED_HEX = 0xFFDC2626
private const val STATUS_RED_SOFT_HEX = 0xFFFEE2E2
private const val INK_HEX = 0xFF211A2E
private const val INK_SECONDARY_HEX = 0xFF716B7C
private const val INK_MUTED_HEX = 0xFF9A94A8
private const val DARK_MUTED_HEX = 0xFF7D7590
private const val LIGHT_BACKGROUND_HEX = 0xFFF9FAFB
private const val LIGHT_BORDER_HEX = 0xFFEFE9FA
private const val DARK_BACKGROUND_HEX = 0xFF171320
private const val DARK_SURFACE_HEX = 0xFF241D30
private const val DARK_ELEVATED_HEX = 0xFF302342
private const val DARK_SECONDARY_HEX = 0xFFB6AFC2
private const val DARK_BORDER_HEX = 0xFF40364D

internal val BrandViolet = Color(BRAND_VIOLET_HEX)
internal val BrandVioletDark = Color(BRAND_VIOLET_DARK_HEX)
internal val BrandVioletSoft = Color(BRAND_VIOLET_SOFT_HEX)
internal val BrandOrange = Color(BRAND_ORANGE_HEX)
internal val BrandOrangeDark = Color(BRAND_ORANGE_DARK_HEX)
internal val BrandOrangeSoft = Color(BRAND_ORANGE_SOFT_HEX)
internal val StatusGreen = Color(STATUS_GREEN_HEX)
internal val StatusGreenSoft = Color(STATUS_GREEN_SOFT_HEX)
internal val StatusRed = Color(STATUS_RED_HEX)
internal val StatusRedSoft = Color(STATUS_RED_SOFT_HEX)
internal val Ink = Color(INK_HEX)
internal val InkSecondary = Color(INK_SECONDARY_HEX)
internal val InkMuted = Color(INK_MUTED_HEX)
internal val DarkMuted = Color(DARK_MUTED_HEX)
internal val LightBackground = Color(LIGHT_BACKGROUND_HEX)
internal val LightBorder = Color(LIGHT_BORDER_HEX)
internal val DarkBackground = Color(DARK_BACKGROUND_HEX)
internal val DarkSurface = Color(DARK_SURFACE_HEX)
internal val DarkElevated = Color(DARK_ELEVATED_HEX)
internal val DarkSecondary = Color(DARK_SECONDARY_HEX)
internal val DarkBorder = Color(DARK_BORDER_HEX)

val SensusPurple = BrandViolet
val SensusPurpleDark = BrandVioletDark
val SensusPurpleLight = BrandViolet
val SensusWhite = Color.White
val SensusDiscountTag = BrandOrange

val SensusSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(SensusWhite, DarkSurface)

val SensusBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(LightBackground, DarkBackground)

val SensusCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(LightBorder, DarkBorder)

val SensusVioletBg: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(BrandVioletSoft, DarkElevated)

val SensusVioletLight: Color
    @Composable
    @ReadOnlyComposable
    get() = SensusVioletBg

val SensusTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(Ink, LightBackground)

val SensusTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(InkSecondary, DarkSecondary)

val SensusTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = SensusTextSecondary

val SensusDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(InkMuted, DarkMuted)

val SensusVioletInk: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(BrandVioletDark, BrandVioletSoft)

val SensusRed: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(StatusRed, StatusRedSoft)

val SensusRedBg: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(StatusRedSoft, DarkElevated)

val SensusSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(StatusGreen, StatusGreenSoft)

val SensusSuccessBg: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(StatusGreenSoft, DarkElevated)

val SensusOrangeDark: Color
    @Composable
    @ReadOnlyComposable
    get() = themed(Ink, BrandOrange)

val SensusRating: Color
    @Composable
    @ReadOnlyComposable
    get() = SensusOrangeDark

@Composable
@ReadOnlyComposable
private fun themed(light: Color, dark: Color): Color = if (LocalSensusDark.current) dark else light
