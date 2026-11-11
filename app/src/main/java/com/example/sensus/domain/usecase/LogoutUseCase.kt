package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository

/**
 * Cierra la sesión actual intentando revocar el JWT en POST /api/v1/auth/logout
 * y purgando siempre credenciales cifradas, caché Room+SQLCipher y estado local.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        runCatching { authRepository.logout() }
        sessionRepository.clearLocalSession()
        return Result.Success(Unit)
    }
}
