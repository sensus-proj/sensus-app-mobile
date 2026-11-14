package com.example.sensus.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.repository.ReverseGeocodingRepository
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidReverseGeocodingRepository(
    private val context: Context
) : ReverseGeocodingRepository {

    override suspend fun resolveAddress(latitude: Double, longitude: Double): Result<ResolvedAddress> {
        if (!Geocoder.isPresent()) {
            return Result.Failure(AppError.Generic("Servicio de geocodificación no disponible en este dispositivo."))
        }
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-MX"))
        val addressResult = fetchFirstAddress(geocoder, latitude, longitude)
        return addressResult.mapOrFallback(latitude, longitude)
    }

    private suspend fun fetchFirstAddress(geocoder: Geocoder, latitude: Double, longitude: Double): Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fetchModern(geocoder, latitude, longitude)
        } else {
            fetchLegacy(geocoder, latitude, longitude)
        }
    }

    private suspend fun fetchModern(geocoder: Geocoder, latitude: Double, longitude: Double): Address? = suspendCancellableCoroutine { continuation ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses.firstOrNull())
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                )
            } else if (continuation.isActive) {
                continuation.resume(null)
            }
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(null)
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun fetchLegacy(geocoder: Geocoder, latitude: Double, longitude: Double): Address? = withContext(Dispatchers.IO) {
        try {
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun Address?.mapOrFallback(latitude: Double, longitude: Double): Result<ResolvedAddress> {
        if (this == null) {
            return Result.Failure(
                AppError.NotFound("No se pudo obtener la dirección exacta; ingrésala manualmente.")
            )
        }
        val street = listOfNotNull(thoroughfare, subThoroughfare).joinToString(" ").trim()
        val area = listOfNotNull(subLocality, locality).distinct().joinToString(", ").trim()
        val combinedStreet = listOf(street, area).filter { it.isNotBlank() }.joinToString(", ")
        val fullAddress = combinedStreet.ifBlank { getAddressLine(0).orEmpty().trim() }
        val postal = postalCode.orEmpty().trim()
        return if (fullAddress.isBlank()) {
            Result.Failure(AppError.NotFound("Dirección no disponible para estas coordenadas."))
        } else {
            Result.Success(
                ResolvedAddress(
                    direccion = fullAddress,
                    codigoPostal = postal,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }
}
