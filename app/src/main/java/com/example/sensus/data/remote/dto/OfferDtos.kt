package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/v1/offers` (FT-15, spec backend 034). Compatible con CreateOfferCommand (.NET). */
@Serializable
data class CreateOfferRequest(
    val title: String,
    val description: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val discountPercentage: Int = 0,
    val stock: Int,
    val startsAt: String = "",
    val endsAt: String = "",
    val expiresAt: String,
    val imageUrl: String? = null,
    val mediaId: String? = null
)

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val originalPrice: Double,
    val discountPrice: Double? = null,
    val discountedPrice: Double? = null,
    val discountPercentage: Int? = null,
    val stock: Int = 1,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val expiresAt: String? = null,
    val status: String? = null,
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceMeters: Double? = null,
    val version: Int = 1,
    val viewCount: Int? = null,
    val viewsCount: Int? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)

/** Respuesta paginada de `GET /api/v1/offers/nearby` y `GET /api/v1/merchants/my-offers` (Specs 007 y 037). */
@Serializable
data class PagedOffersResponseDto(
    val items: List<OfferDto> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageNumber: Int? = null,
    val pageSize: Int = 20
) {
    fun resolvedPage(): Int = pageNumber ?: page
}

/** Cuerpo de `PUT /api/v1/offers/{id}` (Spec 035). */
@Serializable
data class UpdateOfferRequestDto(
    val title: String,
    val description: String,
    val originalPrice: Double,
    val discountPercentage: Int,
    val stock: Int,
    val startsAt: String,
    val endsAt: String,
    val imageUrl: String? = null,
    val expectedVersion: Int = 1
)

/** Cuerpo de `PUT /api/v1/offers/{id}/status` (Spec 036). */
@Serializable
data class ChangeOfferStatusRequestDto(
    val newStatus: String
)
