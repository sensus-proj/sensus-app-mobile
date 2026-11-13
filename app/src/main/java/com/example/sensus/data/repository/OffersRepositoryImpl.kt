package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.MockDataProvider
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.mapper.RemoteOfferMapper.toDomain
import com.example.sensus.data.mapper.RemoteOfferMapper.toRequest
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.OffersRepository
import retrofit2.Response

private const val HTTP_CREATED = 201
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_UNPROCESSABLE = 422

/**
 * Implementación Offline-First de OffersRepository respaldada por Room + SQLCipher (OfferDao).
 * Estrategia de conflicto para inventario: server-wins (Constitución §5).
 *
 * Nota [API-MIGRATION]: En cuanto se integre el endpoint remoto GET /api/v1/offers/nearby (Spec 007 / FT-11),
 * se debe eliminar obligatoriamente la semilla de respaldo MockDataProvider y sincronizar los datos directamente
 * entre el cliente Retrofit y la base de datos cifrada local Room (OfferDao).
 */
@Suppress("DEPRECATION")
class OffersRepositoryImpl(
    private val offerDao: OfferDao? = null,
    private val offersApi: OffersApiService? = null
) : OffersRepository {

    override fun getOffers(): List<ProductOffer> {
        val dao = offerDao ?: return MockDataProvider.sampleOffers
        return try {
            val cachedEntities = dao.getOffers()
            if (!cachedEntities.isNullOrEmpty()) {
                cachedEntities.map { it.toDomain() }
            } else {
                val initialOffers = MockDataProvider.sampleOffers
                dao.insertOffers(initialOffers.map { it.toEntity() })
                initialOffers
            }
        } catch (_: Exception) {
            MockDataProvider.sampleOffers
        }
    }

    override fun addOffer(offer: ProductOffer) {
        MockDataProvider.addOffer(offer)
        try {
            offerDao?.insertOffer(offer.toEntity())
        } catch (_: Exception) {
            // Degradación segura si el DAO no está disponible en el hilo actual
        }
    }

    /**
     * Estrategia de resolución de conflictos Server-Wins (Constitución §5):
     * El catálogo remoto reemplaza autoritativamente el caché local de ofertas.
     */
    override fun syncOffersFromServer(remoteOffers: List<ProductOffer>) {
        val dao = offerDao ?: return
        try {
            dao.clearOffers()
            dao.insertOffers(remoteOffers.map { it.toEntity() })
        } catch (_: Exception) {
            // Degradación segura ante error de E/S local
        }
    }

    /**
     * FT-15 (RF-15-05/06): publica en `POST /api/v1/offers`; solo con `201` se inserta en `OfferDao`.
     * Sin red o sin cliente remoto se bloquea la publicación: nunca se marca como activa una oferta no creada.
     */
    override suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer> {
        val api = offersApi ?: return Result.Failure(AppError.Network("Sin conexión con el servidor. La oferta no se publicó."))
        return safeNetworkCall("Error al publicar la oferta.") {
            val response = api.createOffer(input.toRequest(media))
            val body = response.body()
            if (response.code() == HTTP_CREATED && body != null) {
                val created = body.toDomain(fallbackExpiresAtMillis = input.expiresAtMillis, fallbackImageUrl = media?.url)
                addOffer(created)
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
            HTTP_FORBIDDEN -> AppError.Forbidden(message ?: "Solo comerciantes pueden publicar ofertas.")
            else -> mapCommonHttpStatus(response.code(), message, default401 = "Sesión requerida.", default404 = "Servicio de ofertas no disponible.")
        }
        return Result.Failure(error)
    }
}
