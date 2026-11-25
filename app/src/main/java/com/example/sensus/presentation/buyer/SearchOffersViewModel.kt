package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchRequest
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.SearchOffersUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L
private const val MIN_LIVE_SEARCH_CHARS = 2
private const val DEFAULT_LAT = 19.432608
private const val DEFAULT_LNG = -99.133209
private const val DEFAULT_ALTERNATIVES_LIMIT = 5
private const val MAX_REGISTERED_RADIUS_KM = 50.0

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchOffersViewModel @Inject constructor(
    private val searchOffersUseCase: SearchOffersUseCase,
    private val getOfferAlternativesUseCase: GetOfferAlternativesUseCase,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<SearchOffersViewState, SearchOffersViewIntent, SearchOffersViewEffect>(SearchOffersViewState()) {

    private val queryTrigger = MutableStateFlow("")

    init {
        observeSession()
        observeDebouncedSearch()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                val maxRadius = if (session.isGuest) {
                    GuestRadiusPolicy.GUEST_MAX_RADIUS_KM.toDouble()
                } else {
                    MAX_REGISTERED_RADIUS_KM
                }
                updateState {
                    copy(
                        isGuest = session.isGuest,
                        radiusKm = radiusKm.coerceIn(1.0, maxRadius),
                        history = if (session.isGuest) emptyList() else history
                    )
                }
            }
        }
    }

    private fun observeDebouncedSearch() {
        viewModelScope.launch {
            queryTrigger
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    flow {
                        val trimmed = q.trim()
                        if (trimmed.length >= MIN_LIVE_SEARCH_CHARS) {
                            executeSearch(trimmed, referenceOfferId = viewState.value.referenceOfferId)
                        } else if (trimmed.isEmpty() && viewState.value.hasSearched) {
                            executeSearch("", referenceOfferId = null)
                        }
                        emit(Unit)
                    }
                }
                .collect {}
        }
    }

    override fun processIntent(intent: SearchOffersViewIntent) {
        when (intent) {
            SearchOffersViewIntent.Initialize -> loadHistory()
            is SearchOffersViewIntent.QueryChanged -> {
                updateState { copy(query = intent.query, errorMessage = null) }
                queryTrigger.value = intent.query
            }
            is SearchOffersViewIntent.SubmitSearch -> {
                updateState { copy(query = intent.query, referenceOfferId = intent.referenceOfferId, errorMessage = null) }
                viewModelScope.launch {
                    executeSearch(intent.query.trim(), intent.referenceOfferId)
                }
            }
            is SearchOffersViewIntent.SelectHistory -> {
                updateState { copy(query = intent.query, errorMessage = null) }
                viewModelScope.launch {
                    executeSearch(intent.query.trim(), viewState.value.referenceOfferId)
                }
            }
            SearchOffersViewIntent.ClearHistory -> {
                viewModelScope.launch {
                    searchOffersUseCase.clearHistory()
                    updateState { copy(history = emptyList()) }
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val isGuest = sessionRepository.sessionState.value.isGuest
            val items = if (isGuest) emptyList() else searchOffersUseCase.getHistory()
            updateState { copy(isGuest = isGuest, history = items) }
        }
    }

    private suspend fun executeSearch(trimmedQuery: String, referenceOfferId: String?) {
        if (trimmedQuery.length == 1) {
            updateState { copy(isLoading = false, errorMessage = "Escribe al menos 2 caracteres para buscar.") }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null) }
        val (lat, lng) = resolveCoordinates()
        val res = searchOffersUseCase(
            OfferSearchRequest(
                query = trimmedQuery.ifEmpty { null },
                latitude = lat,
                longitude = lng,
                radiusKm = viewState.value.radiusKm
            )
        )

        when (res) {
            is Result.Success -> {
                val updatedHistory = if (viewState.value.isGuest || trimmedQuery.isEmpty()) {
                    viewState.value.history
                } else {
                    searchOffersUseCase.getHistory()
                }
                val alts = resolveAlternativesIfEmpty(res.data.items.isEmpty(), referenceOfferId, trimmedQuery, lat, lng)
                updateState {
                    copy(
                        results = res.data.items,
                        alternatives = alts,
                        history = updatedHistory,
                        isLoading = false,
                        isOfflineFallback = res.data.isFromLocalFallback,
                        hasSearched = true,
                        lastCompletedQuery = trimmedQuery,
                        errorMessage = null
                    )
                }
            }
            is Result.Failure -> {
                updateState { copy(isLoading = false, errorMessage = res.error.message, hasSearched = true) }
            }
        }
    }

    private suspend fun resolveAlternativesIfEmpty(
        isEmptyResults: Boolean,
        referenceOfferId: String?,
        trimmedQuery: String,
        lat: Double,
        lng: Double
    ): List<OfferAlternativeItem> {
        if (!isEmptyResults || trimmedQuery.isEmpty()) return emptyList()
        if (referenceOfferId.isNullOrBlank()) return nearbyAlternatives(lat, lng)
        return when (val altRes = getOfferAlternativesUseCase(referenceOfferId, DEFAULT_ALTERNATIVES_LIMIT)) {
            is Result.Success -> altRes.data
            is Result.Failure -> emptyList()
        }
    }

    private suspend fun nearbyAlternatives(lat: Double, lng: Double): List<OfferAlternativeItem> {
        val fallback = searchOffersUseCase(
            OfferSearchRequest(
                query = null,
                latitude = lat,
                longitude = lng,
                radiusKm = viewState.value.radiusKm,
                pageSize = DEFAULT_ALTERNATIVES_LIMIT
            )
        )
        return when (fallback) {
            is Result.Success -> fallback.data.items.map { item ->
                OfferAlternativeItem(
                    offerId = item.offerId,
                    title = item.title,
                    categoryCode = item.categoryCode.orEmpty(),
                    categoryDisplayName = item.categoryDisplayName,
                    imageUrl = item.imageUrl,
                    originalPrice = item.originalPrice,
                    finalPrice = item.finalPrice,
                    stock = item.stock,
                    distanceMeters = item.distanceMeters,
                    expiresAtUtc = item.expiresAtUtc,
                    merchantId = item.merchantId,
                    merchantName = item.merchantName
                )
            }
            is Result.Failure -> emptyList()
        }
    }

    private suspend fun resolveCoordinates(): Pair<Double, Double> {
        return if (locationRepository.hasLocationPermission()) {
            val loc = locationRepository.getCurrentLocation()
            loc.latitude to loc.longitude
        } else {
            DEFAULT_LAT to DEFAULT_LNG
        }
    }
}
