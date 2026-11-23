package com.example.sensus.domain.model

data class FeedPost(
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

data class FeedComment(
    val id: String,
    val postId: String,
    val authorUserId: String,
    val authorName: String,
    val text: String,
    val createdAtUtc: String,
    val authorPhotoUrl: String? = null
)

data class PagedFeedComments(
    val items: List<FeedComment>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val hasNextPage: Boolean
)

data class TogglePostLikeResult(
    val postId: String,
    val likesCount: Int,
    val isLikedByMe: Boolean
)

data class PagedFeed(val posts: List<FeedPost>, val page: Int, val hasMore: Boolean, val isCached: Boolean = false)
data class FeedQuery(val latitude: Double, val longitude: Double, val radiusMeters: Int = 2000, val page: Int = 1, val pageSize: Int = 20)
data class CreatePostInput(val text: String, val latitude: Double, val longitude: Double, val merchantId: String? = null)

object FeedPolicy {
    const val MAX_TEXT_LENGTH = 1000
    const val MAX_COMMENT_LENGTH = 300
    const val MAX_RADIUS = 5000
    const val GUEST_RADIUS = 2000
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0
    fun validCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -MAX_LATITUDE..MAX_LATITUDE && longitude in -MAX_LONGITUDE..MAX_LONGITUDE
}
