package com.example.sensus.data.repository

import com.example.sensus.data.MockDataProvider
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository

class OffersRepositoryImpl : OffersRepository {
    override fun getOffers(): List<ProductOffer> {
        return MockDataProvider.sampleOffers
    }

    override fun addOffer(offer: ProductOffer) {
        MockDataProvider.addOffer(offer)
    }
}
