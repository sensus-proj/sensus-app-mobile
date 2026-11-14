package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ActivateMerchantRequest
import com.example.sensus.data.remote.dto.ComercioDto
import com.example.sensus.data.remote.dto.RegisterComercioResponseDto
import com.example.sensus.data.remote.dto.UpdateMerchantProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MerchantApiService {

    @POST("api/v1/merchants/activate")
    suspend fun activateMerchant(@Body request: ActivateMerchantRequest): Response<RegisterComercioResponseDto>

    @POST("api/v1/comercios")
    suspend fun registerComercioFallback(@Body request: ActivateMerchantRequest): Response<RegisterComercioResponseDto>

    @GET("api/v1/merchants/me")
    suspend fun getMyComercio(): Response<ComercioDto>

    @GET("api/v1/comercios/me")
    suspend fun getMyComercioFallback(): Response<ComercioDto>

    @GET("api/v1/merchants/nearby")
    suspend fun getNearbyMerchants(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radioMetros") radioMetros: Int? = null
    ): Response<List<ComercioDto>>

    @GET("api/v1/comercios/nearby")
    suspend fun getNearbyComerciosFallback(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radioMetros") radioMetros: Int? = null
    ): Response<List<ComercioDto>>

    @PUT("api/v1/merchants/profile")
    suspend fun updateMerchantProfile(@Body request: UpdateMerchantProfileRequest): Response<ComercioDto>

    @PUT("api/v1/comercios/me")
    suspend fun updateMyComercioFallback(@Body request: UpdateMerchantProfileRequest): Response<ComercioDto>
}
