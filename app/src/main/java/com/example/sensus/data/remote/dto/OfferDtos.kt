package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/v1/offers` (FT-15, spec backend 034). `expiresAt` en ISO-8601 UTC. */
@Serializable
data class CreateOfferRequest(
    val title: String,
    val description: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val stock: Int,
    val expiresAt: String,
    val mediaId: String? = null
)

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val originalPrice: Double,
    val discountPrice: Double,
    val stock: Int = 1,
    val expiresAt: String? = null,
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
