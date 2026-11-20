package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.GetOfferFeedUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

private const val DEFAULT_LAT = 19.432608
private const val DEFAULT_LNG = -99.133209

@HiltViewModel
class OfferFeedViewModel @Inject constructor(
    private val getOfferFeedUseCase: GetOfferFeedUseCase,
    private val getMyInterestsUseCase: GetMyInterestsUseCase,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<OfferFeedViewState, OfferFeedViewIntent, OfferFeedViewEffect>(OfferFeedViewState()) {

    override fun processIntent(intent: OfferFeedViewIntent) {
        when (intent) {
            OfferFeedViewIntent.LoadInitial -> loadFirstPage(isRefresh = false)
            OfferFeedViewIntent.Refresh -> loadFirstPage(isRefresh = true)
            OfferFeedViewIntent.LoadNextPage -> loadNextPage()
            is OfferFeedViewIntent.ExpandRadius -> {
                updateState { copy(radiusKm = intent.newRadiusKm) }
                loadFirstPage(isRefresh = true)
            }
            OfferFeedViewIntent.OpenInterestsPicker -> emitEffect(OfferFeedViewEffect.NavigateToInterestsPicker)
        }
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        if (sessionRepository.sessionState.value.isGuest) {
            updateState { copy(isGuestBlocked = true, isLoading = false, isRefreshing = false, errorMessage = null) }
            emitEffect(OfferFeedViewEffect.RequireAuth)
            return
        }

        updateState {
            copy(
                isGuestBlocked = false,
                isLoading = !isRefresh && items.isEmpty(),
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val (lat, lng) = resolveCoordinates()
            val interestsRes = getMyInterestsUseCase()
            val hasInterests = interestsRes is Result.Success && interestsRes.data.isNotEmpty()

            val request = OfferFeedRequest(
                latitude = lat,
                longitude = lng,
                radiusKm = viewState.value.radiusKm,
                page = 1,
                forceRefresh = true
            )
            when (val res = getOfferFeedUseCase(request)) {
                is Result.Success -> applyFirstPageSuccess(res.data, hasInterests)
                is Result.Failure -> handleLoadFailure(res.error, isFirstPage = true)
            }
        }
    }

    private fun loadNextPage() {
        val current = viewState.value
        if (!current.canLoadMore || current.isGuestBlocked) return

        updateState { copy(isLoadingMore = true, errorMessage = null) }
        viewModelScope.launch {
            val (lat, lng) = resolveCoordinates()
            val nextPage = current.page + 1
            val request = OfferFeedRequest(
                latitude = lat,
                longitude = lng,
                radiusKm = current.radiusKm,
                page = nextPage,
                forceRefresh = false
            )
            when (val res = getOfferFeedUseCase(request)) {
                is Result.Success -> {
                    val data = res.data
                    updateState {
                        copy(
                            items = items + data.items,
                            page = data.page,
                            totalPages = data.totalPages,
                            snapshotId = data.snapshotId,
                            rankingVersion = data.rankingVersion,
                            isLoadingMore = false,
                            isOfflineCached = data.isCached
                        )
                    }
                }
                is Result.Failure -> handleLoadFailure(res.error, isFirstPage = false)
            }
        }
    }

    private fun applyFirstPageSuccess(data: OfferFeedPage, hasInterests: Boolean) {
        updateState {
            copy(
                items = data.items,
                page = data.page,
                totalPages = data.totalPages,
                snapshotId = data.snapshotId,
                rankingVersion = data.rankingVersion,
                hasUserInterests = hasInterests,
                isLoading = false,
                isRefreshing = false,
                isOfflineCached = data.isCached,
                isGuestBlocked = false,
                errorMessage = null
            )
        }
    }

    private fun handleLoadFailure(error: AppError, isFirstPage: Boolean) {
        if (error is AppError.Unauthorized || error is AppError.Forbidden) {
            updateState {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isGuestBlocked = true
                )
            }
            emitEffect(OfferFeedViewEffect.RequireAuth)
            return
        }

        updateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = if (isFirstPage || items.isNotEmpty()) error.message else null
            )
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
