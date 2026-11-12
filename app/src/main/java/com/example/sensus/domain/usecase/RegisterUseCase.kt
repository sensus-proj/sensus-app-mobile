package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.PasswordPolicy
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository

private const val MIN_NAME_LENGTH = 2
private const val MAX_NAME_LENGTH = 100

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(nombre: String, correo: String, password: String): Result<UserProfile> {
        val trimmedNombre = nombre.trim()
        val trimmedCorreo = correo.trim()
        val validationMessage = when {
            trimmedNombre.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH ->
                "El nombre debe tener entre $MIN_NAME_LENGTH y $MAX_NAME_LENGTH caracteres."
            trimmedCorreo.isBlank() || !trimmedCorreo.contains("@") ->
                "Ingresa un correo electrónico válido."
            !PasswordPolicy.isValid(password) ->
                "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número."
            else -> null
        }
        return if (validationMessage != null) {
            Result.Failure(AppError.Validation(validationMessage))
        } else {
            authRepository.register(trimmedNombre, trimmedCorreo, password)
        }
    }
}
