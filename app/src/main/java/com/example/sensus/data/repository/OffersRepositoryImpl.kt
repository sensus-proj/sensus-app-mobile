package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.mapper.RemoteOfferMapper.formatIsoUtc
import com.example.sensus.data.mapper.RemoteOfferMapper.toDomain
import com.example.sensus.data.mapper.RemoteOfferMapper.toRequest
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.data.remote.api.RepublishOfferRequestDto
import com.example.sensus.data.remote.dto.ChangeOfferStatusRequestDto
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.NearbyOffersQuery
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.OffersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

private const val HTTP_OK = 200
private const val HTTP_CREATED = 201
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val MERCHANT_INVENTORY_PAGE_SIZE = 100
private const val MAX_MERCHANT_PAGES = 20
private const val HTTP_UNPROCESSABLE = 422
private const val DEFAULT_UPDATE_DURATION_MS = 86_400_000L
private const val MIN_BACKEND_DESC_LEN = 5
private val LEGACY_FAKE_OFFER_IDS = setOf("off_1", "off_2", "off_3", "off_4", "off_5", "off_6", "off_7", "off_8")

/**
 * Implementación Offline-First de OffersRepository conectada a `GET /api/v1/offers/nearby`,
 * `GET /api/v1/merchants/my-offers`, `POST /api/v1/offers` y respaldada por Room + SQLCipher (OfferDao).
 * Estrategia de conflicto para inventario: server-wins (Constitución §5).
 */
class OffersRepositoryImpl(
    private val offerDao: OfferDao? = null,
    private val offersApi: OffersApiService? = null
) : OffersRepository {

    override suspend fun readCachedOffers(): List<ProductOffer> = withContext(Dispatchers.IO) { getOffers() }

    override fun getOffers(): List<ProductOffer> {
        val dao = offerDao ?: return emptyList()
        return try {
            val cachedEntities = dao.getOffers().orEmpty()
            val realEntities = cachedEntities.filterNot { it.id in LEGACY_FAKE_OFFER_IDS }
            if (realEntities.size != cachedEntities.size) {
                dao.clearOffers()
                if (realEntities.isNotEmpty()) dao.insertOffers(realEntities)
            }
            realEntities.map { it.toDomain() }.filter { it.isPubliclyVisible }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun addOffer(offer: ProductOffer) {
        try {
            offerDao?.insertOffer(offer.toEntity())
        } catch (_: Exception) {
            // Degradación segura si el DAO no está disponible en el hilo actual
        }
    }

    internal fun syncOffersFromServer(remoteOffers: List<ProductOffer>) {
        val dao = offerDao ?: return
        try {
            dao.clearOffers()
            if (remoteOffers.isNotEmpty()) {
                dao.insertOffers(remoteOffers.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Degradación segura ante error de E/S local
        }
    }

    override suspend fun fetchNearbyOffersPage(query: NearbyOffersQuery): Result<PagedOffers> {
        val api = offersApi ?: return Result.Failure(AppError.Network())
        return safeNetworkCall("Error al cargar ofertas cercanas.") {
            val parameters = buildMap {
                put("lat", query.location.latitude.toString())
                put("lng", query.location.longitude.toString())
                put("page", query.page.toString())
                put("pageSize", query.pageSize.toString())
                query.radiusKm?.let { put("radiusKm", it.toString()) }
                query.category?.let { put("category", it) }
            }
            val response = api.getNearbyOffers(parameters)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val offers = body.items.map { it.toDomain() }.filter { it.isPubliclyVisible }
                withContext(Dispatchers.IO) {
                    if (query.page == 1) {
                        syncOffersFromServer(offers)
                    } else {
                        runCatching { offerDao?.insertOffers(offers.map { it.toEntity() }) }
                    }
                }
                val hasMore = body.resolvedPage() == query.page && body.items.isNotEmpty() &&
                    query.page.toLong() * query.pageSize < body.totalCount
                Result.Success(PagedOffers(offers, (query.page + 1).takeIf { hasMore }))
            } else {
                mapCreateOfferError(response)
            }
        }
    }

    override suspend fun fetchMerchantOffers(status: String?): Result<List<ProductOffer>> {
        val api = offersApi ?: return Result.Failure(AppError.Network("No se pudo consultar el inventario del comercio."))
        return safeNetworkCall("Error al cargar tus ofertas.") {
            val collected = mutableListOf<ProductOffer>()
            var page = 1
            while (page <= MAX_MERCHANT_PAGES) {
                val response = api.getMyMerchantOffers(status = status, page = page, pageSize = MERCHANT_INVENTORY_PAGE_SIZE)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    return@safeNetworkCall mapCreateOfferError(response)
                }
                collected.addAll(body.items.map { it.toDomain() })
                if (body.items.isEmpty() || collected.size >= body.totalCount) break
                page++
            }
            Result.Success(collected)
        }
    }

    override suspend fun updateOfferStock(offer: ProductOffer, newStock: Int): Result<ProductOffer> {
        return if (newStock <= 0) {
            changeOfferStatus(offer.id, OfferLifecycleStatus.PAUSED).let { res ->
                if (res is Result.Success) Result.Success(res.data.copy(unitsLeft = 0, status = OfferLifecycleStatus.PAUSED)) else res
            }
        } else {
            val expiresAt = offer.expiresAtMillis?.takeIf { it > System.currentTimeMillis() }
                ?: (System.currentTimeMillis() + DEFAULT_UPDATE_DURATION_MS)
            val safeDesc = offer.description.trim().takeIf { it.length >= MIN_BACKEND_DESC_LEN }
                ?: "Producto disponible en promoción: ${offer.name}"
            updateOffer(
                UpdateOfferInput(
                    offerId = offer.id,
                    title = offer.name,
                    description = safeDesc,
                    originalPrice = offer.originalPrice,
                    discountPrice = offer.discountPrice,
                    stock = newStock,
                    expiresAtMillis = expiresAt,
                    imageUrl = offer.imageUrl,
                    expectedVersion = offer.version
                )
            )
        }
    }

    override suspend fun updateOffer(input: UpdateOfferInput): Result<ProductOffer> {
        val api = offersApi ?: return Result.Failure(AppError.Network("Sin conexión con el servidor."))
        return safeNetworkCall("Error al actualizar la oferta.") {
            val response = api.updateOffer(input.offerId, input.toRequest())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val updated = body.toDomain(fallbackExpiresAtMillis = input.expiresAtMillis, fallbackImageUrl = input.imageUrl)
                withContext(Dispatchers.IO) { addOffer(updated) }
                Result.Success(updated)
            } else {
                mapCreateOfferError(response)
            }
        }
    }

    override suspend fun changeOfferStatus(offerId: String, newStatus: OfferLifecycleStatus): Result<ProductOffer> {
        val api = offersApi ?: return Result.Failure(AppError.Network("Sin conexión con el servidor."))
        return safeNetworkCall("Error al actualizar el estado de la oferta.") {
            val response = api.changeOfferStatus(offerId, ChangeOfferStatusRequestDto(newStatus.backendValue))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val updated = body.toDomain()
                withContext(Dispatchers.IO) { addOffer(updated) }
                Result.Success(updated)
            } else {
                mapCreateOfferError(response)
            }
        }
    }

    override suspend fun republishOffer(input: RepublishOfferInput): Result<ProductOffer> {
        val api = offersApi ?: return Result.Failure(AppError.Network())
        return safeNetworkCall("Error al republicar la oferta.") {
            val request = RepublishOfferRequestDto(
                formatIsoUtc(input.newStartsAtMillis),
                formatIsoUtc(input.newEndsAtMillis),
                input.newStock
            )
            val response = api.republishOffer(input.offerId, request)
            val body = response.body()
            if (response.code() == HTTP_OK && body != null) {
                val updated = body.toDomain()
                withContext(Dispatchers.IO) { addOffer(updated) }
                Result.Success(updated)
            } else {
                mapCreateOfferError(response)
            }
        }
    }

    override suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer> {
        val api = offersApi ?: return Result.Failure(AppError.Network("Sin conexión con el servidor. La oferta no se publicó."))
        return safeNetworkCall("Error al publicar la oferta.") {
            val response = api.createOffer(input.toRequest(media))
            val body = response.body()
            if (response.code() == HTTP_CREATED && body != null) {
                val created = body.toDomain(fallbackExpiresAtMillis = input.expiresAtMillis, fallbackImageUrl = media?.url)
                    .copy(category = input.category.displayName)
                withContext(Dispatchers.IO) { addOffer(created) }
                Result.Success(created)
            } else {
                mapCreateOfferError(response)
            }
        }
    }

    private fun <T> mapCreateOfferError(response: Response<T>): Result<Nothing> {
        val message = parseBackendErrorMessage(response.errorBody()?.string())
        val error = when (response.code()) {
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE ->
                AppError.ContentRejected(message ?: "La oferta fue rechazada por las políticas de contenido.")
            HTTP_FORBIDDEN -> AppError.Forbidden(message ?: "Solo comerciantes pueden gestionar ofertas.")
            else -> mapCommonHttpStatus(response.code(), message, default401 = "Sesión requerida.", default404 = "Servicio de ofertas no disponible.")
        }
        return Result.Failure(error)
    }
}
