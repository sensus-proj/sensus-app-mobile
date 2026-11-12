package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.presentation.base.MviViewModel
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.toDomain
import com.example.sensus.presentation.common.toStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BuyerMapViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase,
    private val savePreferredMapLayerUseCase: SavePreferredMapLayerUseCase
) : MviViewModel<BuyerMapViewState, BuyerMapViewIntent, BuyerMapViewEffect>(
    BuyerMapViewState(
        selectedLayer = getPreferredMapLayerUseCase.peekCached()?.toStyle() ?: MapLayerStyle.STANDARD
    )
) {

    @Volatile
    private var hasUserChangedLayer = false

    init {
        val offers = offersRepository.getOffers()
        updateState { copy(offers = offers, selectedOffer = null) }
        loadSavedMapLayer()
        observeSession()
        checkInitialLocationPermission()
    }

    private fun loadSavedMapLayer() {
        viewModelScope.launch {
            val savedLayer = getPreferredMapLayerUseCase().toStyle()
            if (!hasUserChangedLayer) {
                updateState { copy(selectedLayer = savedLayer) }
            }
        }
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

    private fun checkInitialLocationPermission() {
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
            is BuyerMapViewIntent.LocationPermissionGranted -> handlePermissionGranted(intent.granted)
            is BuyerMapViewIntent.RefreshLocation -> handleRefreshLocation(intent.forceFresh)
            is BuyerMapViewIntent.RadiusSelected -> handleRadiusSelected(intent.radiusKm)
            is BuyerMapViewIntent.LayerSelected -> handleLayerSelected(intent.layer)
        }
    }

    private fun handleLayerSelected(layer: MapLayerStyle) {
        hasUserChangedLayer = true
        updateState { copy(selectedLayer = layer) }
        viewModelScope.launch {
            savePreferredMapLayerUseCase(layer.toDomain())
        }
    }

    private fun handlePermissionGranted(granted: Boolean) {
        viewModelScope.launch {
            sessionRepository.setLocationPermission(granted)
            val gpsEnabled = locationRepository.isLocationEnabled()
            updateState {
                copy(
                    isLocationPermissionGranted = granted,
                    isLocationEnabled = gpsEnabled
                )
            }
            if (!granted) return@launch
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

    private fun handleRefreshLocation(forceFresh: Boolean) {
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
            fetchLocation(forceFresh = forceFresh)
        }
    }

    private fun handleRadiusSelected(radiusKm: Float) {
        val isGuest = viewState.value.isGuest || sessionRepository.sessionState.value.isGuest
        if (isGuest && !GuestRadiusPolicy.isRadiusAllowedForGuest(radiusKm)) {
            updateState { copy(selectedRadiusKm = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) }
            emitEffect(BuyerMapViewEffect.RequireAuthForRadius)
        } else {
            val safeRadius = GuestRadiusPolicy.clampRadius(radiusKm, isGuest = false)
            updateState { copy(selectedRadiusKm = safeRadius) }
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
        } catch (_: SecurityException) {
            updateState { copy(isLocating = false) }
        } catch (_: IllegalStateException) {
            updateState { copy(isLocating = false) }
        }
    }
}
