package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.OfferAlternativeItemDto
import com.example.sensus.data.remote.dto.OfferSearchPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscoveryApiService {
    @GET("api/v1/offers/search")
    suspend fun searchOffers(
        @Query("q") query: String?,
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Double?,
        @Query("page") page: Int
    ): Response<OfferSearchPageDto>

    @GET("api/v1/offers/{id}/alternatives")
    suspend fun getAlternatives(@Path("id") offerId: String, @Query("limit") limit: Int = 5): Response<List<OfferAlternativeItemDto>>
}
