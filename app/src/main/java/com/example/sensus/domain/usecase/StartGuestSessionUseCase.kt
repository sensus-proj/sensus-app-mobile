package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository

class StartGuestSessionUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<String> {
        val result = authRepository.createGuestSession()
        if (result is Result.Success) {
            sessionRepository.saveGuestSession(result.data, 2000)
        }
        return result
    }
}
