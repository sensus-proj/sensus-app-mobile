package com.example.sensus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartClaimItemDto(
    @SerialName("offerId") val offerId: String,
    @SerialName("quantity") val quantity: Int = 1
)

@Serializable
data class CreateCartClaimRequestDto(
    @SerialName("items") val items: List<CartClaimItemDto>
)

@Serializable
data class OfferClaimItemDto(
    @SerialName("offerId") val offerId: String,
    @SerialName("quantity") val quantity: Int = 1,
    @SerialName("offerTitle") val offerTitle: String? = null,
    @SerialName("unitPrice") val unitPrice: Double? = null
)

@Serializable
data class ClaimOfferResultDto(
    @SerialName("claimId") val claimId: String,
    @SerialName("offerId") val offerId: String,
    @SerialName("code") val code: String,
    @SerialName("qrPayload") val qrPayload: String,
    @SerialName("expiresAtUtc") val expiresAtUtc: String,
    @SerialName("totalUnits") val totalUnits: Int = 1,
    @SerialName("items") val items: List<OfferClaimItemDto> = emptyList()
)

@Serializable
data class ClaimDetailDto(
    val claimId: String,
    val offerId: String,
    val status: String,
    val expiresAtUtc: String,
    val qrPayload: String? = null,
    val offerTitle: String? = null,
    val totalUnits: Int = 1,
    val items: List<OfferClaimItemDto> = emptyList()
)

@Serializable
data class RedeemClaimResultDto(
    @SerialName("claimId") val claimId: String,
    @SerialName("offerId") val offerId: String,
    @SerialName("merchantId") val merchantId: String,
    @SerialName("redeemedAtUtc") val redeemedAtUtc: String,
    @SerialName("totalUnits") val totalUnits: Int = 1,
    @SerialName("items") val items: List<OfferClaimItemDto> = emptyList()
)
