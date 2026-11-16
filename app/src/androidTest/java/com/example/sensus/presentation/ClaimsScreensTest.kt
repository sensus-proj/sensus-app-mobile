package com.example.sensus.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.ClaimOfferContent
import com.example.sensus.presentation.buyer.ClaimOfferViewState
import com.example.sensus.presentation.buyer.ClaimsScreenActions
import com.example.sensus.presentation.merchant.MerchantSalesContent
import com.example.sensus.presentation.merchant.MerchantSalesViewIntent
import com.example.sensus.presentation.merchant.MerchantSalesViewState
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClaimsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buyerOnlyClaimsAfterExplicitButton() {
        var calls = 0
        val offer = ProductOffer("offer", "Pan", "Tienda", "Panadería", 20.0, 10.0, 50, 1, 20)
        compose.setContent {
            MaterialTheme { ClaimOfferContent(offer, ClaimOfferViewState(), ClaimsScreenActions(), onClaim = { calls++ }) }
        }
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Confirmar apartado de una unidad").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun confirmedBuyerDisplaysOnlyBackendCodeExpiryAndQr() {
        var routes = 0
        val offer = ProductOffer("offer", "Pan", "Tienda", "Panadería", 20.0, 10.0, 50, 1, 20)
        val claim = OfferClaim("claim", "offer", "backend-code", "backend-payload", 1000L)
        compose.setContent {
            MaterialTheme {
                ClaimOfferContent(
                    offer,
                    ClaimOfferViewState(result = claim),
                    ClaimsScreenActions(
                        onNavigateToRoute = { routes++ },
                        clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
                    ),
                    onClaim = {}
                )
            }
        }
        compose.onNodeWithText("backend-code").assertExists()
        compose.onNodeWithText("Vence: 1970-01-01T00:00:01Z").assertExists()
        compose.onNodeWithText("Cómo llegar").performClick()
        compose.runOnIdle { assertEquals(1, routes) }
        compose.onNodeWithContentDescription("Código QR del apartado confirmado por el servidor").assertExists()
        compose.onNodeWithText("Confirmar apartado de una unidad").assertDoesNotExist()
    }

    @Test
    fun merchantCapturesLiteralCodeWithoutLocalSuccess() {
        val intents = mutableListOf<MerchantSalesViewIntent>()
        compose.setContent {
            MaterialTheme { MerchantSalesContent(MerchantSalesViewState(), { intents.add(it) }) }
        }
        compose.onNodeWithText("Código del cliente").performTextInput("Ab.signed")
        compose.runOnIdle { assertEquals(MerchantSalesViewIntent.Edit("Ab.signed"), intents.last()) }
        compose.onNodeWithText("Validar y canjear").assertIsNotEnabled()
        compose.onNodeWithText("Canje confirmado por el servidor", substring = true).assertDoesNotExist()
    }

    @Test
    fun merchantFailureHasNoConfirmation() {
        compose.setContent {
            MaterialTheme { MerchantSalesContent(MerchantSalesViewState(error = AppError.Forbidden()), {}) }
        }
        compose.onNodeWithText("Tu cuenta o comercio no tiene permiso", substring = true).assertExists()
        compose.onNodeWithText("Canje confirmado por el servidor", substring = true).assertDoesNotExist()
    }

    @Test
    fun onlyBackendResultDisplaysRedemptionConfirmation() {
        compose.setContent {
            MaterialTheme {
                MerchantSalesContent(MerchantSalesViewState(result = ClaimRedemption("c", "o", "m", 0L)), {})
            }
        }
        compose.onNodeWithText("Canje confirmado por el servidor: 1970-01-01T00:00:00Z").assertExists()
    }
}
