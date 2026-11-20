package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.OfferSearchRequest

interface DiscoveryRepository {
    suspend fun searchOffers(request: OfferSearchRequest): Result<OfferSearchPage>

    suspend fun getAlternatives(offerId: String, limit: Int): Result<List<OfferAlternativeItem>>

    suspend fun getSearchHistory(): List<String> = emptyList()
    suspend fun saveSearchQuery(query: String) {}
    suspend fun clearSearchHistory() {}
}
