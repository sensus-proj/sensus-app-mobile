package com.example.sensus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeedPostDto(
    val id: String,
    val authorName: String,
    val text: String,
    val createdAt: String,
    val distanceMeters: Int = 0,
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val isMine: Boolean = false
)

@Serializable
data class PagedFeedDto(val posts: List<FeedPostDto>, val page: Int, val hasMore: Boolean)

@Serializable
data class CreateFeedPostRequest(
    val text: String,
    val latitude: Double,
    val longitude: Double,
    val mediaId: String? = null,
    val merchantId: String? = null
)

@Serializable
data class TogglePostLikeDto(
    val postId: String = "",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)

@Serializable
data class CreateFeedCommentRequest(
    val content: String
)

@Serializable
data class FeedCommentDto(
    val id: String,
    val postId: String,
    val authorUserId: String = "",
    val authorName: String = "Usuario",
    val authorPhotoUrl: String? = null,
    val content: String = "",
    val createdAtUtc: String = ""
)

@Serializable
data class PagedFeedCommentsDto(
    val items: List<FeedCommentDto> = emptyList(),
    val totalCount: Int = 0,
    val pageNumber: Int = 1,
    val pageSize: Int = 20,
    val hasNextPage: Boolean = false
)
