package com.example.sensus.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sensus.R
import com.example.sensus.theme.AppearancePreference
import com.example.sensus.theme.DarkBackground
import com.example.sensus.theme.LightBackground
import com.example.sensus.theme.LocalAppearancePreference
import com.example.sensus.theme.SensusAppearance
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectingAppearanceUpdatesTheThemeOnACompactScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preference = AppearancePreference(SensusAppearance.SYSTEM) {}
        var appliedColor = Color.Unspecified
        compose.setContent {
            val density = Density(LocalDensity.current.density, fontScale = 1.5f)
            CompositionLocalProvider(LocalAppearancePreference provides preference, LocalDensity provides density) {
                SensusTheme(darkTheme = preference.selected.usesDarkTheme(systemDark = false)) {
                    val background = MaterialTheme.colorScheme.background
                    SideEffect { appliedColor = background }
                    Surface(color = background) {
                        Box(Modifier.width(320.dp)) { AppearanceSettings() }
                    }
                }
            }
        }

        compose.onNodeWithText(context.getString(R.string.appearance_dark)).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(DarkBackground, appliedColor) }
        compose.onNodeWithText(context.getString(R.string.appearance_light)).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(LightBackground, appliedColor) }
        compose.onNodeWithText(context.getString(R.string.appearance_system)).performClick().assertIsSelected()
    }
}
