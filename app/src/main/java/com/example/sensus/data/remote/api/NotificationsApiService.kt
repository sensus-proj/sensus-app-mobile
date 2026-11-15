package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.NotificationsDeviceRequest
import com.example.sensus.data.remote.dto.NotificationsDeviceResponse
import com.example.sensus.data.remote.dto.NotificationsSettingsDto
import com.example.sensus.data.remote.dto.NotificationsUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface NotificationsApiService {
    @GET("api/v1/users/settings")
    suspend fun getSettings(): Response<NotificationsSettingsDto>

    @PUT("api/v1/users/settings")
    suspend fun updateSettings(@Body request: NotificationsUpdateRequest): Response<NotificationsSettingsDto>

    @PUT("api/v1/users/device-token")
    suspend fun registerDevice(@Body request: NotificationsDeviceRequest): Response<NotificationsDeviceResponse>
}
