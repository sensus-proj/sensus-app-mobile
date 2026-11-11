package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile

interface AuthRepository {
    suspend fun login(correo: String, password: String): Result<UserProfile>
    suspend fun register(nombreCompleto: String, correo: String, password: String): Result<UserProfile>
    suspend fun requestPasswordReset(email: String): Result<Unit>
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>
    suspend fun createGuestSession(): Result<String>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(password: String): Result<Unit>
}
