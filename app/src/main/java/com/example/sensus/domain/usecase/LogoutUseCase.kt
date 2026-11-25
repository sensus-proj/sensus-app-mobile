package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository

/**
 * Cierra la sesión actual intentando revocar el JWT en POST /api/v1/auth/logout
 * y purgando siempre credenciales cifradas, caché Room+SQLCipher y estado local.
 *
 * FT-39: envía el `deviceId` de la instalación para que el backend deje de mandar push a este teléfono,
 * y olvida el consentimiento de ubicación del usuario que sale.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val pushDeviceId: () -> String? = { null },
    private val onLoggedOut: () -> Unit = {}
) {
    suspend operator fun invoke(): Result<Unit> {
        val deviceId = runCatching { pushDeviceId() }.getOrNull()
        runCatching { authRepository.logout(deviceId) }
        sessionRepository.clearLocalSession()
        runCatching { onLoggedOut() }
        return Result.Success(Unit)
    }
}
