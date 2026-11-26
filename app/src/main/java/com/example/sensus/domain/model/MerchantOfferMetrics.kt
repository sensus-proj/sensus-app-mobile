package com.example.sensus.domain.model

private const val LOW_STOCK_LIMIT = 3

/**
 * FT-16 inventory summary computed from GET /merchants/my-offers.
 * Per-offer views and redemptions are not part of that contract, so they are not estimated here.
 */
data class MerchantOfferMetrics(
    val active: Int = 0,
    val paused: Int = 0,
    val finished: Int = 0,
    val availableUnits: Int = 0,
    val lowStock: Int = 0
) {
    val total: Int get() = active + paused + finished

    companion object {
        fun from(offers: List<ProductOffer>): MerchantOfferMetrics {
            val active = offers.filter { it.isPubliclyVisible }
            val paused = offers.count { it.status == OfferLifecycleStatus.PAUSED }
            return MerchantOfferMetrics(
                active = active.size,
                paused = paused,
                finished = offers.size - active.size - paused,
                availableUnits = active.sumOf { it.unitsLeft },
                lowStock = active.count { it.unitsLeft <= LOW_STOCK_LIMIT }
            )
        }
    }
}
