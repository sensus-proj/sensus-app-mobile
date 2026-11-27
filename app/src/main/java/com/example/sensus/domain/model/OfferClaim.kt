package com.example.sensus.domain.model

data class ClaimItemSummary(
    val offerId: String,
    val quantity: Int,
    val offerTitle: String? = null,
    val unitPrice: Double? = null
)

data class OfferClaim(
    val claimId: String,
    val offerId: String,
    val code: String,
    val qrPayload: String,
    val expiresAtEpochMs: Long,
    val totalUnits: Int = 1,
    val items: List<ClaimItemSummary> = emptyList()
)

data class ClaimRedemption(val claimId: String, val offerId: String, val merchantId: String, val redeemedAtEpochMs: Long)
