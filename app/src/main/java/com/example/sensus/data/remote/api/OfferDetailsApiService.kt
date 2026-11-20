package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.OfferDetailsDto
import com.example.sensus.data.remote.dto.OfferLikeStatusDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OfferDetailsApiService {
    @GET("api/v1/offers/{id}")
    suspend fun getDetail(@Path("id") offerId: String): Response<OfferDetailsDto>

    @POST("api/v1/offers/{id}/view")
    suspend fun registerView(@Path("id") offerId: String): Response<Unit>

    @POST("api/v1/offers/{id}/like")
    suspend fun toggleLike(@Path("id") offerId: String): Response<OfferLikeStatusDto>
}
