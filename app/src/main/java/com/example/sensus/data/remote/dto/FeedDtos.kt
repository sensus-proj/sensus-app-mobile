package com.example.sensus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedPostDto(
    val id: String,
    val authorName: String? = null,
    val authorDisplayName: String? = null,
    val authorUserId: String = "",
    val authorPhotoUrl: String? = null,
    val text: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
    val createdAtUtc: String? = null,
    val distanceMeters: Int = 0,
    val imageUrl: String? = null,
    val status: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val isMine: Boolean = false
) {
    fun resolvedAuthorName(): String = (authorDisplayName ?: authorName)?.trim()?.takeIf { it.isNotEmpty() } ?: "Vecino"

    fun resolvedText(): String = (content ?: text).orEmpty()

    fun resolvedCreatedAt(): String = (createdAtUtc ?: createdAt).orEmpty()
}

@Serializable
data class PagedFeedDto(
    val items: List<FeedPostDto> = emptyList(),
    val posts: List<FeedPostDto> = emptyList(),
    val totalCount: Int = 0,
    val pageNumber: Int = 1,
    val page: Int = 1,
    val pageSize: Int = 20,
    val hasNextPage: Boolean = false,
    val hasMore: Boolean = false
) {
    fun resolvedPosts(): List<FeedPostDto> = items.ifEmpty { posts }
    fun resolvedPage(): Int = if (items.isNotEmpty() || pageNumber != 1) pageNumber else page
    fun resolvedHasMore(): Boolean = hasNextPage || hasMore
}

@Serializable
data class SetFeedPostLikeRequest(
    val liked: Boolean
)

@Serializable
data class CreateFeedPostRequest(
    val text: String,
    val content: String = text,
    val latitude: Double,
    val longitude: Double,
    @SerialName("mediaAssetId")
    val mediaId: String? = null,
    val merchantId: String? = null
)

@Serializable
data class TogglePostLikeDto(
    val postId: String = "",
    val likesCount: Int? = null,
    val likeCount: Int? = null,
    val isLikedByMe: Boolean? = null,
    val liked: Boolean? = null
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
