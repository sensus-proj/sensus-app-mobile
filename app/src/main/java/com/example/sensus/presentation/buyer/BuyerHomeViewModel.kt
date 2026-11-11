package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyerHomeViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<BuyerHomeViewState, BuyerHomeViewIntent, BuyerHomeViewEffect>(BuyerHomeViewState()) {

    init {
        loadOffers()
        observeSession()
        refreshLocation(forceFresh = false)
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                updateState {
                    copy(
                        isGuest = session.isGuest,
                        selectedRadiusKm = GuestRadiusPolicy.clampRadius(selectedRadiusKm, session.isGuest)
                    )
                }
            }
        }
    }

    override fun processIntent(intent: BuyerHomeViewIntent) {
        when (intent) {
            is BuyerHomeViewIntent.CategorySelected -> updateState { copy(selectedCategory = intent.category) }
            is BuyerHomeViewIntent.SearchQueryChanged -> updateState { copy(searchQuery = intent.query) }
            is BuyerHomeViewIntent.RadiusSelected -> {
                val isGuest = viewState.value.isGuest || sessionRepository.sessionState.value.isGuest
                if (isGuest && !GuestRadiusPolicy.isRadiusAllowedForGuest(intent.radiusKm)) {
                    updateState { copy(selectedRadiusKm = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) }
                    emitEffect(BuyerHomeViewEffect.RequireAuthForRadius)
                } else {
                    updateState { copy(selectedRadiusKm = GuestRadiusPolicy.clampRadius(intent.radiusKm, isGuest = false)) }
                }
            }
            is BuyerHomeViewIntent.RefreshLocation -> refreshLocation(forceFresh = true)
            is BuyerHomeViewIntent.RefreshOffers -> loadOffers()
        }
    }

    private fun refreshLocation(forceFresh: Boolean) {
        viewModelScope.launch {
            if (locationRepository.hasLocationPermission()) {
                val loc = if (forceFresh) {
                    locationRepository.getFreshLocation()
                } else {
                    locationRepository.getCurrentLocation()
                }
                updateState { copy(userLocation = loc) }
            }
        }
    }

    private fun loadOffers() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val offers = offersRepository.getOffers()
            updateState { copy(offers = offers, isLoading = false) }
        }
    }
}
