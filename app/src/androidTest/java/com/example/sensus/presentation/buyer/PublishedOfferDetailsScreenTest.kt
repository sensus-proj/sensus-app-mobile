package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.R
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PublishedOfferDetailsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val offer = ProductOffer("offer-1", "Pan", "Tienda", "Abarrotes", 100.0, 50.0, 50, 2, 100, expiresAtMillis = Long.MAX_VALUE)

    @Test
    fun unconfirmedSummaryDoesNotEnableClaim() {
        compose.setContent {
            SensusTheme {
                ProductDetailContent(
                    offer,
                    ProductDetailViewState(
                        error = AppError.NotFound("Ya no disponible")
                    ),
                    ProductDetailActions(ProductDetailCallbacks(), {
                    }, { _, _ -> })
                )
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.detail_claim_one)).assertIsNotEnabled()
        compose.onNodeWithText("Ya no disponible").assertExists()
    }

    @Test
    fun confirmedDetailOpensOneUnitClaimAndRetryDispatchesLoad() {
        var quantity = 0
        val callbacks = ProductDetailCallbacks(onApartarDirect = { _, units -> quantity = units })
        compose.setContent {
            SensusTheme {
                ProductDetailContent(
                    offer,
                    ProductDetailViewState(details = OfferDetails(offer, 1, false, 0), quantity = 2),
                    ProductDetailActions(callbacks, {}, { _, _ -> })
                )
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.detail_claim_one)).performClick()
        assertEquals(1, quantity)
    }

    @Test
    fun failedDetailOffersExplicitRetry() {
        var retries = 0
        compose.setContent { SensusTheme { ProductDetailStatus(ProductDetailViewState(error = AppError.Network()), { retries++ }) } }
        compose.onNodeWithText(compose.activity.getString(R.string.detail_retry)).performClick()
        assertEquals(1, retries)
    }
}
