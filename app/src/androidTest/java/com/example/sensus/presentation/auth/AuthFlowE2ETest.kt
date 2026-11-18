package com.example.sensus.presentation.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.HiltTestActivity
import com.example.sensus.theme.SensusTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val GUEST_SESSION_TIMEOUT_MS = 20_000L

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthFlowE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun loginScreen_rendersHeaderAndFormElements_properly() {
        var guestClicked = false

        composeTestRule.setContent {
            SensusTheme {
                LoginScreen(
                    callbacks = LoginCallbacks(
                        onContinueAsGuest = { guestClicked = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Sensus").assertExists()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertExists()
        composeTestRule.onNodeWithText("Continuar como invitado").assertExists().performClick()

        // El ViewModel real abre la sesión de invitado contra el backend: se espera su respuesta (E2E, FT-27).
        composeTestRule.waitUntil(timeoutMillis = GUEST_SESSION_TIMEOUT_MS) { guestClicked }
        assertTrue(guestClicked)
    }

    @Test
    fun registerScreen_rendersFormElements_properly() {
        composeTestRule.setContent {
            SensusTheme {
                RegisterScreen(
                    onRegisterSuccess = {},
                    onNavigateToLogin = {},
                    onNavigateToTerms = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Crea tu Cuenta").assertExists()
        composeTestRule.onNodeWithText("Únete para descubrir ofertas exclusivas en tu zona").assertExists()
    }

    @Test
    fun registerScreen_confirmPassword_showsToggleAndRejectsMismatch() {
        composeTestRule.setContent {
            SensusTheme {
                RegisterScreen(
                    onRegisterSuccess = {},
                    onNavigateToLogin = {},
                    onNavigateToTerms = {}
                )
            }
        }

        composeTestRule.onAllNodesWithContentDescription("Mostrar contraseña").assertCountEquals(2)
        composeTestRule.onNodeWithText("Contraseña (mínimo 6 caracteres)").performScrollTo().performTextInput("Secreta123")
        composeTestRule.onNodeWithText("Confirmar contraseña").performScrollTo().performTextInput("Distinta123")
        composeTestRule.onNodeWithText("Crear Cuenta").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Las contraseñas no coinciden").performScrollTo().assertExists()

        composeTestRule.onAllNodesWithContentDescription("Mostrar contraseña")[0].performClick()
        composeTestRule.onAllNodesWithContentDescription("Ocultar contraseña").assertCountEquals(1)
    }
}
