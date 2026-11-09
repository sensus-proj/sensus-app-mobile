package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository

class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(password: String): Result<Unit> {
        if (password.isBlank()) {
            return Result.Failure(AppError.Validation("Debes ingresar tu contraseña para confirmar la eliminación."))
        }

        val result = authRepository.deleteAccount(password)
        if (result is Result.Success) {
            sessionRepository.clearSession()
        }
        return result
    }
}
