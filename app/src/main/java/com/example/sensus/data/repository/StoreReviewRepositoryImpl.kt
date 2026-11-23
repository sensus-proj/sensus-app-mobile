package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.StoreReviewsApiService
import com.example.sensus.data.remote.dto.CreateStoreReviewRequest
import com.example.sensus.data.remote.dto.ReplyStoreReviewRequest
import com.example.sensus.data.remote.dto.StoreReviewDto
import com.example.sensus.data.remote.dto.StoreReviewsSummaryDto
import com.example.sensus.domain.model.RatingBreakdownItem
import com.example.sensus.domain.model.ReviewLikeStatus
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.model.StoreReviewReply
import com.example.sensus.domain.model.StoreReviewsSummary
import com.example.sensus.domain.repository.StoreReviewRepository
import retrofit2.Response

class StoreReviewRepositoryImpl(private val api: StoreReviewsApiService) : StoreReviewRepository {

    override suspend fun getReviews(merchantId: String): Result<StoreReviewsSummary> = safeNetworkCall("No se pudieron cargar las reseñas del comercio.") {
        val response = api.getStoreReviews(merchantId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body.toDomain())
        } else {
            Result.Failure(mapError(response))
        }
    }

    override suspend fun createReview(merchantId: String, rating: Int, comment: String): Result<StoreReviewsSummary> =
        safeNetworkCall("No se pudo publicar tu reseña.") {
            val response = api.createStoreReview(merchantId, CreateStoreReviewRequest(rating, comment))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.toDomain())
            } else {
                Result.Failure(mapError(response))
            }
        }

    override suspend fun toggleReviewLike(reviewId: String): Result<ReviewLikeStatus> = safeNetworkCall("No se pudo actualizar tu me gusta.") {
        val response = api.toggleReviewLike(reviewId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(ReviewLikeStatus(body.reviewId, body.likesCount, body.isLikedByMe))
        } else {
            Result.Failure(mapError(response))
        }
    }

    override suspend fun replyToReview(reviewId: String, comment: String): Result<StoreReview> = safeNetworkCall("No se pudo enviar tu respuesta.") {
        val response = api.replyToReview(reviewId, ReplyStoreReviewRequest(comment))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body.toDomain())
        } else {
            Result.Failure(mapError(response))
        }
    }

    private fun mapError(response: Response<*>): AppError = mapCommonHttpStatus(
        response.code(),
        parseBackendErrorMessage(response.errorBody()?.string()),
        "Inicia sesión para calificar o responder comentarios.",
        "No se encontró el comercio o comentario."
    )

    private fun StoreReviewsSummaryDto.toDomain(): StoreReviewsSummary = StoreReviewsSummary(
        merchantId = merchantId,
        merchantName = merchantName,
        averageRating = averageRating,
        totalReviews = totalReviews,
        breakdown = breakdown.map { RatingBreakdownItem(it.stars, it.count) },
        reviews = reviews.map { it.toDomain() }
    )

    private fun StoreReviewDto.toDomain(): StoreReview = StoreReview(
        id = id,
        merchantId = merchantId,
        authorUserId = authorUserId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        rating = rating,
        comment = comment,
        createdAtUtc = createdAtUtc,
        likesCount = likesCount,
        isLikedByMe = isLikedByMe,
        replies = replies.map {
            StoreReviewReply(
                id = it.id,
                authorUserId = it.authorUserId,
                authorName = it.authorName,
                authorPhotoUrl = it.authorPhotoUrl,
                comment = it.comment,
                createdAtUtc = it.createdAtUtc
            )
        }
    )
}
