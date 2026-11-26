package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.AuthResponse
import com.example.sensus.data.remote.dto.DeleteAccountRequest
import com.example.sensus.data.remote.dto.GuestSessionResponse
import com.example.sensus.data.remote.dto.LoginRequest
import com.example.sensus.data.remote.dto.RefreshTokenRequest
import com.example.sensus.data.remote.dto.RegisterRequest
import com.example.sensus.data.remote.dto.RequestPasswordResetRequest
import com.example.sensus.data.remote.dto.ResetPasswordRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/recover")
    suspend fun requestPasswordReset(@Body request: RequestPasswordResetRequest): Response<Unit>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    @POST("api/v1/auth/guest")
    suspend fun createGuestSession(): Response<GuestSessionResponse>

    /** FT-39: con `deviceId` el backend también desactiva el token push de esta instalación. */
    @POST("api/v1/auth/logout")
    suspend fun logout(@Query("deviceId") deviceId: String?): Response<Unit>

    @HTTP(method = "DELETE", path = "api/v1/users/account", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>
}
