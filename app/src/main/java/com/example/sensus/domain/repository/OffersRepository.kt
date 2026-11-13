package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UploadedMedia

interface OffersRepository {
    fun getOffers(): List<ProductOffer>
    fun addOffer(offer: ProductOffer)
    fun syncOffersFromServer(remoteOffers: List<ProductOffer>) = Unit

    /** Publica en `POST /api/v1/offers` y guarda en cache local al recibir 201 (FT-15, RF-15-05). */
    suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer>
}
