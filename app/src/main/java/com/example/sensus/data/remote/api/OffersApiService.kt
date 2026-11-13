package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.CreateOfferRequest
import com.example.sensus.data.remote.dto.OfferDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OffersApiService {

    @POST("api/v1/offers")
    suspend fun createOffer(@Body request: CreateOfferRequest): Response<OfferDto>
}
