package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FT-27 / FT-24: la tarjeta de oferta de Sensi muestra la información completa y sus botones (Apartar, Como llegar,
 * Ver detalle) invocan las acciones con la oferta correcta.
 */
@RunWith(AndroidJUnit4::class)
class SensiOfferCardComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun offer(stock: Int = 1, latitude: Double? = 19.43, longitude: Double? = -99.13) = CitedOffer(
        offerId = "off_1",
        title = "Teclado Gaming",
        merchantId = "m_1",
        merchantName = "Tienda 3B",
        offerPriceMxn = 150.0,
        regularPriceMxn = 200.0,
        availableStock = stock,
        isOpenNow = true,
        distanceKm = 0.4,
        description = "Teclado con RGB para mas FPS",
        discountPercent = 25,
        merchantAddress = "Calle Falsa 123",
        latitude = latitude,
        longitude = longitude
    )

    private fun show(offer: CitedOffer, actions: SensiOfferActions = SensiOfferActions()) {
        composeTestRule.setContent { SensusTheme { SensiOfferCard(offer = offer, actions = actions) } }
    }

    @Test
    fun card_showsTheCompleteOfferInformation() {
        show(offer())

        composeTestRule.onNodeWithText("Teclado Gaming").assertExists()
        composeTestRule.onNodeWithText("Tienda 3B", substring = true).assertExists()
        composeTestRule.onNodeWithText("\$150.00").assertExists()
        composeTestRule.onNodeWithText("\$200.00").assertExists()
        composeTestRule.onNodeWithText("-25%").assertExists()
        composeTestRule.onNodeWithText("Calle Falsa 123").assertExists()
        composeTestRule.onNodeWithText("¡Última unidad!").assertExists()
        composeTestRule.onNodeWithText("Apartar").assertExists()
        composeTestRule.onNodeWithText("Cómo llegar").assertExists()
        composeTestRule.onNodeWithText("Ver detalle").assertExists()
    }

    @Test
    fun buttons_invokeTheirActionWithTheSameOffer() {
        val apartar = mutableListOf<ProductOffer>()
        val route = mutableListOf<ProductOffer>()
        val detail = mutableListOf<ProductOffer>()
        show(
            offer(),
            SensiOfferActions(
                onApartar = { apartar += it },
                onRoute = { route += it },
                onDetail = { detail += it }
            )
        )

        composeTestRule.onNodeWithText("Apartar").performClick()
        composeTestRule.onNodeWithText("Cómo llegar").performClick()
        composeTestRule.onNodeWithText("Ver detalle").performClick()

        assertEquals(listOf("off_1"), apartar.map { it.id })
        assertEquals(listOf("off_1"), route.map { it.id })
        assertEquals(listOf("off_1"), detail.map { it.id })
    }

    @Test
    fun withoutCoordinates_theRouteButtonIsNotOffered() {
        show(offer(latitude = null, longitude = null))

        composeTestRule.onNodeWithText("Cómo llegar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ver detalle").assertExists()
    }

    @Test
    fun outOfStock_disablesApartar() {
        show(offer(stock = 0))

        composeTestRule.onAllNodesWithText("Agotado").assertCountEquals(2)
        composeTestRule.onNode(hasText("Agotado") and hasClickAction()).assertIsNotEnabled()
    }
}
