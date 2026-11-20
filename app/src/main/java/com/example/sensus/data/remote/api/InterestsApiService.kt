package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.UpdateInterestsRequestDto
import com.example.sensus.data.remote.dto.UserInterestsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface InterestsApiService {
    @GET("api/v1/users/me/interests")
    suspend fun getMyInterests(): Response<UserInterestsResponseDto>

    @PUT("api/v1/users/me/interests")
    suspend fun updateMyInterests(@Body body: UpdateInterestsRequestDto): Response<UserInterestsResponseDto>
}
