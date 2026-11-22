package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ContentReportResponseDto
import com.example.sensus.data.remote.dto.CreateContentReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportsApiService {
    @POST("api/v1/reports")
    suspend fun submitReport(@Body request: CreateContentReportRequest): Response<ContentReportResponseDto>
}
