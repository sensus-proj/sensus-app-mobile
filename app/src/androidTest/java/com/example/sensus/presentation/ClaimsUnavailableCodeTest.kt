package com.example.sensus.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.ClaimOfferContent
import com.example.sensus.presentation.buyer.ClaimOfferViewState
import com.example.sensus.presentation.buyer.ClaimsScreenActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClaimsUnavailableCodeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun existingClaimWithoutCodeShowsExplanationAndNoClaimButtonOrQr() {
        val offer = ProductOffer("offer", "Pan", "Tienda", "Panadería", 20.0, 10.0, 50, 1, 20)
        var submissions = 0
        compose.setContent {
            MaterialTheme {
                ClaimOfferContent(
                    offer,
                    ClaimOfferViewState(offerId = "offer", expectExisting = true),
                    ClaimsScreenActions(expectExisting = true),
                    onClaim = { submissions++ }
                )
            }
        }
        compose.onNodeWithText("Ya tienes un apartado activo", substring = true).assertExists()
        compose.onNodeWithText("Confirmar apartado de una unidad").assertDoesNotExist()
        compose.onNodeWithContentDescription("Código QR del apartado confirmado por el servidor").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, submissions) }
    }
}
