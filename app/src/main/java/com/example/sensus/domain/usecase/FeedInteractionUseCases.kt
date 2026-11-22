package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.moderation.ProfanityFilter
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.SessionRepository

class TogglePostLikeUseCase(
    private val repository: FeedRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(postId: String): Result<TogglePostLikeResult> {
        val current = session.sessionState.value
        if (current.isGuest || !current.isSessionActive) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para dar me gusta."))
        }
        val cleanId = postId.trim()
        if (cleanId.isEmpty()) {
            return Result.Failure(AppError.Validation("Publicación inválida."))
        }
        return repository.toggleLike(cleanId)
    }
}

class GetFeedCommentsUseCase(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(postId: String, page: Int = 1, pageSize: Int = FeedPolicy.DEFAULT_PAGE_SIZE): Result<PagedFeedComments> {
        val cleanId = postId.trim()
        if (cleanId.isEmpty()) {
            return Result.Failure(AppError.Validation("Publicación inválida."))
        }
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, FeedPolicy.MAX_PAGE_SIZE)
        return repository.getComments(cleanId, safePage, safePageSize)
    }
}

class AddFeedCommentUseCase(
    private val repository: FeedRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(postId: String, rawText: String): Result<FeedComment> {
        val current = session.sessionState.value
        if (current.isGuest || !current.isSessionActive) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para comentar."))
        }
        val cleanId = postId.trim()
        val text = rawText.trim()
        return when {
            cleanId.isEmpty() -> Result.Failure(AppError.Validation("Publicación inválida."))
            text.isEmpty() -> Result.Failure(AppError.Validation("Escribe un comentario antes de enviar."))
            text.length > FeedPolicy.MAX_COMMENT_LENGTH ->
                Result.Failure(AppError.Validation("El comentario no debe exceder ${FeedPolicy.MAX_COMMENT_LENGTH} caracteres."))
            ProfanityFilter.containsProfanity(text) ->
                Result.Failure(AppError.ContentRejected(ProfanityFilter.DEFAULT_REJECTION_MESSAGE))
            else -> repository.addComment(cleanId, text)
        }
    }
}

class DeleteFeedPostUseCase(
    private val repository: FeedRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> {
        val current = session.sessionState.value
        if (current.isGuest || !current.isSessionActive) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para eliminar tu publicación."))
        }
        val cleanId = postId.trim()
        if (cleanId.isEmpty()) {
            return Result.Failure(AppError.Validation("Publicación inválida."))
        }
        return repository.deletePost(cleanId)
    }
}
