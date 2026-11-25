package com.example.sensus.domain.model

data class OfferDetails(
    val offer: ProductOffer,
    val viewCount: Int,
    val hasActiveClaim: Boolean,
    val startsAtMillis: Long,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
) {
    val hasLowStock: Boolean
        get() = offer.isPubliclyVisible && offer.unitsLeft <= LOW_STOCK_THRESHOLD

    val isSoldOut: Boolean
        get() = offer.unitsLeft <= 0 || offer.status == OfferLifecycleStatus.SOLD_OUT

    fun isExpired(nowMillis: Long): Boolean = offer.expiresAtMillis?.let { it <= nowMillis } == true

    fun canClaim(nowMillis: Long): Boolean {
        val isCurrent = startsAtMillis <= nowMillis && (offer.expiresAtMillis ?: 0L) > nowMillis
        return offer.isPubliclyVisible && isCurrent && !hasActiveClaim
    }
}

data class ToggleOfferLikeResult(
    val offerId: String,
    val likesCount: Int,
    val isLikedByMe: Boolean
)

private const val LOW_STOCK_THRESHOLD = 3
