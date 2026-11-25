package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantDashboardViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val getMyMerchantUseCase: GetMyMerchantUseCase
) : MviViewModel<MerchantDashboardViewState, MerchantDashboardViewIntent, MerchantDashboardViewEffect>(
    MerchantDashboardViewState()
) {

    private var inventoryJob: Job? = null

    init {
        loadData()
    }

    override fun processIntent(intent: MerchantDashboardViewIntent) {
        when (intent) {
            is MerchantDashboardViewIntent.Refresh -> loadData()
            is MerchantDashboardViewIntent.AdjustOfferStock -> adjustStock(intent.offer, intent.newStock)
            is MerchantDashboardViewIntent.SelectOfferFilter -> updateState { copy(selectedFilter = intent.filter) }
        }
    }

    private fun loadData() {
        inventoryJob?.cancel()
        updateState { MerchantDashboardViewState(selectedFilter = selectedFilter, isLoading = true) }
        inventoryJob = viewModelScope.launch {
            when (val profile = getMyMerchantUseCase()) {
                is Result.Success -> applyRemoteMerchant(profile.data)
                is Result.Failure -> Unit
            }
            val inventory = offersRepository.fetchMerchantOffers()
            ensureActive()
            when (inventory) {
                is Result.Success -> updateState {
                    val active = inventory.data.filter { it.isPubliclyVisible }
                    copy(
                        store = store.copy(activeOffersCount = active.size),
                        activeStoreOffers = active,
                        allStoreOffers = inventory.data,
                        isLoading = false
                    )
                }
                is Result.Failure -> updateState { copy(isLoading = false, errorMessage = inventory.error.message) }
            }
        }
    }

    private fun adjustStock(offer: ProductOffer, newStock: Int) {
        val clamped = newStock.coerceAtLeast(0)
        viewModelScope.launch {
            when (val res = offersRepository.updateOfferStock(offer, clamped)) {
                is Result.Success -> {
                    updateState {
                        val baseList = allStoreOffers.ifEmpty { activeStoreOffers }
                        val updatedAll = baseList.map { item ->
                            if (item.id == offer.id) {
                                if (clamped <= 0) {
                                    res.data.copy(unitsLeft = 0, status = OfferLifecycleStatus.PAUSED)
                                } else {
                                    res.data.copy(unitsLeft = clamped)
                                }
                            } else {
                                item
                            }
                        }
                        val updatedActive = updatedAll.filter { it.isPubliclyVisible }
                        copy(
                            store = store.copy(activeOffersCount = updatedActive.size),
                            activeStoreOffers = updatedActive,
                            allStoreOffers = updatedAll,
                            errorMessage = null
                        )
                    }
                }
                is Result.Failure -> updateState { copy(errorMessage = res.error.message) }
            }
        }
    }

    private fun applyRemoteMerchant(profile: MerchantProfile) {
        updateState {
            copy(
                hasMerchantProfile = true,
                store = store.copy(
                    name = profile.nombreComercial.ifBlank { store.name },
                    address = profile.direccion.ifBlank { store.address },
                    postalCode = profile.codigoPostal.ifBlank { store.postalCode },
                    latitude = profile.latitud,
                    longitude = profile.longitud,
                    isOpen = profile.activo,
                    photoUrl = profile.fotoFachadaUrl ?: store.photoUrl
                )
            )
        }
    }
}
