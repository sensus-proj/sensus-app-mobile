package com.example.sensus.presentation.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginScreen_rendersHeaderAndFormElements_properly() {
        var guestClicked = false

        composeTestRule.setContent {
            SensusTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {},
                    onNavigateToOtp = {},
                    onContinueAsGuest = { guestClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Sensus").assertExists()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertExists()
        composeTestRule.onNodeWithText("Continuar como invitado").assertExists().performClick()

        assertTrue(guestClicked)
    }

    @Test
    fun registerScreen_rendersFormElements_properly() {
        var backClicked = false

        composeTestRule.setContent {
            SensusTheme {
                RegisterScreen(
                    onRegisterSuccess = {},
                    onNavigateToLogin = { backClicked = true },
                    onNavigateToTerms = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Crea tu Cuenta").assertExists()
        composeTestRule.onNodeWithText("Únete para descubrir ofertas exclusivas en tu zona").assertExists()
    }
}
