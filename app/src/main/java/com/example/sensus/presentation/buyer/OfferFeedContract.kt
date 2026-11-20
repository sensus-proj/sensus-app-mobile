package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

private const val DEFAULT_FEED_RADIUS_KM = 10

data class OfferFeedViewState(
    val items: List<OfferFeedItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val snapshotId: String? = null,
    val rankingVersion: String = "",
    val radiusKm: Int = DEFAULT_FEED_RADIUS_KM,
    val hasUserInterests: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOfflineCached: Boolean = false,
    val isGuestBlocked: Boolean = false,
    val errorMessage: String? = null
) : ViewState {
    val canLoadMore: Boolean
        get() = !isLoading && !isLoadingMore && !isOfflineCached && page < totalPages
}

sealed interface OfferFeedViewIntent : ViewIntent {
    data object LoadInitial : OfferFeedViewIntent
    data object Refresh : OfferFeedViewIntent
    data object LoadNextPage : OfferFeedViewIntent
    data class ExpandRadius(val newRadiusKm: Int) : OfferFeedViewIntent
    data object OpenInterestsPicker : OfferFeedViewIntent
}

sealed interface OfferFeedViewEffect : ViewEffect {
    data object RequireAuth : OfferFeedViewEffect
    data object NavigateToInterestsPicker : OfferFeedViewEffect
}
