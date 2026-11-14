package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ResolvedAddress

interface ReverseGeocodingRepository {
    suspend fun resolveAddress(latitude: Double, longitude: Double): Result<ResolvedAddress>
}
