package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendItemDto(
    val userId: String = "",
    val inviteCode: String = "",
    val displayName: String = "",
    val addedVia: String = "QR",
    val connectedAtUtc: String = "",
    val photoUrl: String? = null
)

@Serializable
data class PendingFriendRequestDto(
    val requestId: String = "",
    val senderInviteCode: String = "",
    val senderName: String = "",
    val addedVia: String = "QR",
    val createdAtUtc: String = "",
    val senderPhotoUrl: String? = null
)

@Serializable
data class SentFriendRequestDto(
    val requestId: String = "",
    val targetInviteCode: String = "",
    val targetName: String = "",
    val addedVia: String = "QR",
    val createdAtUtc: String = "",
    val targetPhotoUrl: String? = null
)

@Serializable
data class FriendsOverviewDto(
    val myInviteCode: String = "",
    val friends: List<FriendItemDto> = emptyList(),
    val pendingReceived: List<PendingFriendRequestDto> = emptyList(),
    val pendingSent: List<SentFriendRequestDto> = emptyList()
)

@Serializable
data class SendFriendRequestBody(
    val inviteCode: String,
    val addedVia: String = "QR"
)

@Serializable
data class SendFriendRequestResponseDto(
    val outcome: String = "",
    val friend: FriendItemDto? = null,
    val pendingRequest: SentFriendRequestDto? = null
)

@Serializable
data class ShareOfferRequestBody(
    val recipientUserIds: List<String>
)

@Serializable
data class SharedOfferItemDto(
    val id: String = "",
    val offerId: String = "",
    val senderUserId: String = "",
    val recipientUserId: String = "",
    val offerTitle: String = "",
    val storeName: String = "",
    val merchantId: String? = null,
    val category: String? = null,
    val originalPrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val discountPercentage: Int = 0,
    val imageUrl: String? = null,
    val sharedAtUtc: String = ""
)

@Serializable
data class ShareOfferResultDto(
    val sharedCount: Int = 0,
    val items: List<SharedOfferItemDto> = emptyList()
)

@Serializable
data class FriendSharedOffersThreadDto(
    val friend: FriendItemDto = FriendItemDto(),
    val offers: List<SharedOfferItemDto> = emptyList()
)

@Serializable
data class CommunityActivityItemDto(
    val id: String = "",
    val activityType: String = "",
    val actorUserId: String = "",
    val actorName: String = "",
    val actorPhotoUrl: String? = null,
    val offerId: String? = null,
    val merchantId: String? = null,
    val targetTitle: String = "",
    val targetSubtitle: String? = null,
    val commentText: String? = null,
    val rating: Int? = null,
    val createdAtUtc: String = ""
)

@Serializable
data class PagedCommunityActivityResponseDto(
    val items: List<CommunityActivityItemDto> = emptyList(),
    val totalCount: Int = 0,
    val pageNumber: Int = 1,
    val pageSize: Int = 20
)
