package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.PasswordPolicy
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(nombre: String, correo: String, password: String): Result<UserProfile> {
        val trimmedNombre = nombre.trim()
        val trimmedCorreo = correo.trim()
        if (trimmedNombre.length !in 2..100) {
            return Result.Failure(AppError.Validation("El nombre debe tener entre 2 y 100 caracteres."))
        }
        if (trimmedCorreo.isBlank() || !trimmedCorreo.contains("@")) {
            return Result.Failure(AppError.Validation("Ingresa un correo electrónico válido."))
        }
        if (!PasswordPolicy.isValid(password)) {
            return Result.Failure(
                AppError.Validation("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.")
            )
        }
        return authRepository.register(trimmedNombre, trimmedCorreo, password)
    }
}
