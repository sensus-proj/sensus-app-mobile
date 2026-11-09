package com.example.sensus.domain.repository

import com.example.sensus.domain.model.ProductOffer

interface OffersRepository {
    fun getOffers(): List<ProductOffer>
    fun addOffer(offer: ProductOffer)
    fun syncOffersFromServer(remoteOffers: List<ProductOffer>) = Unit
}
