package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.sensus.R
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.OfferDiscoveryFilters
import com.example.sensus.domain.model.OfferSortOrder
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
    fun detailsShowPublishedScheduleAndPaymentMethods() {
        val profile = MerchantProfile(
            id = "store",
            nombreComercial = "Tienda",
            direccion = "",
            codigoPostal = "",
            horarios = listOf(HorarioDia("Martes", "09:00", "18:00")),
            paymentMethods = listOf("Tarjeta", "Efectivo")
        )
        val enriched = offer.copy(merchantId = profile.id, merchantProfile = profile)
        compose.setContent {
            SensusTheme {
                ProductDetailContent(
                    enriched,
                    ProductDetailViewState(details = OfferDetails(enriched, 0, false, 0)),
                    ProductDetailActions(ProductDetailCallbacks(), {}, { _, _ -> })
                )
            }
        }
        compose.onNodeWithText("Martes: 09:00 - 18:00").assertExists()
        compose.onNodeWithText("Métodos de pago: Tarjeta, Efectivo").assertExists()
    }

    @Test
    fun confirmedLowStockShowsRemainingUnits() {
        compose.setContent {
            SensusTheme { ProductDetailStatus(ProductDetailViewState(details = OfferDetails(offer, 0, false, 0)), {}) }
        }
        compose.onNodeWithText(compose.activity.resources.getQuantityString(R.plurals.detail_low_stock, 2, 2)).assertExists()
    }

    @Test
    fun soldOutOfferShowsStatusAndDisablesClaim() {
        compose.setContent {
            SensusTheme {
                ProductDetailContent(
                    offer,
                    ProductDetailViewState(details = OfferDetails(offer.copy(unitsLeft = 0), 0, false, 0)),
                    ProductDetailActions(ProductDetailCallbacks(), {}, { _, _ -> })
                )
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.detail_sold_out)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.detail_claim_one)).assertIsNotEnabled()
    }

    @Test
    fun failedDetailOffersExplicitRetry() {
        var retries = 0
        compose.setContent { SensusTheme { ProductDetailStatus(ProductDetailViewState(error = AppError.Network()), { retries++ }) } }
        compose.onNodeWithText(compose.activity.getString(R.string.detail_retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun discoveryFiltersRejectInvalidDiscountAndApplyValidValues() {
        var applied = OfferDiscoveryFilters()
        compose.setContent {
            SensusTheme { OfferDiscoveryFilterSheet(OfferDiscoveryFilters(), { applied = it }, {}) }
        }
        val applyButton = compose.onNodeWithText(compose.activity.getString(R.string.offer_filters_apply))
        val discount = compose.onNodeWithText(compose.activity.getString(R.string.offer_filters_min_discount))
        discount.performTextReplacement("101")
        applyButton.assertIsNotEnabled()

        discount.performTextReplacement("25")
        compose.onNodeWithText(compose.activity.getString(R.string.offer_filters_max_price)).performTextReplacement("80,50")
        compose.onNodeWithText(compose.activity.getString(R.string.offer_filters_price)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.merchant_open_now)).performClick()
        applyButton.performClick()

        compose.runOnIdle { assertEquals(OfferDiscoveryFilters(80.50, 25, OfferSortOrder.PRICE, openNowOnly = true), applied) }
    }
}
