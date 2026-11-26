package com.example.sensus.presentation.merchant

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MerchantEditOfferContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val offer = ProductOffer(
        id = "bread",
        name = "Pan",
        storeName = "Panadería",
        category = "Panadería",
        originalPrice = 100.0,
        discountPrice = 60.0,
        discountPercent = 40,
        unitsLeft = 2,
        distanceMeters = 350
    )

    @Test
    fun inventoryCardShowsOnlyReportedViewsIncludingZero() {
        val currentOffer = mutableStateOf(offer)
        compose.setContent {
            SensusTheme { MerchantOfferItem(currentOffer.value, {}, {}) }
        }
        compose.onAllNodes(hasText("Vistas:", substring = true)).assertCountEquals(0)
        compose.runOnIdle { currentOffer.value = offer.copy(viewCount = 0) }
        compose.onNodeWithText("Vistas: 0").assertExists()
        compose.runOnIdle { currentOffer.value = offer.copy(viewCount = 17) }
        compose.onNodeWithText("Vistas: 17").assertExists()
        compose.onNodeWithText("Vistas: 0").assertDoesNotExist()
    }

    @Test
    fun editingStockSendsUpdatedInputWhenSaved() {
        val intents = renderOffer()

        compose.onNodeWithContentDescription("Aumentar stock").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(MerchantEditOfferViewIntent.FieldEdited), intents) }
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()

        compose.runOnIdle {
            val save = intents.filterIsInstance<MerchantEditOfferViewIntent.SaveChanges>().single()
            assertEquals(offer.id, save.input.offerId)
            assertEquals(3, save.input.stock)
            assertEquals(offer.version, save.input.expectedVersion)
        }
    }

    @Test
    fun pausingRequiresConfirmationAndCancelDoesNotSendStatusChange() {
        val intents = renderOffer()
        val pauseButton = compose.onNodeWithText("Pausar promoción")
        val confirmButton = compose.onNodeWithText("Sí, pausar")

        pauseButton.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(emptyList<MerchantEditOfferViewIntent>(), intents) }
        compose.onNodeWithText("Cancelar").performClick()
        confirmButton.assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<MerchantEditOfferViewIntent>(), intents) }

        pauseButton.performClick()
        confirmButton.performClick()
        compose.runOnIdle {
            assertEquals(listOf(MerchantEditOfferViewIntent.ChangeStatus(offer, OfferLifecycleStatus.PAUSED)), intents)
        }
    }

    private fun renderOffer(): List<MerchantEditOfferViewIntent> {
        val intents = mutableListOf<MerchantEditOfferViewIntent>()
        compose.setContent {
            SensusTheme {
                MerchantEditOfferContent(offer, MerchantEditOfferViewState(), {}, { intents.add(it) })
            }
        }
        return intents
    }
}
