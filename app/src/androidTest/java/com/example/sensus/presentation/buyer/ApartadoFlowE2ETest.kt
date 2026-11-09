package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApartadoFlowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun activeApartadoScreen_rendersConfirmedOrder_withCouponAndStore() {
        val sampleOffer = ProductOffer(
            id = "off-e2e-1",
            name = "Panettone Navideño",
            storeName = "Panadería Don Bosco",
            category = "Panadería",
            originalPrice = 180.0,
            discountPrice = 90.0,
            discountPercent = 50,
            unitsLeft = 2,
            distanceMeters = 400
        )

        val sampleOrder = ApartadoOrder(
            id = "ord-e2e-100",
            storeName = "Panadería Don Bosco",
            address = "Av. Revolución 45",
            latitude = 19.4,
            longitude = -99.15,
            items = listOf(CartItem(sampleOffer, 2)),
            couponCode = "SNS-TEST-99",
            totalOriginalPrice = 360.0,
            totalDiscountPrice = 180.0,
            totalSavedAmount = 180.0,
            timeToCollectMinutes = 30
        )

        composeTestRule.setContent {
            SensusTheme {
                ActiveApartadoScreen(
                    order = sampleOrder,
                    onNavigateBack = {},
                    onNavigateToRoute = {},
                    onCancelApartado = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Apartado confirmado").assertExists()
        composeTestRule.onNodeWithText("Tu ahorro te espera en Panadería Don Bosco").assertExists()
        composeTestRule.onNodeWithText("SNS-TEST-99").assertExists()
    }
}
