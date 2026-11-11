package com.example.sensus.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.google.android.gms.location.CurrentLocationRequest
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

    override fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override suspend fun getCurrentLocation(): UserLocation = fetchLocation(forceFresh = false)

    override suspend fun getFreshLocation(): UserLocation = fetchLocation(forceFresh = true)

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(forceFresh: Boolean): UserLocation {
        if (!hasLocationPermission()) {
            return DEFAULT_LOCATION
        }

        val lastSystem = getBestLastKnownAndroidLocation()
        val hasFine = hasFineLocationPermission()

        // En carga inicial rápida, reutilizar una lectura muy reciente (< 90 s) y precisa
        if (!forceFresh && lastSystem != null) {
            val ageMs = System.currentTimeMillis() - lastSystem.time
            val isAccurateEnough = !hasFine || !lastSystem.hasAccuracy() || lastSystem.accuracy <= 120f
            if (ageMs in 0L..90_000L && isAccurateEnough) {
                return UserLocation(lastSystem.latitude, lastSystem.longitude)
            }
        }

        val primaryPriority = if (hasFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        if (isLocationEnabled()) {
            // 1. Solicitar lectura fresca del proveedor Fused (GPS + Wi-Fi + Red móvil)
            val freshPrimary = requestFusedCurrentLocation(
                priority = primaryPriority,
                maxAgeMillis = if (forceFresh) 0L else 60_000L,
                timeoutMs = 5500L
            )
            if (freshPrimary != null) return freshPrimary

            // 2. Fallback en interiores: intentar precisión balanceada (Wi-Fi / celdas) si GPS satelital tardó
            if (primaryPriority == Priority.PRIORITY_HIGH_ACCURACY) {
                val balanced = requestFusedCurrentLocation(
                    priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    maxAgeMillis = if (forceFresh) 30_000L else 120_000L,
                    timeoutMs = 3500L
                )
                if (balanced != null) return balanced
            }
        }

        // 3. Fallback a última ubicación conocida de FusedLocationProviderClient
        val fusedLast = requestFusedLastLocation()
        if (fusedLast != null) return fusedLast

        // 4. Fallback a la lectura más reciente conocida por LocationManager
        if (lastSystem != null) {
            return UserLocation(lastSystem.latitude, lastSystem.longitude)
        }

        return DEFAULT_LOCATION
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownAndroidLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val providers = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(LocationManager.FUSED_PROVIDER)
                }
                add(LocationManager.GPS_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
                add(LocationManager.PASSIVE_PROVIDER)
            }
            providers.mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }
            }.maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedCurrentLocation(
        priority: Int,
        maxAgeMillis: Long,
        timeoutMs: Long
    ): UserLocation? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            continuation.invokeOnCancellation { cts.cancel() }
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(priority)
                    .setMaxUpdateAgeMillis(maxAgeMillis)
                    .setDurationMillis(timeoutMs)
                    .build()

                fusedClient.getCurrentLocation(request, cts.token)
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) {
                            continuation.resume(loc?.let { UserLocation(it.latitude, it.longitude) })
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedLastLocation(): UserLocation? = withTimeoutOrNull(1500L) {
        suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) {
                            continuation.resume(loc?.let { UserLocation(it.latitude, it.longitude) })
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    companion object {
        // Coordenadas base (Ciudad de México) para fallback si no hay permisos o GPS activo
        val DEFAULT_LOCATION = UserLocation(latitude = 19.432608, longitude = -99.133209)
    }
}
