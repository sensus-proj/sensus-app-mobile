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
import com.example.sensus.data.remote.dto.SetFeedPostLikeRequest
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.FeedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val HTTP_CREATED = 201
private const val HTTP_ACCEPTED = 202
private const val HTTP_NO_CONTENT = 204
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNPROCESSABLE = 422
private const val METERS_PER_KM = 1000.0
private const val MIN_RADIUS_KM = 0.1
private const val MAX_RADIUS_KM = 5.0

class FeedRepositoryImpl(
    private val api: FeedApiService,
    private val dao: FeedPostDao
) : FeedRepository {
    override suspend fun nearby(query: FeedQuery): Result<PagedFeed> = withContext(Dispatchers.IO) {
        val key = "${query.latitude}:${query.longitude}:${query.radiusMeters}:${query.pageSize}"
        val radiusKm = (query.radiusMeters / METERS_PER_KM).coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)
        val result = safeNetworkCall("No se pudo cargar la comunidad.") {
            val response = api.nearby(query.latitude, query.longitude, radiusKm, query.page, query.pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val dtoPosts = body.resolvedPosts()
                savePage(key, query.page, dtoPosts)
                Result.Success(PagedFeed(dtoPosts.map { it.toDomain() }, body.resolvedPage(), body.resolvedHasMore()))
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
            val cached = readPage(key, query.page)
            if (cached.isNotEmpty()) Result.Success(PagedFeed(cached, query.page, hasMore = false, isCached = true)) else result
        } else {
            result
        }
    }

    private fun savePage(key: String, page: Int, posts: List<FeedPostDto>) {
        try {
            val entities = posts.mapIndexed { index, dto ->
                FeedPostEntity(dto.id, key, page, index, Json.encodeToString(dto))
            }
            dao.replacePage(key, page, entities)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The server response remains usable when local storage is unavailable.
        }
    }

    private fun readPage(key: String, page: Int): List<FeedPost> = try {
        dao.getPage(key, page).mapNotNull { entry ->
            try {
                Json.decodeFromString<FeedPostDto>(entry.payload).toDomain()
            } catch (_: SerializationException) {
                null
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun createPost(input: CreatePostInput, media: UploadedMedia?): Result<FeedPost> = safeNetworkCall("No se pudo publicar.") {
        val request = CreateFeedPostRequest(
            text = input.text,
            content = input.text,
            latitude = input.latitude,
            longitude = input.longitude,
            mediaId = media?.mediaId,
            merchantId = input.merchantId
        )
        val response = api.createPost(request)
        val body = response.body()
        when {
            response.code() == HTTP_ACCEPTED || body?.status.equals("Quarantined", ignoreCase = true) ->
                Result.Failure(AppError.Unprocessable("Tu publicación quedó en revisión preventiva antes de hacerse visible."))
            response.isSuccessful && body != null -> Result.Success(body.toDomain())
            else -> Result.Failure(mapCreatePostError(response))
        }
    }

    private fun <T> mapCreatePostError(response: retrofit2.Response<T>): AppError {
        val message = parseBackendErrorMessage(response.errorBody()?.string())
        val isRejection = response.code() == HTTP_BAD_REQUEST || response.code() == HTTP_UNPROCESSABLE
        return if (isRejection) {
            AppError.ContentRejected(message ?: "La publicación no cumple las políticas de la comunidad.")
        } else {
            mapCommonHttpStatus(response.code(), message, "Inicia sesión para publicar.", "Servicio no disponible.")
        }
    }

    override suspend fun toggleLike(postId: String): Result<TogglePostLikeResult> = setPostLike(postId, true)

    override suspend fun setPostLike(postId: String, liked: Boolean): Result<TogglePostLikeResult> = safeNetworkCall("No se pudo actualizar tu me gusta.") {
        val response = api.toggleLike(postId, SetFeedPostLikeRequest(liked))
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
