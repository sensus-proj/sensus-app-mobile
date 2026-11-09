package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.PasswordPolicy
import com.example.sensus.domain.repository.AuthRepository

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        if (token.isBlank()) {
            return Result.Failure(AppError.Validation("El token de restablecimiento es inválido o ha expirado."))
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            return Result.Failure(
                AppError.Validation("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.")
            )
        }
        return authRepository.resetPassword(token, newPassword)
    }
}
