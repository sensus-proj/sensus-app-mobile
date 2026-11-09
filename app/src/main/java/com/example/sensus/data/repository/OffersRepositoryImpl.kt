package com.example.sensus.data.repository

import com.example.sensus.data.MockDataProvider
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository

/**
 * Implementación de OffersRepository.
 * TODO: [API-MIGRATION] En cuanto se integre el endpoint remoto GET /api/v1/offers/nearby (Spec 007 / FT-07),
 * se debe eliminar obligatoriamente la dependencia de MockDataProvider y sincronizar los datos directamente
 * entre el cliente Retrofit y la base de datos cifrada local Room (OfferDao).
 */
class OffersRepositoryImpl : OffersRepository {
    override fun getOffers(): List<ProductOffer> {
        return MockDataProvider.sampleOffers
    }

    override fun addOffer(offer: ProductOffer) {
        MockDataProvider.addOffer(offer)
    }
}
