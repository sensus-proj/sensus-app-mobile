package com.example.sensus.domain.model

data class OfferDetails(val offer: ProductOffer, val viewCount: Int, val hasActiveClaim: Boolean, val startsAtMillis: Long) {
    fun canClaim(nowMillis: Long): Boolean {
        val isCurrent = startsAtMillis <= nowMillis && (offer.expiresAtMillis ?: 0L) > nowMillis
        return offer.isPubliclyVisible && isCurrent && !hasActiveClaim
    }
}
