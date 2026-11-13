package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.FeedPostDto
import com.example.sensus.domain.model.FeedPost

object FeedMapper {
    fun FeedPostDto.toDomain(): FeedPost = FeedPost(id, authorName, text, createdAt, distanceMeters, imageUrl, merchantId, merchantName)
}
