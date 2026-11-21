package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.ContentReportResponseDto
import com.example.sensus.data.remote.dto.FeedCommentDto
import com.example.sensus.data.remote.dto.FeedPostDto
import com.example.sensus.data.remote.dto.PagedFeedCommentsDto
import com.example.sensus.data.remote.dto.TogglePostLikeDto
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.model.TogglePostLikeResult

object FeedMapper {
    fun FeedPostDto.toDomain(): FeedPost = FeedPost(
        id = id,
        authorName = authorName,
        text = text,
        createdAt = createdAt,
        distanceMeters = distanceMeters,
        imageUrl = imageUrl,
        merchantId = merchantId,
        merchantName = merchantName,
        likesCount = likesCount,
        isLikedByMe = isLikedByMe,
        commentsCount = commentsCount,
        isMine = isMine
    )

    fun TogglePostLikeDto.toDomain(fallbackPostId: String): TogglePostLikeResult = TogglePostLikeResult(
        postId = postId.ifBlank { fallbackPostId },
        likesCount = likesCount,
        isLikedByMe = isLikedByMe
    )

    fun FeedCommentDto.toDomain(): FeedComment = FeedComment(
        id = id,
        postId = postId,
        authorUserId = authorUserId,
        authorName = authorName,
        text = content,
        createdAtUtc = createdAtUtc,
        authorPhotoUrl = authorPhotoUrl
    )

    fun PagedFeedCommentsDto.toDomain(): PagedFeedComments = PagedFeedComments(
        items = items.map { it.toDomain() },
        totalCount = totalCount,
        pageNumber = pageNumber,
        pageSize = pageSize,
        hasNextPage = hasNextPage
    )

    fun ContentReportResponseDto.toDomain(): SubmittedReport = SubmittedReport(
        id = id,
        targetType = targetType,
        targetId = targetId,
        reason = reason,
        targetMarkedForReview = targetMarkedForReview,
        createdAtUtc = createdAtUtc,
        message = message
    )
}
