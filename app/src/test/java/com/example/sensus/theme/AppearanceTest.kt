package com.example.sensus.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    @Test
    fun systemModeFollowsConfigurationAndExplicitModesOverrideIt() {
        assertEquals(false, SensusAppearance.SYSTEM.usesDarkTheme(false))
        assertEquals(true, SensusAppearance.SYSTEM.usesDarkTheme(true))
        assertEquals(false, SensusAppearance.LIGHT.usesDarkTheme(true))
        assertEquals(true, SensusAppearance.DARK.usesDarkTheme(false))
    }

    @Test
    fun preferencesRestoreKnownModesAndDefaultToSystem() {
        SensusAppearance.entries.forEach { appearance ->
            assertEquals(appearance, SensusAppearance.fromPreference(appearance.name))
        }
        assertEquals(SensusAppearance.SYSTEM, SensusAppearance.fromPreference(null))
        assertEquals(SensusAppearance.SYSTEM, SensusAppearance.fromPreference("unknown"))
    }

    @Test
    fun changingAppearanceUpdatesTheSelectionAndPersistsIt() {
        val saved = mutableListOf<SensusAppearance>()
        val preference = AppearancePreference(SensusAppearance.SYSTEM) { saved.add(it) }
        preference.select(SensusAppearance.DARK)
        assertEquals(SensusAppearance.DARK, preference.selected)
        assertEquals(listOf(SensusAppearance.DARK), saved)
    }

    @Test
    fun textAndDiscountPairsMeetNormalTextContrast() {
        val pairs = listOf(
            Ink to LightBackground,
            InkSecondary to LightBackground,
            InkSecondary to SensusWhite,
            BrandVioletDark to BrandVioletSoft,
            LightBackground to DarkBackground,
            DarkSecondary to DarkSurface,
            DarkSecondary to DarkElevated,
            BrandVioletSoft to DarkElevated,
            SensusWhite to BrandViolet,
            Ink to BrandOrange,
            Ink to BrandOrangeSoft,
            SensusWhite to StatusRed,
            StatusRedSoft to DarkElevated,
            StatusGreenSoft to DarkElevated
        )
        pairs.forEach { (foreground, background) ->
            val ratio = contrast(foreground, background)
            assertTrue("Contrast of $foreground on $background was $ratio", ratio >= 4.5)
        }
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val luminance = listOf(foreground.luminance(), background.luminance()).sorted()
        return (luminance.last() + 0.05f) / (luminance.first() + 0.05f)
    }
}
