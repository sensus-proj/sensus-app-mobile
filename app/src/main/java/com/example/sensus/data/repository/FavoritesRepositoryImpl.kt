package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.FavoriteDao
import com.example.sensus.data.mapper.FavoriteMapper.toDomain
import com.example.sensus.data.mapper.FavoriteMapper.toEntity
import com.example.sensus.data.remote.api.FavoritesApiService
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.FavoriteToggleResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.FavoritesRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

private const val HTTP_OK = 200
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429

class FavoritesRepositoryImpl(
    private val api: FavoritesApiService,
    private val favoriteDao: () -> FavoriteDao? = { null },
    private val sessionRepository: SessionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) : FavoritesRepository {

    private val mutex = Mutex()
    private val _favorites = MutableStateFlow<List<FavoriteOffer>>(emptyList())
    override val favorites: StateFlow<List<FavoriteOffer>> = _favorites.asStateFlow()

    init {
        scope.launch {
            sessionRepository.sessionState
                .map { ownerKeyFor(it) }
                .distinctUntilChanged()
                .collect { ownerKey -> loadFromLocalCache(ownerKey) }
        }
    }

    override fun isFavorite(offerId: String): Boolean = _favorites.value.any { it.offer.id == offerId }

    override suspend fun syncFavorites(page: Int, pageSize: Int): Result<List<FavoriteOffer>> {
        val ownerKey = ownerKeyFor(sessionRepository.sessionState.value) ?: return Result.Failure(AppError.Unauthorized())
        return executeNetworkCall {
            val response = api.getFavorites(page = page, pageSize = pageSize)
            if (ownerKeyFor(sessionRepository.sessionState.value) != ownerKey) {
                Result.Failure(AppError.Unauthorized())
            } else {
                val body = response.body()
                if (response.code() == HTTP_OK && body != null) {
                    val mapped = body.items.map { it.toDomain() }
                    persistFullSnapshot(ownerKey, mapped)
                    Result.Success(mapped)
                } else {
                    mapHttpFailure(response.code())
                }
            }
        }
    }

    override suspend fun toggleFavorite(offer: ProductOffer): Result<FavoriteToggleResult> {
        val ownerKey = ownerKeyFor(sessionRepository.sessionState.value) ?: return Result.Failure(AppError.Unauthorized())
        val previousSnapshot = mutex.withLock {
            val previous = _favorites.value
            val wasFavorite = previous.any { it.offer.id == offer.id }
            _favorites.value = applyOptimisticToggle(previous, offer, !wasFavorite)
            previous
        }
        val result = executeNetworkCall {
            val response = api.toggleFavorite(offer.id)
            if (ownerKeyFor(sessionRepository.sessionState.value) != ownerKey) {
                Result.Failure(AppError.Unauthorized())
            } else {
                val body = response.body()
                if (response.code() == HTTP_OK && body != null) {
                    Result.Success(FavoriteToggleResult(body.offerId, body.favorited, body.favoriteCount))
                } else {
                    mapHttpFailure(response.code())
                }
            }
        }
        return when (result) {
            is Result.Success -> {
                commitToggleOutcome(ownerKey, previousSnapshot, offer, result.data)
                result
            }
            is Result.Failure -> {
                rollbackIfSameOwner(ownerKey, previousSnapshot)
                result
            }
        }
    }

    private suspend fun loadFromLocalCache(ownerKey: String?) {
        val loaded = if (ownerKey == null) {
            emptyList()
        } else {
            withContext(ioDispatcher) {
                runCatching { favoriteDao()?.getByOwner(ownerKey).orEmpty().map { it.toDomain() } }.getOrDefault(emptyList())
            }
        }
        mutex.withLock {
            if (ownerKeyFor(sessionRepository.sessionState.value) == ownerKey) {
                _favorites.value = loaded
            }
        }
    }

    private suspend fun persistFullSnapshot(ownerKey: String, items: List<FavoriteOffer>) {
        val now = System.currentTimeMillis()
        withContext(ioDispatcher) {
            runCatching {
                favoriteDao()?.let { dao ->
                    dao.clearByOwner(ownerKey)
                    dao.upsertAll(items.map { it.toEntity(ownerKey, now) })
                }
            }
        }
        mutex.withLock {
            if (ownerKeyFor(sessionRepository.sessionState.value) == ownerKey) {
                _favorites.value = items
            }
        }
    }

    private suspend fun commitToggleOutcome(ownerKey: String, previousSnapshot: List<FavoriteOffer>, offer: ProductOffer, outcome: FavoriteToggleResult) {
        val now = System.currentTimeMillis()
        val updated = applyOptimisticToggle(previousSnapshot, offer, outcome.favorited, now)
        withContext(ioDispatcher) {
            runCatching {
                favoriteDao()?.let { dao ->
                    if (outcome.favorited) {
                        val entry = updated.firstOrNull { it.offer.id == offer.id }
                        if (entry != null) dao.upsert(entry.toEntity(ownerKey, now))
                    } else {
                        dao.deleteById(ownerKey, offer.id)
                    }
                }
            }
        }
        mutex.withLock {
            if (ownerKeyFor(sessionRepository.sessionState.value) == ownerKey) {
                _favorites.value = updated
            }
        }
    }

    private suspend fun rollbackIfSameOwner(ownerKey: String, previousSnapshot: List<FavoriteOffer>) {
        mutex.withLock {
            if (ownerKeyFor(sessionRepository.sessionState.value) == ownerKey) {
                _favorites.value = previousSnapshot
            }
        }
    }

    private fun applyOptimisticToggle(
        current: List<FavoriteOffer>,
        offer: ProductOffer,
        targetFavorited: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): List<FavoriteOffer> = if (targetFavorited) {
        val existing = current.firstOrNull { it.offer.id == offer.id }
        val updatedEntry = existing?.copy(offer = offer)
            ?: FavoriteOffer(offer = offer, favoritedAtUtc = Instant.ofEpochMilli(nowMillis).toString(), likesCount = offer.likesCount)
        listOf(updatedEntry) + current.filterNot { it.offer.id == offer.id }
    } else {
        current.filterNot { it.offer.id == offer.id }
    }

    private suspend fun <T> executeNetworkCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        Result.Failure(AppError.Unprocessable())
    } catch (http: HttpException) {
        mapHttpFailure(http.code())
    } catch (_: IOException) {
        Result.Failure(AppError.Network())
    }

    private fun <T> mapHttpFailure(code: Int): Result<T> = Result.Failure(
        when (code) {
            HTTP_UNAUTHORIZED -> AppError.Unauthorized()
            HTTP_FORBIDDEN -> AppError.Forbidden()
            HTTP_NOT_FOUND -> AppError.NotFound()
            HTTP_TOO_MANY_REQUESTS -> AppError.Generic("Demasiadas solicitudes de favoritos. Intenta más tarde.")
            else -> AppError.Generic("Error al sincronizar favoritos ($code)")
        }
    )

    private fun ownerKeyFor(session: SessionState): String? {
        if (!session.isSessionActive || session.isGuest || session.activeToken.isNullOrBlank()) return null
        return session.activeEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "authenticated"
    }
}
