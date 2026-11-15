package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.ChangeOfferStatusRequestDto
import com.example.sensus.data.remote.dto.CreateOfferRequest
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.data.remote.dto.PagedOffersResponseDto
import com.example.sensus.data.remote.dto.UpdateOfferRequestDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

private const val DEFAULT_NEARBY_PAGE_SIZE = 50

@Serializable
data class RepublishOfferRequestDto(
    @SerialName("newStartsAt") val newStartsAt: String,
    @SerialName("newEndsAt") val newEndsAt: String,
    @SerialName("newStock") val newStock: Int
)

interface OffersApiService {

    @POST("api/v1/offers/{id}/republish")
    suspend fun republishOffer(@Path("id") id: String, @Body request: RepublishOfferRequestDto): Response<OfferDto>

    @POST("api/v1/offers")
    suspend fun createOffer(@Body request: CreateOfferRequest): Response<OfferDto>

    @GET("api/v1/offers/nearby")
    suspend fun getNearbyOffers(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double? = null,
        @Query("category") category: String? = null,
        @Query("pageSize") pageSize: Int = DEFAULT_NEARBY_PAGE_SIZE
    ): Response<PagedOffersResponseDto>

    @GET("api/v1/merchants/my-offers")
    suspend fun getMyMerchantOffers(@Query("status") status: String? = null): Response<List<OfferDto>>

    @PUT("api/v1/offers/{id}")
    suspend fun updateOffer(@Path("id") id: String, @Body request: UpdateOfferRequestDto): Response<OfferDto>

    @PUT("api/v1/offers/{id}/status")
    suspend fun changeOfferStatus(@Path("id") id: String, @Body request: ChangeOfferStatusRequestDto): Response<OfferDto>
}
