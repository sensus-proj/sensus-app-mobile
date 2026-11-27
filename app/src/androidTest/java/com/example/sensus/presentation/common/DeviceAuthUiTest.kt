package com.example.sensus.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.sensus.domain.security.DeviceAuthOutcome
import com.example.sensus.domain.security.DeviceAuthenticator
import com.example.sensus.presentation.modals.ConfirmDeleteAccountDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeviceAuthUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sensitiveQrGate_requiresAuthenticationBeforeRevealingQr() {
        var nextOutcome: DeviceAuthOutcome = DeviceAuthOutcome.Cancelled
        val authenticator = DeviceAuthenticator { nextOutcome }

        compose.setContent {
            MaterialTheme {
                SensitiveQrGate(unlockKey = "claim-1", deviceAuthenticator = authenticator) {
                    Text("QR-DESBLOQUEADO")
                }
            }
        }

        compose.onNodeWithText("QR-DESBLOQUEADO").assertDoesNotExist()
        compose.onNodeWithText("Mostrar código QR").performClick()
        compose.onNodeWithText("QR-DESBLOQUEADO").assertDoesNotExist()

        nextOutcome = DeviceAuthOutcome.Authenticated
        compose.onNodeWithText("Mostrar código QR").performClick()
        compose.onNodeWithText("QR-DESBLOQUEADO").assertExists()
    }

    @Test
    fun confirmDeleteAccountDialog_blocksDeleteWhenDeviceAuthFails() {
        var nextOutcome: DeviceAuthOutcome = DeviceAuthOutcome.Unavailable
        var confirmedCalls = 0
        val authenticator = DeviceAuthenticator { nextOutcome }

        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDeviceAuthenticator provides authenticator) {
                    ConfirmDeleteAccountDialog(
                        onDismiss = {},
                        onConfirmDeleteWithPassword = { confirmedCalls++ }
                    )
                }
            }
        }

        compose.onNodeWithText("Contraseña actual").performTextInput("Pass1234!")
        compose.onNodeWithText("Eliminar definitivamente").performClick()
        assertEquals(0, confirmedCalls)

        nextOutcome = DeviceAuthOutcome.Authenticated
        compose.onNodeWithText("Eliminar definitivamente").performClick()
        assertEquals(1, confirmedCalls)
    }
}
