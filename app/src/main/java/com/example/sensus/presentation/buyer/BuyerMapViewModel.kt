package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyerMapViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<BuyerMapViewState, BuyerMapViewIntent, BuyerMapViewEffect>(BuyerMapViewState()) {

    init {
        val offers = offersRepository.getOffers()
        updateState { copy(offers = offers, selectedOffer = offers.firstOrNull()) }
        viewModelScope.launch {
            val hasPerm = sessionRepository.sessionState.value.hasLocationPermission
            updateState { copy(isLocationPermissionGranted = hasPerm) }
        }
    }

    override fun processIntent(intent: BuyerMapViewIntent) {
        when (intent) {
            is BuyerMapViewIntent.OfferSelected -> updateState { copy(selectedOffer = intent.offer) }
            is BuyerMapViewIntent.LocationPermissionGranted -> {
                viewModelScope.launch {
                    sessionRepository.setLocationPermission(intent.granted)
                    updateState { copy(isLocationPermissionGranted = intent.granted) }
                }
            }
        }
    }
}
