package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModerateImageRequest(
    val imageBase64: String,
    val contentType: String = "image/jpeg"
)

@Serializable
data class ModerateImageResponse(
    val approved: Boolean,
    val message: String,
    val flaggedCategories: List<String>? = null
)

/** Respuesta de `POST /api/v1/media/upload` (FT-15, spec backend 022). */
@Serializable
data class UploadMediaResponse(
    val mediaId: String,
    val url: String,
    val approved: Boolean = false,
    val message: String? = null
)
