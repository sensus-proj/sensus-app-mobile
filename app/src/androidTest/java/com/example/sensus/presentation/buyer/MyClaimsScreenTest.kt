package com.example.sensus.presentation.buyer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyClaimsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val active = ClaimSummary("c1", "offer", "Pan dulce", ClaimStatus.ACTIVE, Long.MAX_VALUE, "payload")
    private val redeemed = ClaimSummary("c2", "offer2", "Leche", ClaimStatus.REDEEMED, 10L, null)

    @Test
    fun activeClaimOffersCancelAndOthersDoNot() {
        val intents = mutableListOf<MyClaimsViewIntent>()
        compose.setContent {
            MaterialTheme { MyClaimsContent(MyClaimsViewState(claims = listOf(active, redeemed), loaded = true), {}, { intents += it }) }
        }
        compose.onNodeWithText("Pan dulce").assertExists()
        compose.onNodeWithText("Canjeada").assertExists()
        compose.onNodeWithText("Cancelar reserva").performClick()
        compose.runOnIdle { assertEquals(listOf<MyClaimsViewIntent>(MyClaimsViewIntent.RequestCancel("c1")), intents) }
    }

    @Test
    fun confirmationDialogEmitsConfirmOrDismiss() {
        val intents = mutableListOf<MyClaimsViewIntent>()
        compose.setContent {
            MaterialTheme {
                MyClaimsContent(MyClaimsViewState(claims = listOf(active), loaded = true, pendingCancelClaimId = "c1"), {}, { intents += it })
            }
        }
        compose.onNodeWithText("Conservar").performClick()
        compose.onNodeWithText("Sí, cancelar").performClick()
        compose.runOnIdle { assertEquals(listOf(MyClaimsViewIntent.DismissCancel, MyClaimsViewIntent.ConfirmCancel), intents) }
    }

    @Test
    fun emptyStateIsShown() {
        compose.setContent { MaterialTheme { MyClaimsContent(MyClaimsViewState(loaded = true), {}, {}) } }
        compose.onNodeWithText("Aún no tienes reservas.").assertExists()
    }
}
