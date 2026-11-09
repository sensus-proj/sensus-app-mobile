package com.example.sensus.domain.repository

import com.example.sensus.domain.model.UserLocation

interface LocationRepository {
    suspend fun getCurrentLocation(): UserLocation
    fun hasLocationPermission(): Boolean
}
