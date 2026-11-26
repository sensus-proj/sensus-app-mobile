package com.example.sensus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaimOfferResultDto(
    @SerialName("claimId") val claimId: String,
    @SerialName("offerId") val offerId: String,
    @SerialName("code") val code: String,
    @SerialName("qrPayload") val qrPayload: String,
    @SerialName("expiresAtUtc") val expiresAtUtc: String
)

@Serializable
data class ClaimDetailDto(
    val claimId: String,
    val offerId: String,
    val status: String,
    val expiresAtUtc: String,
    val qrPayload: String? = null,
    val offerTitle: String? = null
)

@Serializable
data class RedeemClaimResultDto(
    @SerialName("claimId") val claimId: String,
    @SerialName("offerId") val offerId: String,
    @SerialName("merchantId") val merchantId: String,
    @SerialName("redeemedAtUtc") val redeemedAtUtc: String
)
