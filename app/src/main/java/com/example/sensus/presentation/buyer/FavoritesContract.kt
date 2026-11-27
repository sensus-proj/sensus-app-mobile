package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class FavoritesViewState(
    val favorites: List<FavoriteOffer> = emptyList(),
    val filter: FavoriteFilter = FavoriteFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUsingOfflineCache: Boolean = false,
    val errorMessage: String? = null,
    val pendingOfferIds: Set<String> = emptySet()
) : ViewState {
    val favoriteOfferIds: Set<String>
        get() = favorites.mapTo(mutableSetOf()) { it.offer.id }

    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && favorites.isEmpty()
}

sealed interface FavoritesViewIntent : ViewIntent {
    data object Load : FavoritesViewIntent
    data object Refresh : FavoritesViewIntent
    data class SelectFilter(val filter: FavoriteFilter) : FavoritesViewIntent
    data class ToggleFavorite(val offer: ProductOffer) : FavoritesViewIntent
}

sealed interface FavoritesViewEffect : ViewEffect {
    data object RequireAuth : FavoritesViewEffect
    data class ShowMessage(val message: String) : FavoritesViewEffect
}
