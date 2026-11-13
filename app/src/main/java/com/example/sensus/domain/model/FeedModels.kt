package com.example.sensus.domain.model

data class FeedPost(
    val id: String,
    val authorName: String,
    val text: String,
    val createdAt: String,
    val distanceMeters: Int = 0,
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val merchantName: String? = null
)

data class PagedFeed(val posts: List<FeedPost>, val page: Int, val hasMore: Boolean, val isCached: Boolean = false)
data class FeedQuery(val latitude: Double, val longitude: Double, val radiusMeters: Int = 2000, val page: Int = 1, val pageSize: Int = 20)
data class CreatePostInput(val text: String, val latitude: Double, val longitude: Double, val merchantId: String? = null)

object FeedPolicy {
    const val MAX_TEXT_LENGTH = 1000
    const val MAX_RADIUS = 5000
    const val GUEST_RADIUS = 2000
    const val MAX_PAGE_SIZE = 100
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0
    fun validCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -MAX_LATITUDE..MAX_LATITUDE && longitude in -MAX_LONGITUDE..MAX_LONGITUDE
}
