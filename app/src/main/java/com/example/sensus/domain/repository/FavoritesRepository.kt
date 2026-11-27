package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.FavoriteToggleResult
import com.example.sensus.domain.model.ProductOffer
import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favorites: StateFlow<List<FavoriteOffer>>
    fun isFavorite(offerId: String): Boolean
    suspend fun syncFavorites(page: Int = 1, pageSize: Int = 20): Result<List<FavoriteOffer>>
    suspend fun toggleFavorite(offer: ProductOffer): Result<FavoriteToggleResult>
}
