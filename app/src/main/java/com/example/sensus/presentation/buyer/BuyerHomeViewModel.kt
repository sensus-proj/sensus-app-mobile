package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

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
                    val safeRadius = GuestRadiusPolicy.clampRadius(intent.radiusKm, isGuest = false)
                    updateState { copy(selectedRadiusKm = safeRadius) }
                    loadOffers()
                }
            }
            is BuyerHomeViewIntent.RefreshLocation -> refreshLocation(forceFresh = true)
            is BuyerHomeViewIntent.RefreshOffers -> loadOffers()
        }
    }

    private fun refreshLocation(forceFresh: Boolean) {
        viewModelScope.launch {
            val permissionGranted = locationRepository.hasLocationPermission()
            val locationEnabled = locationRepository.isLocationEnabled()

            if (permissionGranted && !locationEnabled) {
                updateState { copy(isLocationDisabled = true) }
                return@launch
            } else {
                updateState { copy(isLocationDisabled = false) }
            }

            if (permissionGranted) {
                val loc = if (forceFresh) {
                    locationRepository.getFreshLocation()
                } else {
                    locationRepository.getCurrentLocation()
                }
                updateState { copy(userLocation = loc) }
                loadOffers()
            }
        }
    }

    private fun loadOffers() {
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val loc = viewState.value.userLocation
            val radiusKm = viewState.value.selectedRadiusKm.toDouble()
            val remoteRes = offersRepository.fetchNearbyOffers(
                latitude = loc.latitude,
                longitude = loc.longitude,
                radiusKm = radiusKm
            )
            val cached = offersRepository.getOffers().filter { it.isPubliclyVisible }
            val (offers, errorMsg) = when (remoteRes) {
                is com.example.sensus.common.Result.Success -> {
                    val list = remoteRes.data.filter { it.isPubliclyVisible }.ifEmpty { cached }
                    Pair(list, null)
                }
                is com.example.sensus.common.Result.Failure -> {
                    val err = if (cached.isEmpty()) "Sin conexión" else null
                    Pair(cached, err)
                }
            }
            updateState { copy(offers = offers, isLoading = false, errorMessage = errorMsg) }
        }
    }
}
