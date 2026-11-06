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
