package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendItemDto(
    val userId: String = "",
    val inviteCode: String = "",
    val displayName: String = "",
    val addedVia: String = "QR",
    val connectedAtUtc: String = ""
)

@Serializable
data class PendingFriendRequestDto(
    val requestId: String = "",
    val senderInviteCode: String = "",
    val senderName: String = "",
    val addedVia: String = "QR",
    val createdAtUtc: String = ""
)

@Serializable
data class SentFriendRequestDto(
    val requestId: String = "",
    val targetInviteCode: String = "",
    val targetName: String = "",
    val addedVia: String = "QR",
    val createdAtUtc: String = ""
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
