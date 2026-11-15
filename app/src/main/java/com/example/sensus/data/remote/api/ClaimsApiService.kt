package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.remote.dto.RedeemClaimResultDto
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface ClaimsApiService {
    @POST("api/v1/offers/{id}/claim")
    suspend fun createClaim(@Path("id") id: String): Response<ClaimOfferResultDto>

    @POST("api/v1/offers/claims/{code}/redeem")
    suspend fun redeemClaim(@Path("code") code: String): Response<RedeemClaimResultDto>
}
