package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ReviewLikeStatus
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.model.StoreReviewsSummary
import com.example.sensus.domain.moderation.ProfanityFilter
import com.example.sensus.domain.repository.StoreReviewRepository

private const val MIN_STAR_RATING = 1
private const val MAX_STAR_RATING = 5
private const val MAX_REVIEW_LENGTH = 600
private const val MAX_REPLY_LENGTH = 500

class GetStoreReviewsUseCase(private val repository: StoreReviewRepository) {
    suspend operator fun invoke(merchantId: String): Result<StoreReviewsSummary> = repository.getReviews(merchantId)
}

class CreateStoreReviewUseCase(private val repository: StoreReviewRepository) {
    suspend operator fun invoke(merchantId: String, rating: Int, comment: String): Result<StoreReviewsSummary> {
        val trimmed = comment.trim()
        val error = when {
            rating !in MIN_STAR_RATING..MAX_STAR_RATING -> "Selecciona entre 1 y 5 estrellas."
            trimmed.isEmpty() -> "Escribe tu opinión antes de publicar."
            trimmed.length > MAX_REVIEW_LENGTH -> "La opinión no puede superar $MAX_REVIEW_LENGTH caracteres."
            ProfanityFilter.containsProfanity(trimmed) -> ProfanityFilter.DEFAULT_REJECTION_MESSAGE
            else -> null
        }
        return if (error != null) {
            Result.Failure(AppError.Validation(error))
        } else {
            repository.createReview(merchantId, rating, trimmed)
        }
    }
}

class ToggleStoreReviewLikeUseCase(private val repository: StoreReviewRepository) {
    suspend operator fun invoke(reviewId: String): Result<ReviewLikeStatus> = repository.toggleReviewLike(reviewId)
}

class ReplyStoreReviewUseCase(private val repository: StoreReviewRepository) {
    suspend operator fun invoke(reviewId: String, comment: String): Result<StoreReview> {
        val trimmed = comment.trim()
        val error = when {
            trimmed.isEmpty() -> "Escribe una respuesta antes de enviar."
            trimmed.length > MAX_REPLY_LENGTH -> "La respuesta no puede superar $MAX_REPLY_LENGTH caracteres."
            ProfanityFilter.containsProfanity(trimmed) -> ProfanityFilter.DEFAULT_REJECTION_MESSAGE
            else -> null
        }
        return if (error != null) {
            Result.Failure(AppError.Validation(error))
        } else {
            repository.replyToReview(reviewId, trimmed)
        }
    }
}
