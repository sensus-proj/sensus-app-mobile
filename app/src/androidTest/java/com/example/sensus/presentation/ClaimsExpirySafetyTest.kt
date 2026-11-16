package com.example.sensus.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.ClaimOfferContent
import com.example.sensus.presentation.buyer.ClaimOfferViewState
import com.example.sensus.presentation.buyer.ClaimsScreenActions
import com.example.sensus.presentation.common.ConfirmedClaimQr
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClaimsExpirySafetyTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expirationHidesQrAndCodeWithoutResubmitting() {
        val clock = MutableClaimsClock()
        val claim = OfferClaim("claim", "offer", "server-code", "server-payload", 1000L)
        val offer = ProductOffer("offer", "Pan", "Tienda", "Panadería", 20.0, 10.0, 50, 1, 20)
        var submissions = 0
        compose.setContent {
            MaterialTheme {
                ClaimOfferContent(
                    offer,
                    ClaimOfferViewState(result = claim),
                    ClaimsScreenActions(clock = clock),
                    onClaim = { submissions++ }
                )
            }
        }
        compose.onNodeWithContentDescription("Código QR del apartado confirmado por el servidor").assertExists()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { clock.now = 1000L }
        compose.mainClock.advanceTimeBy(1100L)
        compose.waitForIdle()
        compose.onNodeWithText("Apartado vencido.", substring = true).assertExists()
        compose.onNodeWithText("server-code").assertDoesNotExist()
        compose.onNodeWithContentDescription("Código QR del apartado confirmado por el servidor").assertDoesNotExist()
        compose.onNodeWithText("Confirmar apartado de una unidad").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, submissions) }
    }

    @Test
    fun oversizedPayloadShowsFallbackWithoutFalseQr() {
        compose.setContent { MaterialTheme { ConfirmedClaimQr("é".repeat(1025)) } }
        compose.onNodeWithText("No se pudo mostrar el QR.", substring = true).assertExists()
        compose.onNodeWithContentDescription("Código QR del apartado confirmado por el servidor").assertDoesNotExist()
    }
}

private class MutableClaimsClock : Clock() {
    @Volatile
    var now = 0L

    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(now)
    override fun millis(): Long = now
}
