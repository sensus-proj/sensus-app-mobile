package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HorarioDiaDto(
    val dia: Int,
    val horaApertura: String? = null,
    val horaCierre: String? = null,
    val estaCerrado: Boolean = false
)

@Serializable
data class ActivateMerchantRequest(
    val nombreComercial: String,
    val direccion: String,
    val codigoPostal: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoFachadaUrl: String? = null
)

@Serializable
data class UpdateMerchantProfileRequest(
    val nombreComercial: String? = null,
    val direccion: String,
    val codigoPostal: String,
    val horarios: List<HorarioDiaDto>? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoFachadaUrl: String? = null,
    val activo: Boolean? = null
)

@Serializable
data class ComercioDto(
    val id: String,
    val usuarioId: String,
    val nombreComercial: String,
    val direccion: String,
    val codigoPostal: String,
    val latitud: Double,
    val longitud: Double,
    val activo: Boolean = true,
    val fechaRegistro: String? = null,
    val horarios: List<HorarioDiaDto>? = null,
    val fotoFachadaUrl: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0
)

@Serializable
data class StoreReviewReplyDto(
    val id: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val comment: String = "",
    val createdAtUtc: String = ""
)

@Serializable
data class StoreReviewDto(
    val id: String = "",
    val merchantId: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val rating: Int = 5,
    val comment: String = "",
    val createdAtUtc: String = "",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val replies: List<StoreReviewReplyDto> = emptyList()
)

@Serializable
data class RatingBreakdownItemDto(
    val stars: Int = 0,
    val count: Int = 0
)

@Serializable
data class StoreReviewsSummaryDto(
    val merchantId: String = "",
    val merchantName: String = "",
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val breakdown: List<RatingBreakdownItemDto> = emptyList(),
    val reviews: List<StoreReviewDto> = emptyList()
)

@Serializable
data class ReviewLikeStatusDto(
    val reviewId: String = "",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)

@Serializable
data class CreateStoreReviewRequest(
    val rating: Int,
    val comment: String
)

@Serializable
data class ReplyStoreReviewRequest(
    val comment: String
)

@Serializable
data class RegisterComercioResponseDto(
    val mensaje: String = "",
    val comercioId: String = "",
    val esComerciante: Boolean = true,
    val roles: List<String> = emptyList(),
    val comercio: ComercioDto,
    val token: String = "",
    val tokenType: String = "Bearer",
    val expiresAtUtc: String? = null
)
