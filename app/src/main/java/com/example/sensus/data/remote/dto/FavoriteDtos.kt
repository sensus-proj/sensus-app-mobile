package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteToggleResultDto(
    val offerId: String,
    val favorited: Boolean,
    val favoriteCount: Int = 0
)

@Serializable
data class FavoriteOfferDto(
    val id: String,
    val merchantId: String = "",
    val merchantName: String = "",
    val category: String = "",
    val title: String,
    val description: String = "",
    val originalPrice: Double = 0.0,
    val discountPercentage: Int = 0,
    val discountedPrice: Double = 0.0,
    val stock: Int = 0,
    val imageUrl: String? = null,
    val startsAt: String = "",
    val endsAt: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val likesCount: Int = 0,
    val favoritedAtUtc: String = ""
)

@Serializable
data class FavoritePageDto(
    val items: List<FavoriteOfferDto> = emptyList(),
    val totalCount: Int = 0,
    val pageNumber: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 0,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false
)
