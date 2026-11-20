package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

/** Published OfferDetailDto, backend Spec 032. */
@Serializable
data class OfferDetailsDto(
    val id: String,
    val merchantId: String,
    val merchantName: String,
    val merchantAddress: String,
    val category: String,
    val title: String,
    val description: String,
    val originalPrice: Double,
    val discountPercentage: Int,
    val discountedPrice: Double,
    val stock: Int,
    val status: String,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startsAt: String,
    val endsAt: String,
    val viewCount: Int,
    val hasActiveClaim: Boolean,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)

@Serializable
data class OfferLikeStatusDto(
    val offerId: String,
    val likesCount: Int,
    val isLikedByMe: Boolean
)
