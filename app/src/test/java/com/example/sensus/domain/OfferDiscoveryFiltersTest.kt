package com.example.sensus.domain

import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.OfferDiscoveryFilters
import com.example.sensus.domain.model.OfferSortOrder
import com.example.sensus.domain.model.ProductOffer
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferDiscoveryFiltersTest {
    private val near = ProductOffer("near", "Pan", "Tienda", "Abarrotes", 100.0, 50.0, 50, 3, 100, expiresAtMillis = 200)
    private val far = near.copy(id = "far", discountPrice = 25.0, discountPercent = 75, distanceMeters = 500)

    @Test
    fun appliesInclusivePriceAndDiscountLimits() {
        assertEquals(listOf(far), OfferDiscoveryFilters(maxPrice = 25.0, minDiscount = 75).applyTo(listOf(near, far)))
        assertTrue(OfferDiscoveryFilters(maxPrice = 24.0).applyTo(listOf(near, far)).isEmpty())
        assertTrue(OfferDiscoveryFilters(minDiscount = 76).applyTo(listOf(near, far)).isEmpty())
    }

    @Test
    fun ordersMatchingOffersByTheSelectedCriterion() {
        assertEquals(listOf(near, far), OfferDiscoveryFilters().applyTo(listOf(far, near)))
        assertEquals(listOf(far, near), OfferDiscoveryFilters(order = OfferSortOrder.PRICE).applyTo(listOf(near, far)))
        assertEquals(listOf(far, near), OfferDiscoveryFilters(order = OfferSortOrder.DISCOUNT).applyTo(listOf(near, far)))
    }

    @Test
    fun openNowRequiresAMatchingPublishedScheduleAndRespectsClosingTime() {
        val profile = MerchantProfile(
            nombreComercial = "Panadería",
            direccion = "Calle 1",
            codigoPostal = "42800",
            horarios = listOf(HorarioDia("lunes", "08:00", "18:00"))
        )
        val scheduled = near.copy(merchantProfile = profile)
        val offers = listOf(scheduled, far)
        val filters = OfferDiscoveryFilters(openNowOnly = true)
        val monday = LocalDateTime.of(2026, 11, 23, 17, 59)
        assertEquals(listOf(scheduled), filters.applyTo(offers, monday))
        assertTrue(filters.applyTo(offers, monday.plusMinutes(1)).isEmpty())
        assertEquals(offers, OfferDiscoveryFilters().applyTo(offers, monday))
    }

    @Test
    fun distinguishesLowStockSoldOutAndExpiration() {
        val available = OfferDetails(near, 0, false, 0)
        assertTrue(available.hasLowStock)
        assertFalse(available.copy(offer = near.copy(unitsLeft = 4)).hasLowStock)
        assertFalse(available.isSoldOut)
        assertTrue(available.canClaim(199))
        assertTrue(available.isExpired(200))
        assertFalse(available.canClaim(200))
        val soldOut = available.copy(offer = near.copy(unitsLeft = 0))
        assertTrue(soldOut.isSoldOut)
        assertFalse(soldOut.hasLowStock)
        assertFalse(soldOut.canClaim(199))
    }
}
