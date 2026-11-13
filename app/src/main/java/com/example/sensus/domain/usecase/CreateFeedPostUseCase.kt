package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.SessionRepository

class CreateFeedPostUseCase(
    private val repository: FeedRepository,
    private val session: SessionRepository,
    private val media: MediaRepository,
    private val compressor: ImageCompressor
) {
    suspend operator fun invoke(input: CreatePostInput, imageBytes: ByteArray? = null): Result<FeedPost> {
        validate(input)?.let { return Result.Failure(it) }
        val text = input.text.trim()
        val uploaded = when (val result = uploadMediaIfPresent(media, compressor, imageBytes)) {
            is Result.Failure -> return result
            is Result.Success -> result.data
        }
        return repository.createPost(input.copy(text = text), uploaded)
    }
    private fun validate(input: CreatePostInput): AppError? {
        val current = session.sessionState.value
        val text = input.text.trim()
        return when {
            current.isGuest || !current.isSessionActive -> AppError.Unauthorized("Inicia sesión para publicar.")
            text.isBlank() || text.length > FeedPolicy.MAX_TEXT_LENGTH ->
                AppError.Validation("Escribe entre 1 y ${FeedPolicy.MAX_TEXT_LENGTH} caracteres.")
            !FeedPolicy.validCoordinates(input.latitude, input.longitude) -> AppError.Validation("Ubicación inválida.")
            else -> null
        }
    }
}
