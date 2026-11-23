package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.remote.dto.RedeemClaimResultDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ClaimsApiService {
    /** FT-13: listado de reservas propias. Contrato propuesto, pendiente de publicacion en el backend. */
    @GET("api/v1/offers/claims")
    suspend fun getMyClaims(): Response<List<ClaimDetailDto>>

    /** FT-13: cancelacion de una reserva activa propia. Contrato propuesto, pendiente de publicacion en el backend. */
    @PATCH("api/v1/offers/claims/{claimId}/cancel")
    suspend fun cancelClaim(@Path("claimId") claimId: String): Response<ClaimDetailDto>

    @GET("api/v1/offers/claims/{claimId}")
    suspend fun getClaim(@Path("claimId") claimId: String): Response<ClaimDetailDto>

    @POST("api/v1/offers/{id}/claim")
    suspend fun createClaim(@Path("id") id: String): Response<ClaimOfferResultDto>

    @POST("api/v1/offers/claims/{code}/redeem")
    suspend fun redeemClaim(@Path("code") code: String): Response<RedeemClaimResultDto>
}
