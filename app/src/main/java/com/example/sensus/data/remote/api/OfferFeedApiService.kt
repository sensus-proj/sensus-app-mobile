package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.OfferFeedPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OfferFeedApiService {
    @GET("api/v1/feed/offers")
    suspend fun getOfferFeed(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Int,
        @Query("page") page: Int,
        @Query("snapshotId") snapshotId: String? = null
    ): Response<OfferFeedPageDto>
}
