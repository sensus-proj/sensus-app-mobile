package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository

private const val GUEST_MAX_RADIUS_METERS = 2000

class StartGuestSessionUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<String> {
        val result = authRepository.createGuestSession()
        if (result is Result.Success) {
            sessionRepository.saveGuestSession(result.data, GUEST_MAX_RADIUS_METERS)
        }
        return result
    }
}
