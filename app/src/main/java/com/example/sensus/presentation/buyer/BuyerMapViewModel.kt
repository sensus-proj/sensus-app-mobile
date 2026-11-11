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
class BuyerMapViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<BuyerMapViewState, BuyerMapViewIntent, BuyerMapViewEffect>(BuyerMapViewState()) {

    init {
        val offers = offersRepository.getOffers()
        updateState { copy(offers = offers, selectedOffer = null) }

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

        viewModelScope.launch {
            val osPermission = locationRepository.hasLocationPermission()
            val gpsEnabled = locationRepository.isLocationEnabled()
            val effectivePermission = osPermission || sessionRepository.sessionState.value.hasLocationPermission
            updateState {
                copy(
                    isLocationPermissionGranted = effectivePermission,
                    isLocationEnabled = gpsEnabled
                )
            }
            if (osPermission) {
                sessionRepository.setLocationPermission(true)
                fetchLocation(forceFresh = false)
            }
        }
    }

    override fun processIntent(intent: BuyerMapViewIntent) {
        when (intent) {
            is BuyerMapViewIntent.OfferSelected -> updateState { copy(selectedOffer = intent.offer) }
            is BuyerMapViewIntent.LocationPermissionGranted -> {
                viewModelScope.launch {
                    sessionRepository.setLocationPermission(intent.granted)
                    val gpsEnabled = locationRepository.isLocationEnabled()
                    updateState {
                        copy(
                            isLocationPermissionGranted = intent.granted,
                            isLocationEnabled = gpsEnabled
                        )
                    }
                    if (intent.granted) {
                        if (!gpsEnabled) {
                            emitEffect(
                                BuyerMapViewEffect.ShowLocationMessage(
                                    message = "Activa la ubicación GPS de tu celular para centrar el mapa",
                                    isGpsDisabled = true
                                )
                            )
                        }
                        fetchLocation(forceFresh = true)
                    }
                }
            }
            is BuyerMapViewIntent.RefreshLocation -> {
                viewModelScope.launch {
                    val hasPerm = locationRepository.hasLocationPermission()
                    val gpsEnabled = locationRepository.isLocationEnabled()
                    updateState {
                        copy(
                            isLocationPermissionGranted = hasPerm,
                            isLocationEnabled = gpsEnabled
                        )
                    }
                    if (!hasPerm) {
                        emitEffect(
                            BuyerMapViewEffect.ShowLocationMessage(
                                message = "Permite el acceso a tu ubicación para encontrarte en el mapa",
                                isGpsDisabled = false
                            )
                        )
                        return@launch
                    }
                    if (!gpsEnabled) {
                        emitEffect(
                            BuyerMapViewEffect.ShowLocationMessage(
                                message = "El GPS del dispositivo está desactivado. Actívalo para ubicarte",
                                isGpsDisabled = true
                            )
                        )
                    }
                    fetchLocation(forceFresh = intent.forceFresh)
                }
            }
            is BuyerMapViewIntent.RadiusSelected -> {
                val isGuest = viewState.value.isGuest || sessionRepository.sessionState.value.isGuest
                if (isGuest && !GuestRadiusPolicy.isRadiusAllowedForGuest(intent.radiusKm)) {
                    updateState { copy(selectedRadiusKm = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) }
                    emitEffect(BuyerMapViewEffect.RequireAuthForRadius)
                } else {
                    val safeRadius = GuestRadiusPolicy.clampRadius(intent.radiusKm, isGuest = false)
                    updateState { copy(selectedRadiusKm = safeRadius) }
                }
            }
            is BuyerMapViewIntent.LayerSelected -> {
                updateState { copy(selectedLayer = intent.layer) }
            }
        }
    }

    private suspend fun fetchLocation(forceFresh: Boolean) {
        updateState { copy(isLocating = true) }
        try {
            val loc = if (forceFresh) {
                locationRepository.getFreshLocation()
            } else {
                locationRepository.getCurrentLocation()
            }
            val hasPerm = locationRepository.hasLocationPermission()
            val gpsEnabled = locationRepository.isLocationEnabled()
            updateState {
                copy(
                    userLocation = loc,
                    hasRealLocation = hasPerm && gpsEnabled,
                    isLocationEnabled = gpsEnabled,
                    isLocating = false,
                    recenterSignal = recenterSignal + 1
                )
            }
        } catch (_: Exception) {
            updateState { copy(isLocating = false) }
        }
    }
}
