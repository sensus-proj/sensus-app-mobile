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
    val merchantName: String? = null
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
