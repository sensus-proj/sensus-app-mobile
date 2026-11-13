package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ModerateImageRequest
import com.example.sensus.data.remote.dto.ModerateImageResponse
import com.example.sensus.data.remote.dto.UploadMediaResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MediaApiService {

    @POST("api/v1/media/moderate")
    suspend fun moderateImage(@Body request: ModerateImageRequest): Response<ModerateImageResponse>

    @Multipart
    @POST("api/v1/media/upload")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<UploadMediaResponse>
}
