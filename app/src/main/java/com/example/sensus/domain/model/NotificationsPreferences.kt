package com.example.sensus.domain.model

data class NotificationsPreferences(val pushEnabled: Boolean, val offerAlerts: Boolean, val socialAlerts: Boolean, val radiusKm: Int) {
    companion object {
        const val MIN_RADIUS_KM = 1
        const val MAX_RADIUS_KM = 50
    }
}

// Ephemeral SDK credentials: never persist or log this model.
class NotificationsDevice(val deviceId: String, val token: String)
