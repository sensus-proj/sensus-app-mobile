package com.example.sensus.presentation.main

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sensus.HiltTestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for [com.example.sensus.presentation.main.MainScreen]. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        grantRuntimePermissions()
        composeTestRule.setContent { MainScreen() }
    }

    /**
     * La pantalla del mapa solicita ubicación al abrirse; el diálogo del sistema pausaría la actividad y Compose
     * perdería la jerarquía. Se conceden antes para que la prueba vea la pantalla real.
     */
    private fun grantRuntimePermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ).forEach { permission ->
            runCatching { instrumentation.uiAutomation.grantRuntimePermission(packageName, permission) }
        }
    }

    @Test
    fun appLoads_successfully() {
        composeTestRule.onNodeWithText("Buscar en el mapa...").assertExists()
    }
}
