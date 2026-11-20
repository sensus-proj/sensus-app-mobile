package com.example.sensus.data.repository

import android.database.sqlite.SQLiteException
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.FeedOfferDao
import com.example.sensus.data.local.db.entity.FeedOfferEntity
import com.example.sensus.data.remote.api.OfferFeedApiService
import com.example.sensus.data.remote.dto.OfferFeedItemDto
import com.example.sensus.data.remote.dto.OfferFeedPageDto
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.repository.OfferFeedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_PAGE_SIZE = 20

class OfferFeedRepositoryImpl(
    private val api: OfferFeedApiService,
    private val dao: FeedOfferDao? = null
) : OfferFeedRepository {

    private val serializer = Json { ignoreUnknownKeys = true }

    override suspend fun getFeed(request: OfferFeedRequest): Result<OfferFeedPage> = withContext(Dispatchers.IO) {
        val networkResult = safeNetworkCall("No se pudieron cargar las promociones.") {
            val response = api.getOfferFeed(
                request.latitude,
                request.longitude,
                request.radiusKm,
                request.page,
                request.snapshotId
            )
            val body: OfferFeedPageDto? = response.body()
            if (response.isSuccessful && body != null) {
                if (request.page == 1) {
                    persistFirstPageSafely(body)
                }
                Result.Success(body.toDomain(isCached = false))
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        default401 = "Inicia sesión para ver promociones personalizadas.",
                        default404 = "El feed de promociones no está disponible."
                    )
                )
            }
        }

        if (networkResult is Result.Failure && networkResult.error is AppError.Network && request.page == 1) {
            val cached = readCachedFirstPageSafely()
            if (cached != null && cached.items.isNotEmpty()) {
                return@withContext Result.Success(cached)
            }
        }

        networkResult
    }

    override suspend fun getCachedFirstPage(): OfferFeedPage? = withContext(Dispatchers.IO) {
        readCachedFirstPageSafely()
    }

    private fun persistFirstPageSafely(pageDto: OfferFeedPageDto) {
        val targetDao = dao ?: return
        val now = System.currentTimeMillis()
        try {
            val entities = pageDto.items.mapIndexed { idx, item ->
                FeedOfferEntity(
                    item.offerId,
                    idx,
                    serializer.encodeToString(item),
                    pageDto.rankingVersion,
                    now
                )
            }
            targetDao.replaceFirstPage(entities)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: SQLiteException) {
            // La caché cifrada es accesoria (docs/errores-conocidos.md E-02): nunca rompe el flujo.
        } catch (_: IllegalStateException) {
            // Ignora estado cerrado de Room sin interrumpir el feed.
        } catch (_: SerializationException) {
            // Ignora error de serialización en caché accesoria.
        }
    }

    private fun readCachedFirstPageSafely(): OfferFeedPage? {
        val targetDao = dao ?: return null
        return try {
            val rows = targetDao.getAllOrdered()
            if (rows.isEmpty()) return null
            val items = rows.map { serializer.decodeFromString<OfferFeedItemDto>(it.payloadJson).toDomain() }
            val version = rows.firstOrNull()?.rankingVersion ?: "feed-v1"
            OfferFeedPage(
                items = items,
                page = 1,
                pageSize = DEFAULT_PAGE_SIZE,
                totalCount = items.size,
                totalPages = 1,
                snapshotId = null,
                rankingVersion = version,
                isCached = true
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: SQLiteException) {
            null
        } catch (_: IllegalStateException) {
            null
        } catch (_: SerializationException) {
            null
        }
    }
}
