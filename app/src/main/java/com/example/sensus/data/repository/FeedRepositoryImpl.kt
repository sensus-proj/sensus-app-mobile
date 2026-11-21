package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.local.db.entity.FeedPostEntity
import com.example.sensus.data.mapper.FeedMapper.toDomain
import com.example.sensus.data.remote.api.FeedApiService
import com.example.sensus.data.remote.dto.CreateFeedCommentRequest
import com.example.sensus.data.remote.dto.CreateFeedPostRequest
import com.example.sensus.data.remote.dto.FeedPostDto
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val HTTP_CREATED = 201
private const val HTTP_NO_CONTENT = 204
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNPROCESSABLE = 422

class FeedRepositoryImpl(
    private val api: FeedApiService,
    private val dao: FeedPostDao
) : FeedRepository {
    override suspend fun nearby(query: FeedQuery): Result<PagedFeed> = withContext(Dispatchers.IO) {
        val key = "${query.latitude}:${query.longitude}:${query.radiusMeters}:${query.pageSize}"
        val result = safeNetworkCall("No se pudo cargar la comunidad.") {
            val response = api.nearby(query.latitude, query.longitude, query.radiusMeters, query.page, query.pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val entities = body.posts.mapIndexed { index, dto ->
                    FeedPostEntity(dto.id, key, query.page, index, Json.encodeToString(dto))
                }
                dao.replacePage(key, query.page, entities)
                Result.Success(PagedFeed(body.posts.map { it.toDomain() }, query.page, body.hasMore))
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        "Inicia sesión para continuar.",
                        "La comunidad no está disponible."
                    )
                )
            }
        }
        if (result is Result.Failure && result.error is AppError.Network) {
            val cached = dao.getPage(key, query.page).map { Json.decodeFromString<FeedPostDto>(it.payload).toDomain() }
            if (cached.isNotEmpty()) Result.Success(PagedFeed(cached, query.page, hasMore = false, isCached = true)) else result
        } else {
            result
        }
    }

    override suspend fun createPost(input: CreatePostInput, media: UploadedMedia?): Result<FeedPost> = safeNetworkCall("No se pudo publicar.") {
        val request = CreateFeedPostRequest(input.text, input.latitude, input.longitude, media?.mediaId, input.merchantId)
        val response = api.createPost(request)
        val body = response.body()
        if (response.code() == HTTP_CREATED && body != null) {
            Result.Success(body.toDomain())
        } else {
            val message = parseBackendErrorMessage(response.errorBody()?.string())
            val error = if (response.code() == HTTP_BAD_REQUEST || response.code() == HTTP_UNPROCESSABLE) {
                AppError.ContentRejected(message ?: "La publicación no cumple las políticas de la comunidad.")
            } else {
                mapCommonHttpStatus(response.code(), message, "Inicia sesión para publicar.", "Servicio no disponible.")
            }
            Result.Failure(error)
        }
    }

    override suspend fun toggleLike(postId: String): Result<TogglePostLikeResult> = safeNetworkCall("No se pudo actualizar tu me gusta.") {
        val response = api.toggleLike(postId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body.toDomain(postId))
        } else {
            Result.Failure(
                mapCommonHttpStatus(
                    response.code(),
                    parseBackendErrorMessage(response.errorBody()?.string()),
                    "Inicia sesión para dar me gusta.",
                    "La publicación no está disponible."
                )
            )
        }
    }

    override suspend fun getComments(postId: String, page: Int, pageSize: Int): Result<PagedFeedComments> =
        safeNetworkCall("No se pudieron cargar los comentarios.") {
            val response = api.getComments(postId, page, pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.toDomain())
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        "Inicia sesión para ver los comentarios.",
                        "La publicación no está disponible."
                    )
                )
            }
        }

    override suspend fun addComment(postId: String, content: String): Result<FeedComment> = safeNetworkCall("No se pudo enviar el comentario.") {
        val response = api.addComment(postId, CreateFeedCommentRequest(content))
        val body = response.body()
        if ((response.code() == HTTP_CREATED || response.isSuccessful) && body != null) {
            Result.Success(body.toDomain())
        } else {
            val message = parseBackendErrorMessage(response.errorBody()?.string())
            val error = if (response.code() == HTTP_BAD_REQUEST || response.code() == HTTP_UNPROCESSABLE) {
                AppError.ContentRejected(message ?: "El comentario infringe las normas comunitarias.")
            } else {
                mapCommonHttpStatus(
                    response.code(),
                    message,
                    "Inicia sesión para comentar.",
                    "La publicación no está disponible."
                )
            }
            Result.Failure(error)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> = safeNetworkCall("No se pudo eliminar la publicación.") {
        val response = api.deletePost(postId)
        if (response.code() == HTTP_NO_CONTENT || response.isSuccessful) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                mapCommonHttpStatus(
                    response.code(),
                    parseBackendErrorMessage(response.errorBody()?.string()),
                    "Inicia sesión para eliminar tu publicación.",
                    "La publicación ya no existe."
                )
            )
        }
    }
}
