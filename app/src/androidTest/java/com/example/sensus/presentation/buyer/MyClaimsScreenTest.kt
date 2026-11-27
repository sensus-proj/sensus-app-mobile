package com.example.sensus.presentation.buyer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import org.junit.Rule
import org.junit.Test

class MyClaimsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val active = ClaimSummary("c1", "offer", "Pan dulce", ClaimStatus.ACTIVE, Long.MAX_VALUE, "payload")
    private val redeemed = ClaimSummary("c2", "offer2", "Leche", ClaimStatus.REDEEMED, 10L, null)
    private val expired = ClaimSummary("c3", "offer3", "Cafe", ClaimStatus.EXPIRED, 5L, null)

    @Test
    fun claimsRenderStatusesWithoutCancelButton() {
        compose.setContent {
            MaterialTheme {
                MyClaimsContent(MyClaimsViewState(claims = listOf(active, redeemed, expired), loaded = true), {}, {})
            }
        }
        compose.onNodeWithText("Pan dulce").assertExists()
        compose.onNodeWithText("Canjeada").assertExists()
        compose.onNodeWithText("Vencida").assertExists()
        compose.onNodeWithText("Cancelar reserva").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsShown() {
        compose.setContent { MaterialTheme { MyClaimsContent(MyClaimsViewState(loaded = true), {}, {}) } }
        compose.onNodeWithText("Aún no tienes reservas.").assertExists()
    }
}
