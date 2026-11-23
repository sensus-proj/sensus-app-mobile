package com.example.sensus.data.repository

import android.database.sqlite.SQLiteException
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.SearchHistoryDao
import com.example.sensus.data.local.db.entity.SearchHistoryEntity
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.remote.api.DiscoveryApiService
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.OfferSearchRequest
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.DiscoveryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscoveryRepositoryImpl(
    private val api: DiscoveryApiService,
    private val offerDao: OfferDao? = null,
    private val searchHistoryDao: SearchHistoryDao? = null
) : DiscoveryRepository {

    override suspend fun searchOffers(request: OfferSearchRequest): Result<OfferSearchPage> = withContext(Dispatchers.IO) {
        val networkResult = safeNetworkCall("No se pudo realizar la búsqueda.") {
            val response = api.searchOffers(
                request.query,
                request.latitude,
                request.longitude,
                request.radiusKm,
                request.page
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.toDomain())
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        default401 = "Sesión expirada. Vuelve a iniciar sesión.",
                        default404 = "Servicio de búsqueda no disponible."
                    )
                )
            }
        }

        if (networkResult is Result.Failure && networkResult.error is AppError.Network) {
            val localPage = fallbackLocalSearch(request.query, request.page, request.pageSize)
            if (localPage != null) {
                return@withContext Result.Success(localPage)
            }
        }

        networkResult
    }

    override suspend fun getAlternatives(offerId: String, limit: Int): Result<List<OfferAlternativeItem>> = withContext(Dispatchers.IO) {
        safeNetworkCall("No se pudieron cargar ofertas similares.") {
            val response = api.getAlternatives(offerId, limit)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.map { it.toDomain() })
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        default401 = "Sesión expirada. Vuelve a iniciar sesión.",
                        default404 = "No encontramos alternativas para esta oferta."
                    )
                )
            }
        }
    }

    override suspend fun getSearchHistory(): List<String> = withContext(Dispatchers.IO) {
        val dao = searchHistoryDao ?: return@withContext emptyList()
        try {
            dao.getRecentQueries()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: SQLiteException) {
            emptyList()
        } catch (_: IllegalStateException) {
            emptyList()
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        val dao = searchHistoryDao ?: return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                dao.upsert(SearchHistoryEntity(trimmed, System.currentTimeMillis()))
                dao.trimToFive()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: SQLiteException) {
                // Ignora falla transitoria de caché local sin interrumpir la búsqueda.
            } catch (_: IllegalStateException) {
                // Ignora base cerrada.
            }
        }
    }

    override suspend fun clearSearchHistory() {
        val dao = searchHistoryDao ?: return
        withContext(Dispatchers.IO) {
            try {
                dao.clearAll()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: SQLiteException) {
                // Ignora falla transitoria.
            } catch (_: IllegalStateException) {
                // Ignora base cerrada.
            }
        }
    }

    private fun fallbackLocalSearch(query: String?, page: Int, pageSize: Int): OfferSearchPage? {
        val dao = offerDao ?: return null
        return try {
            val qLower = query?.trim()?.lowercase().orEmpty()
            val localItems = dao.getOffers()
                .map { it.toDomain() }
                .filter { it.isPubliclyVisible && matchesLocalQuery(it, qLower) }
                .map { it.toSearchItem() }

            if (localItems.isEmpty()) {
                null
            } else {
                OfferSearchPage(localItems.take(pageSize), localItems.size, page, pageSize, 1, isFromLocalFallback = true)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: SQLiteException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun matchesLocalQuery(offer: ProductOffer, qLower: String): Boolean {
        if (qLower.isEmpty()) return true
        return offer.name.lowercase().contains(qLower) ||
            offer.description.lowercase().contains(qLower) ||
            offer.category.lowercase().contains(qLower) ||
            offer.storeName.lowercase().contains(qLower)
    }

    private fun ProductOffer.toSearchItem(): OfferSearchItem = OfferSearchItem(
        offerId = id,
        title = name,
        description = description,
        categoryCode = null,
        categoryDisplayName = category,
        imageUrl = imageUrl,
        originalPrice = originalPrice,
        finalPrice = discountPrice,
        stock = unitsLeft,
        distanceMeters = distanceMeters,
        expiresAtUtc = expirationDate,
        merchantId = merchantId,
        merchantName = storeName,
        score = 0.0
    )
}
