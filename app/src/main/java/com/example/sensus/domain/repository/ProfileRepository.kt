package com.example.sensus.domain.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(nombreCompleto: String, fotoPerfilUrl: String?): Result<UserProfile>
    suspend fun requestPhotoSignature(): Result<Pair<String, Long>>
    suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<String> =
        Result.Failure(AppError.Generic("No implementado"))
}
