package com.example.sensus.domain.model

data class StoreReviewReply(
    val id: String,
    val authorUserId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val comment: String,
    val createdAtUtc: String
)

data class StoreReview(
    val id: String,
    val merchantId: String,
    val authorUserId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val rating: Int,
    val comment: String,
    val createdAtUtc: String,
    val likesCount: Int,
    val isLikedByMe: Boolean,
    val replies: List<StoreReviewReply>
)

data class RatingBreakdownItem(
    val stars: Int,
    val count: Int
)

data class StoreReviewsSummary(
    val merchantId: String,
    val merchantName: String,
    val averageRating: Double,
    val totalReviews: Int,
    val breakdown: List<RatingBreakdownItem>,
    val reviews: List<StoreReview>
)

data class ReviewLikeStatus(
    val reviewId: String,
    val likesCount: Int,
    val isLikedByMe: Boolean
)
