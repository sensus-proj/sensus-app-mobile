package com.example.sensus.domain.repository

import com.example.sensus.domain.model.UserLocation

interface LocationRepository {
    suspend fun getCurrentLocation(): UserLocation
    suspend fun getFreshLocation(): UserLocation = getCurrentLocation()
    fun hasLocationPermission(): Boolean
    fun hasFineLocationPermission(): Boolean = hasLocationPermission()
    fun isLocationEnabled(): Boolean = true
}
