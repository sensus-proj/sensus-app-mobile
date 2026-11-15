package com.example.sensus.domain.model

data class OfferClaim(val claimId: String, val offerId: String, val code: String, val qrPayload: String, val expiresAtEpochMs: Long)

data class ClaimRedemption(val claimId: String, val offerId: String, val merchantId: String, val redeemedAtEpochMs: Long)
