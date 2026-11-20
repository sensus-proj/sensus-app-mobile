package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

private const val DEFAULT_SEARCH_RADIUS_KM = 10.0

data class SearchOffersViewState(
    val query: String = "",
    val results: List<OfferSearchItem> = emptyList(),
    val alternatives: List<OfferAlternativeItem> = emptyList(),
    val history: List<String> = emptyList(),
    val isGuest: Boolean = false,
    val radiusKm: Double = DEFAULT_SEARCH_RADIUS_KM,
    val isLoading: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val hasSearched: Boolean = false,
    val lastCompletedQuery: String = "",
    val referenceOfferId: String? = null,
    val errorMessage: String? = null
) : ViewState

sealed interface SearchOffersViewIntent : ViewIntent {
    data object Initialize : SearchOffersViewIntent
    data class QueryChanged(val query: String) : SearchOffersViewIntent
    data class SubmitSearch(val query: String, val referenceOfferId: String? = null) : SearchOffersViewIntent
    data class SelectHistory(val query: String) : SearchOffersViewIntent
    data object ClearHistory : SearchOffersViewIntent
}

sealed interface SearchOffersViewEffect : ViewEffect
