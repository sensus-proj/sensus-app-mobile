package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository

class RequestPasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            return Result.Failure(AppError.Validation("Ingresa un correo electrónico válido."))
        }
        return authRepository.requestPasswordReset(trimmed)
    }
}
