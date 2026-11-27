package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository
) {
    operator fun invoke(filter: FavoriteFilter, nowEpochMillisProvider: () -> Long = System::currentTimeMillis): Flow<List<FavoriteOffer>> =
        repository.favorites.map { items ->
            when (filter) {
                FavoriteFilter.ALL -> items
                FavoriteFilter.AVAILABLE_ONLY -> {
                    val nowMillis = nowEpochMillisProvider()
                    items.filter { item ->
                        val expiresAt = item.offer.expiresAtMillis
                        item.offer.unitsLeft > 0 && (expiresAt == null || expiresAt > nowMillis)
                    }
                }
            }
        }
}
