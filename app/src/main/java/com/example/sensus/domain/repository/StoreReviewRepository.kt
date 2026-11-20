package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ReviewLikeStatus
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.model.StoreReviewsSummary

interface StoreReviewRepository {
    suspend fun getReviews(merchantId: String): Result<StoreReviewsSummary>
    suspend fun createReview(merchantId: String, rating: Int, comment: String): Result<StoreReviewsSummary>
    suspend fun toggleReviewLike(reviewId: String): Result<ReviewLikeStatus>
    suspend fun replyToReview(reviewId: String, comment: String): Result<StoreReview>
}
