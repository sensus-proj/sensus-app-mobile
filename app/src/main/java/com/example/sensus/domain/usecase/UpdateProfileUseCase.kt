package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.ProfileRepository

class UpdateProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(nombreCompleto: String, fotoPerfilUrl: String?): Result<UserProfile> {
        val trimmedName = nombreCompleto.trim()
        if (trimmedName.length !in 2..100) {
            return Result.Failure(AppError.Validation("El nombre debe tener entre 2 y 100 caracteres."))
        }
        return profileRepository.updateProfile(trimmedName, fotoPerfilUrl)
    }
}
