package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetNearbyOffersUseCase
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.domain.usecase.nearbyRadiusKm
import com.example.sensus.presentation.base.MviViewModel
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.toDomain
import com.example.sensus.presentation.common.toStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val MAP_METERS_PER_KM = 1000

@HiltViewModel
class BuyerMapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase,
    private val savePreferredMapLayerUseCase: SavePreferredMapLayerUseCase,
    private val getNearbyMerchantsUseCase: GetNearbyMerchantsUseCase,
    private val getNearbyOffers: GetNearbyOffersUseCase
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
        locationRepository,
        sessionRepository,
        getPreferredMapLayerUseCase,
        savePreferredMapLayerUseCase,
        GetNearbyMerchantsUseCase(NoOpMapMerchantRepository),
        GetNearbyOffersUseCase(offersRepository)
    )

    @Volatile
    private var hasUserChangedLayer = false
    private var nearbyRefresh: Job? = null
    private var currentSearch: NearbySearch? = null
    private var initialCacheLoad: Job? = null

    init {
        initialCacheLoad = viewModelScope.launch {
            val offers = getNearbyOffers.readCachedOffers().filter { it.isPubliclyVisible }
            ensureActive()
            updateState { copy(offers = offers) }
        }
        loadSavedMapLayer()
        observeSession()
        checkInitialLocationPermission()
        refreshNearbyOffers(viewState.value.userLocation, viewState.value.selectedRadiusKm)
    }

    private fun refreshNearbyOffers(location: UserLocation, radiusKm: Float, force: Boolean = false) {
        val isGuest = sessionRepository.sessionState.value.isGuest
        val search = NearbySearch(location, nearbyRadiusKm(radiusKm, isGuest), isGuest)
        if (!force && search == currentSearch && nearbyRefresh?.isActive == true) return
        if (currentSearch != null && (force || search != currentSearch)) {
            initialCacheLoad?.cancel()
        }
        currentSearch = search
        nearbyRefresh?.cancel()
        nearbyRefresh = viewModelScope.launch {
            launch { refreshOffers(search) }
            launch { refreshMerchants(search) }
        }
    }

    private suspend fun refreshOffers(search: NearbySearch) {
        val result = getNearbyOffers(search.location, search.radiusKm, search.isGuest)
        currentCoroutineContext().ensureActive()
        if (result is Result.Success) {
            initialCacheLoad?.cancel()
            updateState { copy(offers = result.data.filter { it.isPubliclyVisible }) }
        }
    }

    private suspend fun refreshMerchants(search: NearbySearch) {
        val radiusMeters = (search.radiusKm * MAP_METERS_PER_KM).toInt()
        val result = getNearbyMerchantsUseCase(search.location.latitude, search.location.longitude, radiusMeters, search.isGuest)
        currentCoroutineContext().ensureActive()
        if (result is Result.Success) {
            updateState { copy(nearbyMerchants = result.data.filter { it.activo }) }
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
                val previousRadius = viewState.value.selectedRadiusKm
                val preferredRadius = if (session.isGuest) null else getNearbyOffers.getPreferredRadiusKm()
                val userChanged = lastIsGuest != null &&
                    (lastEmail != session.activeEmail || lastIsGuest != session.isGuest)
                val storeVisibilityChanged = lastIsStoreOpen != null && lastIsStoreOpen != session.isStoreOpen
                lastEmail = session.activeEmail
                lastIsGuest = session.isGuest
                lastIsStoreOpen = session.isStoreOpen
                updateState {
                    copy(
                        isGuest = session.isGuest,
                        selectedRadiusKm = nearbyRadiusKm(preferredRadius ?: selectedRadiusKm, session.isGuest)
                    )
                }
                if (userChanged || storeVisibilityChanged || viewState.value.selectedRadiusKm != previousRadius) {
                    refreshNearbyOffers(viewState.value.userLocation, viewState.value.selectedRadiusKm, force = true)
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
            is BuyerMapViewIntent.ManualLocationSelected -> selectManualLocation(intent.location)
            is BuyerMapViewIntent.RadiusSelected -> handleRadiusSelected(intent.radiusKm)
            is BuyerMapViewIntent.LayerSelected -> handleLayerSelected(intent.layer)
            BuyerMapViewIntent.RefreshNearby -> refreshNearbyOffers(
                viewState.value.userLocation,
                viewState.value.selectedRadiusKm
            )
        }
    }

    private fun selectManualLocation(location: UserLocation) {
        updateState { copy(userLocation = location, recenterSignal = recenterSignal + 1) }
        refreshNearbyOffers(location, viewState.value.selectedRadiusKm)
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
        if (isGuest && radiusKm != GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) {
            updateState { copy(selectedRadiusKm = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) }
            emitEffect(BuyerMapViewEffect.RequireAuthForRadius)
        } else {
            val safeRadius = GuestRadiusPolicy.clampRadius(radiusKm, isGuest = false)
            updateState { copy(selectedRadiusKm = safeRadius) }
            if (!isGuest) viewModelScope.launch { getNearbyOffers.savePreferredRadiusKm(safeRadius) }
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

private data class NearbySearch(val location: UserLocation, val radiusKm: Float, val isGuest: Boolean)

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
