package com.example.sensus.domain.model

data class UserLocation(
    val latitude: Double = 20.0536,
    val longitude: Double = -99.3436
)

object GuestRadiusPolicy {
    const val GUEST_MAX_RADIUS_METERS = 2000
    const val GUEST_MAX_RADIUS_KM = 2.0f

    fun isRadiusAllowedForGuest(radiusKm: Float): Boolean {
        return radiusKm <= GUEST_MAX_RADIUS_KM
    }

    fun clampRadius(radiusKm: Float, isGuest: Boolean): Float {
        return if (isGuest && radiusKm > GUEST_MAX_RADIUS_KM) GUEST_MAX_RADIUS_KM else radiusKm
    }
}
