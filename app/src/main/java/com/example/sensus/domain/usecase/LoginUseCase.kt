package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(correo: String, password: String): Result<UserProfile> {
        val trimmedCorreo = correo.trim()
        if (trimmedCorreo.isBlank() || !trimmedCorreo.contains("@")) {
            return Result.Failure(AppError.Validation("Ingresa un correo electrónico válido."))
        }
        if (password.isBlank()) {
            return Result.Failure(AppError.Validation("Ingresa tu contraseña."))
        }
        return authRepository.login(trimmedCorreo, password)
    }
}
