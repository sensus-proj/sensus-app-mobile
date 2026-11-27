package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetFavoritesUseCase
import com.example.sensus.domain.usecase.ObserveFavoritesUseCase
import com.example.sensus.domain.usecase.ToggleFavoriteUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val sessionRepository: SessionRepository
) : MviViewModel<FavoritesViewState, FavoritesViewIntent, FavoritesViewEffect>(FavoritesViewState()) {

    private val filterFlow = MutableStateFlow(FavoriteFilter.ALL)

    init {
        viewModelScope.launch {
            filterFlow
                .flatMapLatest { filter -> observeFavoritesUseCase(filter) }
                .collect { items -> updateState { copy(favorites = items) } }
        }
    }

    override fun processIntent(intent: FavoritesViewIntent) {
        when (intent) {
            FavoritesViewIntent.Load -> sync(isRefresh = false)
            FavoritesViewIntent.Refresh -> sync(isRefresh = true)
            is FavoritesViewIntent.SelectFilter -> selectFilter(intent.filter)
            is FavoritesViewIntent.ToggleFavorite -> toggleFavorite(intent.offer)
        }
    }

    private fun selectFilter(filter: FavoriteFilter) {
        filterFlow.value = filter
        updateState { copy(filter = filter) }
    }

    private fun sync(isRefresh: Boolean) {
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive || session.activeToken.isNullOrBlank()) {
            updateState { copy(isLoading = false, isRefreshing = false, isUsingOfflineCache = false) }
            return
        }
        if (viewState.value.isLoading || viewState.value.isRefreshing) return
        updateState {
            copy(
                isLoading = !isRefresh && favorites.isEmpty(),
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            when (val res = getFavoritesUseCase()) {
                is Result.Success -> updateState {
                    copy(isLoading = false, isRefreshing = false, isUsingOfflineCache = false, errorMessage = null)
                }
                is Result.Failure -> handleSyncFailure(res.error)
            }
        }
    }

    private fun handleSyncFailure(error: AppError) {
        if (error is AppError.Forbidden || error is AppError.Unauthorized) {
            updateState { copy(isLoading = false, isRefreshing = false, isUsingOfflineCache = false) }
            emitEffect(FavoritesViewEffect.RequireAuth)
            return
        }
        val hasCached = viewState.value.favorites.isNotEmpty()
        updateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isUsingOfflineCache = hasCached,
                errorMessage = if (hasCached || error is AppError.NotFound) null else error.message
            )
        }
    }

    private fun toggleFavorite(offer: ProductOffer) {
        if (sessionRepository.sessionState.value.isGuest) {
            emitEffect(FavoritesViewEffect.RequireAuth)
            return
        }
        if (offer.id in viewState.value.pendingOfferIds) return
        updateState { copy(pendingOfferIds = pendingOfferIds + offer.id) }
        viewModelScope.launch {
            when (val res = toggleFavoriteUseCase(offer)) {
                is Result.Success -> {
                    val msg = if (res.data.favorited) "Agregado a favoritos" else "Quitado de favoritos"
                    emitEffect(FavoritesViewEffect.ShowMessage(msg))
                }
                is Result.Failure -> {
                    if (res.error is AppError.Forbidden || res.error is AppError.Unauthorized) {
                        emitEffect(FavoritesViewEffect.RequireAuth)
                    } else {
                        emitEffect(FavoritesViewEffect.ShowMessage(res.error.message))
                    }
                }
            }
            updateState { copy(pendingOfferIds = pendingOfferIds - offer.id) }
        }
    }
}
