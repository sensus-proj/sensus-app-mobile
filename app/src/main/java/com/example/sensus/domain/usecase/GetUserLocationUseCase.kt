package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository

class GetUserLocationUseCase(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(): UserLocation {
        return locationRepository.getCurrentLocation()
    }
}
