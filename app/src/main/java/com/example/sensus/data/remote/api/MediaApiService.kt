package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ModerateImageRequest
import com.example.sensus.data.remote.dto.ModerateImageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MediaApiService {

    @POST("api/v1/media/moderate")
    suspend fun moderateImage(@Body request: ModerateImageRequest): Response<ModerateImageResponse>
}
