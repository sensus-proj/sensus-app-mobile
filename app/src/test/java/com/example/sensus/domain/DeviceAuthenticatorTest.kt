package com.example.sensus.domain

import com.example.sensus.domain.security.DeviceAuthOutcome
import com.example.sensus.domain.security.DeviceAuthPrompt
import com.example.sensus.domain.security.DeviceAuthenticator
import com.example.sensus.domain.security.userFacingErrorMessage
import com.example.sensus.presentation.common.runDeviceAuthentication
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAuthenticatorTest {

    @Test
    fun nullAuthenticator_allowsCallerWithoutCrashingHeadlessTests() = runTest {
        val outcome = runDeviceAuthentication(null, DeviceAuthPrompt("Confirmar", "Subtítulo"))
        assertEquals(DeviceAuthOutcome.Authenticated, outcome)
        assertNull(outcome.userFacingErrorMessage())
    }

    @Test
    fun outcomes_produceExplicitSpanishMessagesWhenDeniedOrUnavailable() = runTest {
        val cancelledAuth = DeviceAuthenticator { DeviceAuthOutcome.Cancelled }
        val cancelled = runDeviceAuthentication(cancelledAuth, DeviceAuthPrompt("QR", "Desbloquear"))
        assertEquals(DeviceAuthOutcome.Cancelled, cancelled)
        assertTrue(cancelled.userFacingErrorMessage()?.contains("cancelada") == true)

        val unavailableAuth = DeviceAuthenticator { DeviceAuthOutcome.Unavailable }
        val unavailable = runDeviceAuthentication(unavailableAuth, DeviceAuthPrompt("QR", "Desbloquear"))
        assertEquals(DeviceAuthOutcome.Unavailable, unavailable)
        assertTrue(unavailable.userFacingErrorMessage()?.contains("PIN") == true)

        val blankError = DeviceAuthOutcome.Error(" ")
        assertTrue(blankError.userFacingErrorMessage()?.contains("No fue posible") == true)
        assertEquals("Fallo hardware", DeviceAuthOutcome.Error("Fallo hardware").userFacingErrorMessage())
    }
}
