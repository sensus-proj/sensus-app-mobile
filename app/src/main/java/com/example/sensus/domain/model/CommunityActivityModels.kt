package com.example.sensus.domain.model

enum class CommunityActivityType {
    OFFER_LIKE,
    STORE_REVIEW,
    REVIEW_REPLY
}

data class CommunityActivityItem(
    val id: String,
    val type: CommunityActivityType,
    val actorUserId: String,
    val actorName: String,
    val actorPhotoUrl: String?,
    val offerId: String?,
    val merchantId: String?,
    val targetTitle: String,
    val targetSubtitle: String?,
    val commentText: String?,
    val rating: Int?,
    val createdAtUtc: String
)
