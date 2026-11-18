package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.presentation.base.MviViewModel
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.toDomain
import com.example.sensus.presentation.common.toStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

private const val MAP_METERS_PER_KM = 1000

@HiltViewModel
class BuyerMapViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase,
    private val savePreferredMapLayerUseCase: SavePreferredMapLayerUseCase,
    private val getNearbyMerchantsUseCase: GetNearbyMerchantsUseCase
) : MviViewModel<BuyerMapViewState, BuyerMapViewIntent, BuyerMapViewEffect>(
    BuyerMapViewState(
        selectedLayer = getPreferredMapLayerUseCase.peekCached()?.toStyle() ?: MapLayerStyle.STANDARD
    )
) {

    constructor(
        offersRepository: OffersRepository,
        locationRepository: LocationRepository,
        sessionRepository: SessionRepository,
        getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase,
        savePreferredMapLayerUseCase: SavePreferredMapLayerUseCase
    ) : this(
        offersRepository,
        locationRepository,
        sessionRepository,
        getPreferredMapLayerUseCase,
        savePreferredMapLayerUseCase,
        GetNearbyMerchantsUseCase(NoOpMapMerchantRepository)
    )

    @Volatile
    private var hasUserChangedLayer = false

    init {
        val offers = offersRepository.getOffers().filter { it.isPubliclyVisible }
        updateState { copy(offers = offers, selectedOffer = null) }
        loadSavedMapLayer()
        observeSession()
        checkInitialLocationPermission()
        refreshNearbyOffers(viewState.value.userLocation, viewState.value.selectedRadiusKm)
    }

    private fun refreshNearbyOffers(location: com.example.sensus.domain.model.UserLocation, radiusKm: Float) {
        viewModelScope.launch {
            val res = offersRepository.fetchNearbyOffers(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = radiusKm.toDouble()
            )
            if (res is Result.Success) {
                updateState { copy(offers = res.data.filter { it.isPubliclyVisible }) }
            }
        }
        viewModelScope.launch {
            val radiusMeters = (radiusKm * MAP_METERS_PER_KM).toInt()
            val isGuest = viewState.value.isGuest || sessionRepository.sessionState.value.isGuest
            val merchantsRes = getNearbyMerchantsUseCase(location.latitude, location.longitude, radiusMeters, isGuest)
            if (merchantsRes is Result.Success) {
                updateState { copy(nearbyMerchants = merchantsRes.data.filter { it.activo }) }
            }
        }
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
        var lastEmail: String? = null
        var lastIsGuest: Boolean? = null
        var lastIsStoreOpen: Boolean? = null
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                val userChanged = lastIsGuest != null &&
                    (lastEmail != session.activeEmail || lastIsGuest != session.isGuest)
                val storeVisibilityChanged = lastIsStoreOpen != null && lastIsStoreOpen != session.isStoreOpen
                lastEmail = session.activeEmail
                lastIsGuest = session.isGuest
                lastIsStoreOpen = session.isStoreOpen
                updateState {
                    copy(
                        isGuest = session.isGuest,
                        selectedRadiusKm = GuestRadiusPolicy.clampRadius(selectedRadiusKm, session.isGuest)
                    )
                }
                if (userChanged || storeVisibilityChanged) {
                    refreshNearbyOffers(viewState.value.userLocation, viewState.value.selectedRadiusKm)
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
            is BuyerMapViewIntent.OfferSelected -> updateState { copy(selectedOffer = intent.offer, selectedMerchant = null) }
            is BuyerMapViewIntent.MerchantSelected -> updateState { copy(selectedMerchant = intent.merchant, selectedOffer = null) }
            is BuyerMapViewIntent.LocationPermissionGranted -> handlePermissionGranted(intent.granted)
            is BuyerMapViewIntent.RefreshLocation -> handleRefreshLocation(intent.forceFresh)
            is BuyerMapViewIntent.RadiusSelected -> handleRadiusSelected(intent.radiusKm)
            is BuyerMapViewIntent.LayerSelected -> handleLayerSelected(intent.layer)
            BuyerMapViewIntent.RefreshNearby -> refreshNearbyOffers(
                viewState.value.userLocation,
                viewState.value.selectedRadiusKm
            )
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
            refreshNearbyOffers(viewState.value.userLocation, safeRadius)
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
            refreshNearbyOffers(loc, viewState.value.selectedRadiusKm)
        } catch (_: SecurityException) {
            updateState { copy(isLocating = false) }
        } catch (_: IllegalStateException) {
            updateState { copy(isLocating = false) }
        }
    }
}

private object NoOpMapMerchantRepository : MerchantRepository {
    override suspend fun getMyMerchant(): Result<MerchantProfile> = Result.Success(MerchantProfile("", "", "", "", ""))
    override suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double?,
        longitud: Double?
    ): Result<MerchantProfile> = Result.Success(MerchantProfile("", "", "", "", ""))
    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>
    ): Result<MerchantProfile> = Result.Success(MerchantProfile("", "", "", "", ""))
}
