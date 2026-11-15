package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia

interface OffersRepository {
    suspend fun republishOffer(input: RepublishOfferInput): Result<ProductOffer>

    fun getOffers(): List<ProductOffer>
    fun addOffer(offer: ProductOffer)
    fun syncOffersFromServer(remoteOffers: List<ProductOffer>) = Unit

    /** Consulta las ofertas reales cercanas en `GET /api/v1/offers/nearby` (Spec 007) y sincroniza caché local. */
    suspend fun fetchNearbyOffers(latitude: Double, longitude: Double, radiusKm: Double? = null, category: String? = null): Result<List<ProductOffer>> =
        Result.Success(getOffers())

    /** Consulta las ofertas propias del comerciante en `GET /api/v1/merchants/my-offers` (Spec 037). */
    suspend fun fetchMerchantOffers(status: String? = null): Result<List<ProductOffer>> = Result.Success(getOffers())

    /** Actualiza el stock o estado de una oferta del comerciante (`PUT /api/v1/offers/{id}`). */
    suspend fun updateOfferStock(offer: ProductOffer, newStock: Int): Result<ProductOffer> = Result.Success(offer.copy(unitsLeft = newStock))

    /** Edita los campos de una oferta existente (`PUT /api/v1/offers/{id}`, FT-16 / Spec 035). */
    suspend fun updateOffer(input: UpdateOfferInput): Result<ProductOffer> = Result.Failure(com.example.sensus.common.AppError.Generic("No implementado"))

    /** Cambia el ciclo de vida de una oferta (`PUT /api/v1/offers/{id}/status`, FT-16 / Spec 036). */
    suspend fun changeOfferStatus(offerId: String, newStatus: OfferLifecycleStatus): Result<ProductOffer> =
        Result.Failure(com.example.sensus.common.AppError.Generic("No implementado"))

    /** Publica en `POST /api/v1/offers` y guarda en cache local al recibir 201 (FT-15, RF-15-05). */
    suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer>
}
