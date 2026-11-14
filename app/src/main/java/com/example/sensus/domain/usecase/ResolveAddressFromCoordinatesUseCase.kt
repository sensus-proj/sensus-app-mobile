package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GeoCoordinatePolicy
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.repository.ReverseGeocodingRepository

class ResolveAddressFromCoordinatesUseCase(
    private val reverseGeocodingRepository: ReverseGeocodingRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<ResolvedAddress> {
        val coordinateError = GeoCoordinatePolicy.validateRequired(latitude, longitude)
        return if (coordinateError != null) {
            Result.Failure(AppError.Validation(coordinateError))
        } else {
            reverseGeocodingRepository.resolveAddress(latitude, longitude)
        }
    }
}
