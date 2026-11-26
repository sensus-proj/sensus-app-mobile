package com.example.sensus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationsSettingsDto(
    @SerialName("userId") val userId: String,
    @SerialName("pushEnabled") val pushEnabled: Boolean,
    @SerialName("offerAlerts") val offerAlerts: Boolean,
    @SerialName("socialAlerts") val socialAlerts: Boolean,
    @SerialName("radiusKm") val radiusKm: Int,
    @SerialName("createdAtUtc") val createdAtUtc: String,
    @SerialName("updatedAtUtc") val updatedAtUtc: String? = null
)

@Serializable
data class NotificationsUpdateRequest(
    @SerialName("pushEnabled") val pushEnabled: Boolean?,
    @SerialName("offerAlerts") val offerAlerts: Boolean?,
    @SerialName("socialAlerts") val socialAlerts: Boolean?,
    @SerialName("radiusKm") val radiusKm: Int?
)

@Serializable
class NotificationsDeviceRequest(val deviceId: String, val platform: String, val token: String)

@Serializable
data class NotificationsLocationRequest(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)

@Serializable
class NotificationsDeviceResponse(
    val deviceId: String,
    val platform: String,
    val token: String,
    val isActive: Boolean,
    val registeredAtUtc: String,
    val message: String
)
