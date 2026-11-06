package com.example.sensus.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class FusedLocationRepository(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationRepository {

    override fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): UserLocation {
        if (!hasLocationPermission()) {
            return DEFAULT_LOCATION
        }

        // 1. Intentar obtener la última ubicación conocida del sistema (rápido en 5ms)
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = lastGps ?: lastNetwork
            if (bestLast != null && (System.currentTimeMillis() - bestLast.time) < 1000 * 60 * 30) {
                return UserLocation(bestLast.latitude, bestLast.longitude)
            }
        } catch (_: Exception) { }

        // 2. Intentar FusedLocationProviderClient con alta precisión (GPS real) con timeout de 6s
        val locationResult = withTimeoutOrNull(6000L) {
            suspendCancellableCoroutine<UserLocation?> { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }

                try {
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null && continuation.isActive) {
                            continuation.resume(UserLocation(lastLoc.latitude, lastLoc.longitude))
                        } else {
                            // Si no hay última ubicación, forzar lectura fresca del chip GPS
                            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                .addOnSuccessListener { freshLoc ->
                                    if (continuation.isActive) {
                                        if (freshLoc != null) {
                                            continuation.resume(UserLocation(freshLoc.latitude, freshLoc.longitude))
                                        } else {
                                            continuation.resume(null)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                        }
                    }.addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

        return locationResult ?: DEFAULT_LOCATION
    }

    companion object {
        // Coordenadas base (Ciudad de México / Tula) para fallback si el usuario no tiene GPS encendido
        val DEFAULT_LOCATION = UserLocation(latitude = 19.432608, longitude = -99.133209)
    }
}
