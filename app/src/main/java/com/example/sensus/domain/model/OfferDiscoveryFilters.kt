package com.example.sensus.domain.model

import java.time.LocalDateTime

enum class OfferSortOrder {
    DISTANCE,
    DISCOUNT,
    PRICE
}

data class OfferDiscoveryFilters(
    val maxPrice: Double? = null,
    val minDiscount: Int? = null,
    val order: OfferSortOrder = OfferSortOrder.DISTANCE,
    val openNowOnly: Boolean = false
) {
    fun applyTo(offers: List<ProductOffer>, now: LocalDateTime = LocalDateTime.now()): List<ProductOffer> {
        val matching = offers.filter { offer ->
            (!openNowOnly || offer.merchantProfile.openStatusAt(now) == MerchantOpenStatus.OPEN) &&
                (maxPrice == null || offer.discountPrice <= maxPrice) &&
                (minDiscount == null || offer.discountPercent >= minDiscount)
        }
        return when (order) {
            OfferSortOrder.DISTANCE -> matching.sortedBy { it.distanceMeters }
            OfferSortOrder.DISCOUNT -> matching.sortedByDescending { it.discountPercent }
            OfferSortOrder.PRICE -> matching.sortedBy { it.discountPrice }
        }
    }
}
