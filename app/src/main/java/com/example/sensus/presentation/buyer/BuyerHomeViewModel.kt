package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetNearbyOffersUseCase
import com.example.sensus.domain.usecase.nearbyRadiusKm
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val METERS_PER_KM = 1000

@HiltViewModel
class BuyerHomeViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository,
    private val getNearbyOffers: GetNearbyOffersUseCase,
    private val getNearbyMerchants: GetNearbyMerchantsUseCase? = null
) : MviViewModel<BuyerHomeViewState, BuyerHomeViewIntent, BuyerHomeViewEffect>(BuyerHomeViewState()) {

    constructor(offersRepository: OffersRepository, locationRepository: LocationRepository, sessionRepository: SessionRepository) : this(
        locationRepository,
        sessionRepository,
        GetNearbyOffersUseCase(offersRepository)
    )

    private var merchantProfiles = emptyMap<String, MerchantProfile>()
    private var offersRefresh: Job? = null
    private var profileRefresh: Job? = null

    init {
        loadOffers()
        observeSession()
        refreshLocation(forceFresh = false)
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                val previousRadius = viewState.value.selectedRadiusKm
                val preferredRadius = if (session.isGuest) null else getNearbyOffers.getPreferredRadiusKm()
                updateState {
                    copy(
                        isGuest = session.isGuest,
                        selectedRadiusKm = nearbyRadiusKm(preferredRadius ?: selectedRadiusKm, session.isGuest)
                    )
                }
                if (viewState.value.selectedRadiusKm != previousRadius) loadOffers()
            }
        }
    }

    override fun processIntent(intent: BuyerHomeViewIntent) {
        when (intent) {
            is BuyerHomeViewIntent.CategorySelected -> updateState { copy(selectedCategory = intent.category) }
            is BuyerHomeViewIntent.SearchQueryChanged -> updateState { copy(searchQuery = intent.query) }
            is BuyerHomeViewIntent.RadiusSelected -> selectRadius(intent.radiusKm)
            is BuyerHomeViewIntent.RefreshLocation -> refreshLocation(forceFresh = true)
            BuyerHomeViewIntent.RefreshOffers -> loadOffers()
            is BuyerHomeViewIntent.ChangeFilters -> updateState { copy(filters = intent.filters) }
            BuyerHomeViewIntent.LoadNextPage -> viewState.value.nextPage?.let { loadOffers(it) }
        }
    }

    private fun selectRadius(radiusKm: Float) {
        val isGuest = viewState.value.isGuest || sessionRepository.sessionState.value.isGuest
        if (isGuest && radiusKm != GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) {
            updateState { copy(selectedRadiusKm = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM) }
            emitEffect(BuyerHomeViewEffect.RequireAuthForRadius)
        } else {
            val safeRadius = GuestRadiusPolicy.clampRadius(radiusKm, isGuest = false)
            updateState { copy(selectedRadiusKm = safeRadius) }
            if (!isGuest) viewModelScope.launch { getNearbyOffers.savePreferredRadiusKm(safeRadius) }
            loadOffers()
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

    private fun loadOffers(page: Int = 1) {
        if (page == 1) {
            offersRefresh?.cancel()
            profileRefresh?.cancel()
            merchantProfiles = emptyMap()
            updateState { copy(isLoading = true, errorMessage = null, nextPage = null, pageLoadFailed = false) }
        } else if (offersRefresh?.isActive == true) {
            return
        }
        offersRefresh = viewModelScope.launch {
            val state = viewState.value
            val result = getNearbyOffers.page(state.userLocation, state.selectedRadiusKm, sessionRepository.sessionState.value.isGuest, page)
            ensureActive()
            applyOffersPage(page, result)
            if (page == 1 && result is Result.Success && result.data.items.isNotEmpty()) {
                loadProfiles(state)
            }
        }
    }

    private fun loadProfiles(state: BuyerHomeViewState) {
        val nearby = getNearbyMerchants ?: return
        profileRefresh = viewModelScope.launch {
            val profiles = nearby(
                state.userLocation.latitude,
                state.userLocation.longitude,
                (state.selectedRadiusKm * METERS_PER_KM).toInt(),
                sessionRepository.sessionState.value.isGuest
            ).getOrNull().orEmpty()
            ensureActive()
            merchantProfiles = profiles.associateBy { it.id }
            updateState { copy(offers = offers.map { it.copy(merchantProfile = merchantProfiles[it.merchantId]) }) }
        }
    }

    private suspend fun applyOffersPage(page: Int, result: Result<PagedOffers>) {
        when {
            result is Result.Success -> updateState {
                val visible = result.data.items.filter { it.isPubliclyVisible }.map { it.copy(merchantProfile = merchantProfiles[it.merchantId]) }
                val merged = if (page == 1) visible else (offers + visible).distinctBy { it.id }
                copy(offers = merged, nextPage = result.data.nextPage, isLoading = false, pageLoadFailed = false, usingCachedOffers = false)
            }
            page == 1 -> {
                val cached = getNearbyOffers.readCachedOffers().filter { it.isPubliclyVisible }
                kotlin.coroutines.coroutineContext.ensureActive()
                val offline = if (cached.isEmpty()) "Sin conexión" else null
                updateState { copy(offers = cached, isLoading = false, usingCachedOffers = true, errorMessage = offline) }
            }
            else -> updateState { copy(pageLoadFailed = true) }
        }
    }
}
