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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val DEFAULT_CDMX_LAT = 19.432608
private const val DEFAULT_CDMX_LNG = -99.133209
private const val RECENT_CACHE_AGE_MS = 90_000L
private const val MAX_ACCURACY_METERS = 120f
private const val PRIMARY_CACHE_AGE_MS = 60_000L
private const val PRIMARY_TIMEOUT_MS = 5500L
private const val BALANCED_FORCE_AGE_MS = 30_000L
private const val BALANCED_CACHE_AGE_MS = 120_000L
private const val BALANCED_TIMEOUT_MS = 3500L
private const val LAST_LOCATION_TIMEOUT_MS = 1500L

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
        if (!hasLocationPermission()) return DEFAULT_LOCATION
        val lastSystem = getBestLastKnownAndroidLocation()
        val hasFine = hasFineLocationPermission()
        val recent = recentSystemLocationIfValid(forceFresh, lastSystem, hasFine)
        if (recent != null) return recent

        val live = queryLiveLocation(forceFresh, hasFine)
        return live ?: resolveFallbackLocation(lastSystem)
    }

    private fun recentSystemLocationIfValid(forceFresh: Boolean, lastSystem: Location?, hasFine: Boolean): UserLocation? {
        if (forceFresh || lastSystem == null) return null
        val ageMs = System.currentTimeMillis() - lastSystem.time
        val isAccurateEnough = !hasFine || !lastSystem.hasAccuracy() || lastSystem.accuracy <= MAX_ACCURACY_METERS
        return if (ageMs in 0L..RECENT_CACHE_AGE_MS && isAccurateEnough) {
            UserLocation(lastSystem.latitude, lastSystem.longitude)
        } else {
            null
        }
    }

    private suspend fun queryLiveLocation(forceFresh: Boolean, hasFine: Boolean): UserLocation? {
        if (!isLocationEnabled()) return null
        val primaryPriority = if (hasFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val primary = requestFusedCurrentLocation(
            priority = primaryPriority,
            maxAgeMillis = if (forceFresh) 0L else PRIMARY_CACHE_AGE_MS,
            timeoutMs = PRIMARY_TIMEOUT_MS
        )
        if (primary != null || primaryPriority != Priority.PRIORITY_HIGH_ACCURACY) return primary
        return requestFusedCurrentLocation(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            maxAgeMillis = if (forceFresh) BALANCED_FORCE_AGE_MS else BALANCED_CACHE_AGE_MS,
            timeoutMs = BALANCED_TIMEOUT_MS
        )
    }

    private suspend fun resolveFallbackLocation(lastSystem: Location?): UserLocation {
        val fusedLast = requestFusedLastLocation()
        return when {
            fusedLast != null -> fusedLast
            lastSystem != null -> UserLocation(lastSystem.latitude, lastSystem.longitude)
            else -> DEFAULT_LOCATION
        }
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
                } catch (_: SecurityException) {
                    null
                }
            }.maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedCurrentLocation(priority: Int, maxAgeMillis: Long, timeoutMs: Long): UserLocation? = withTimeoutOrNull(timeoutMs) {
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
            } catch (_: IllegalStateException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedLastLocation(): UserLocation? = withTimeoutOrNull(LAST_LOCATION_TIMEOUT_MS) {
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
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (_: IllegalStateException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    companion object {
        // Coordenadas base (Ciudad de México) para fallback si no hay permisos o GPS activo
        val DEFAULT_LOCATION = UserLocation(latitude = DEFAULT_CDMX_LAT, longitude = DEFAULT_CDMX_LNG)
    }
}
