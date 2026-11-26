package com.example.sensus.presentation.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
        composeTestRule.onNodeWithText("Mirar como invitado").performScrollTo().performClick()

        // El ViewModel real abre la sesión de invitado contra el backend: se espera su respuesta (E2E, FT-27).
        composeTestRule.waitUntil(timeoutMillis = GUEST_SESSION_TIMEOUT_MS) { guestClicked }
        assertTrue(guestClicked)
    }

    @Test
    fun compactLoginWithLargeText_keepsValidationAndSecondaryActionsReachable() {
        var smsOpened = false
        var recoveryOpened = false
        var registerOpened = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.5f)) {
                SensusTheme {
                    Box(Modifier.size(width = 320.dp, height = 480.dp)) {
                        LoginScreen(
                            callbacks = LoginCallbacks(
                                onNavigateToOtp = { smsOpened = true },
                                onForgotPassword = { recoveryOpened = true },
                                onNavigateToRegister = { registerOpened = true }
                            )
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        composeTestRule.onNodeWithContentDescription("Ocultar contraseña").assertExists()
        composeTestRule.onNodeWithText("Entrar").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Ingresa tu correo o teléfono.").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Entrar con código SMS").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Iniciar sesión").assertIsSelected()
        composeTestRule.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        composeTestRule.runOnIdle {
            assertTrue(recoveryOpened)
            assertTrue(smsOpened)
            assertTrue(registerOpened)
        }
    }

    @Test
    fun registerScreen_rendersFormElements_properly() {
        var loginOpened = false
        composeTestRule.setContent {
            SensusTheme(darkTheme = true) {
                RegisterScreen(
                    onRegisterSuccess = {},
                    onNavigateToLogin = { loginOpened = true },
                    onNavigateToTerms = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Lo bueno, por menos").assertExists()
        composeTestRule.onNodeWithText("Crear cuenta").assertIsSelected()
        composeTestRule.onNodeWithText("Crea tu Cuenta").assertExists()
        composeTestRule.onNodeWithText("Únete para descubrir ofertas exclusivas en tu zona").assertExists()
        composeTestRule.onNodeWithText("Mirar como invitado").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        composeTestRule.runOnIdle { assertTrue(loginOpened) }
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
