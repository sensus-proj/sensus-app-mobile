package com.example.sensus.domain.model

private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

data class ResolvedAddress(
    val direccion: String,
    val codigoPostal: String,
    val latitude: Double,
    val longitude: Double
)

object GeoCoordinatePolicy {
    fun validatePair(latitude: Double?, longitude: Double?): String? {
        if ((latitude == null) != (longitude == null)) {
            return "Debes proporcionar tanto latitud como longitud."
        }
        if (latitude == null || longitude == null) {
            return null
        }
        return validateRequired(latitude, longitude)
    }

    fun validateRequired(latitude: Double, longitude: Double): String? {
        return when {
            !latitude.isFinite() || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE ->
                "La latitud debe encontrarse entre -90 y 90 grados."
            !longitude.isFinite() || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE ->
                "La longitud debe encontrarse entre -180 y 180 grados."
            else -> null
        }
    }
}
