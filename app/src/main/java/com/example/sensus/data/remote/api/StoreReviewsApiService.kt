package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.CreateStoreReviewRequest
import com.example.sensus.data.remote.dto.ReplyStoreReviewRequest
import com.example.sensus.data.remote.dto.ReviewLikeStatusDto
import com.example.sensus.data.remote.dto.StoreReviewDto
import com.example.sensus.data.remote.dto.StoreReviewsSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreReviewsApiService {
    @GET("api/v1/merchants/{merchantId}/reviews")
    suspend fun getStoreReviews(@Path("merchantId") merchantId: String): Response<StoreReviewsSummaryDto>

    @POST("api/v1/merchants/{merchantId}/reviews")
    suspend fun createStoreReview(@Path("merchantId") merchantId: String, @Body request: CreateStoreReviewRequest): Response<StoreReviewsSummaryDto>

    @POST("api/v1/merchants/reviews/{reviewId}/like")
    suspend fun toggleReviewLike(@Path("reviewId") reviewId: String): Response<ReviewLikeStatusDto>

    @POST("api/v1/merchants/reviews/{reviewId}/replies")
    suspend fun replyToReview(@Path("reviewId") reviewId: String, @Body request: ReplyStoreReviewRequest): Response<StoreReviewDto>
}
