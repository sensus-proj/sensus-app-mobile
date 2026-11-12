package com.example.sensus.domain.model

private const val DEFAULT_TULA_LAT = 20.0536
private const val DEFAULT_TULA_LNG = -99.3436

data class UserLocation(
    val latitude: Double = DEFAULT_TULA_LAT,
    val longitude: Double = DEFAULT_TULA_LNG
)

object GuestRadiusPolicy {
    const val MIN_RADIUS_METERS = 50
    const val MIN_RADIUS_KM = 0.05f
    const val GUEST_MAX_RADIUS_METERS = 2000
    const val GUEST_MAX_RADIUS_KM = 2.0f
    const val MAX_RADIUS_METERS = 5000
    const val MAX_RADIUS_KM = 5.0f

    fun isRadiusAllowedForGuest(radiusKm: Float): Boolean {
        return radiusKm in MIN_RADIUS_KM..GUEST_MAX_RADIUS_KM
    }

    fun clampRadius(radiusKm: Float, isGuest: Boolean): Float {
        val maxAllowed = if (isGuest) GUEST_MAX_RADIUS_KM else MAX_RADIUS_KM
        return radiusKm.coerceIn(MIN_RADIUS_KM, maxAllowed)
    }
}
