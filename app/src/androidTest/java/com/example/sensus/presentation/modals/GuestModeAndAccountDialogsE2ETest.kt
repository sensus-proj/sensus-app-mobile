package com.example.sensus.presentation.modals

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuestModeAndAccountDialogsE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun guestPromptDialog_rendersRestrictionsAndHandlesNavigation() {
        var loginClicked = false
        var registerClicked = false

        composeTestRule.setContent {
            SensusTheme {
                GuestPromptDialog(
                    onDismiss = {},
                    onNavigateToLogin = { loginClicked = true },
                    onNavigateToRegister = { registerClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Inicia sesión para continuar").assertExists()
        composeTestRule.onNodeWithText(
            "Estás navegando en Modo Invitado (2 km). Para apartar ofertas, ampliar tu radio, " +
                "publicar productos o guardar favoritos necesitas una cuenta Sensus."
        ).assertExists()

        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()
        assertTrue(loginClicked)

        composeTestRule.onNodeWithText("Registrarme").performClick()
        assertTrue(registerClicked)
    }

    @Test
    fun confirmDeleteAccountDialog_requiresPassword_beforeConfirming() {
        var confirmedPassword = ""

        composeTestRule.setContent {
            SensusTheme {
                ConfirmDeleteAccountDialog(
                    onDismiss = {},
                    onConfirmDeleteWithPassword = { confirmedPassword = it }
                )
            }
        }

        composeTestRule.onNodeWithText("¿Eliminar cuenta definitivamente?").assertExists()
        composeTestRule.onNodeWithText(
            "Esta acción es irreversible. Se eliminarán de forma permanente tus ahorros acumulados, historial de apartados y nivel de usuario."
        ).assertExists()

        // Type password to enable delete button
        composeTestRule.onNodeWithText("Contraseña actual").performTextInput("Pass1234!")
        composeTestRule.onNodeWithText("Eliminar definitivamente").performClick()

        assertEquals("Pass1234!", confirmedPassword)
    }
}
