package com.example.sensus.domain.repository

import com.example.sensus.domain.model.ProductOffer
import kotlinx.coroutines.flow.StateFlow

interface SavedOffersRepository {
    val savedOffers: StateFlow<List<ProductOffer>>
    fun isOfferSaved(offerId: String): Boolean
    suspend fun toggleSavedOffer(offer: ProductOffer): Boolean
    suspend fun removeSavedOffer(offerId: String)
}
