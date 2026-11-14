package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.MerchantRepository

/**
 * Consulta los comercios activos cercanos respetando la política espacial de radio (SE-008, FT-07/FT-11).
 */
class GetNearbyMerchantsUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double, radiusMeters: Int, isGuest: Boolean): Result<List<MerchantProfile>> {
        val maxAllowed = if (isGuest) GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS else GuestRadiusPolicy.MAX_RADIUS_METERS
        val safeRadius = radiusMeters.coerceIn(GuestRadiusPolicy.MIN_RADIUS_METERS, maxAllowed)
        return merchantRepository.getNearbyMerchants(latitude, longitude, safeRadius)
    }
}
