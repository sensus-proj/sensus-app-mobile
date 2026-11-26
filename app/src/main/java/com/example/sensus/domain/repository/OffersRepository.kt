package com.example.sensus.domain.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.NearbyOffersQuery
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.model.UserLocation

interface NearbyOffersReader {
    suspend fun fetchNearbyOffersPage(query: NearbyOffersQuery): Result<PagedOffers> = Result.Failure(AppError.Network())
}

interface OffersRepository : NearbyOffersReader {
    suspend fun republishOffer(input: RepublishOfferInput): Result<ProductOffer>

    fun getOffers(): List<ProductOffer>
    suspend fun readCachedOffers(): List<ProductOffer> = getOffers()
    fun addOffer(offer: ProductOffer)

    /** Returns the first nearby page or the offline catalog for existing consumers. */
    suspend fun fetchNearbyOffers(latitude: Double, longitude: Double, radiusKm: Double? = null, category: String? = null): Result<List<ProductOffer>> {
        val query = NearbyOffersQuery(UserLocation(latitude, longitude), radiusKm, category)
        return when (val result = fetchNearbyOffersPage(query)) {
            is Result.Success -> Result.Success(result.data.items)
            is Result.Failure -> Result.Success(readCachedOffers())
        }
    }

    /** Consulta las ofertas propias del comerciante en `GET /api/v1/merchants/my-offers` (Spec 037). */
    suspend fun fetchMerchantOffers(status: String? = null): Result<List<ProductOffer>> = Result.Failure(AppError.Network())

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
