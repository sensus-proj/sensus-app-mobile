package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateContentReportRequest(
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String? = null
)

@Serializable
data class ContentReportResponseDto(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val targetMarkedForReview: Boolean = false,
    val createdAtUtc: String = "",
    val message: String = "Reporte enviado."
)
