package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.remote.dto.CreateCartClaimRequestDto
import com.example.sensus.data.remote.dto.RedeemClaimResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClaimsApiService {
    @GET("api/v1/offers/claims/{claimId}")
    suspend fun getClaim(@Path("claimId") claimId: String): Response<ClaimDetailDto>

    @POST("api/v1/offers/{id}/claim")
    suspend fun createClaim(@Path("id") id: String): Response<ClaimOfferResultDto>

    @POST("api/v1/offers/claims")
    suspend fun createCartClaim(@Body body: CreateCartClaimRequestDto): Response<ClaimOfferResultDto>

    @POST("api/v1/offers/claims/{code}/redeem")
    suspend fun redeemClaim(@Path("code") code: String): Response<RedeemClaimResultDto>
}
