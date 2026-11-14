package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantDashboardViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val offersRepository: OffersRepository,
    private val getMyMerchantUseCase: GetMyMerchantUseCase
) : MviViewModel<MerchantDashboardViewState, MerchantDashboardViewIntent, MerchantDashboardViewEffect>(
    MerchantDashboardViewState()
) {

    private var fetchRealMerchantEnabled: Boolean = true

    constructor(
        storeRepository: StoreRepository,
        offersRepository: OffersRepository
    ) : this(storeRepository, offersRepository, GetMyMerchantUseCase(NoOpDashboardMerchantRepository)) {
        fetchRealMerchantEnabled = false
    }

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
        val sampleStore = storeRepository.getSampleStore()
        val cachedAll = offersRepository.getOffers()
        val cachedActive = cachedAll.filter { it.isPubliclyVisible }
        updateState {
            val currentName = store.name.takeIf { it != MerchantStore().name } ?: sampleStore.name
            copy(
                store = sampleStore.copy(name = currentName, activeOffersCount = cachedActive.size),
                activeStoreOffers = cachedActive,
                allStoreOffers = cachedAll
            )
        }
        if (!fetchRealMerchantEnabled) return
        viewModelScope.launch {
            when (val res = getMyMerchantUseCase()) {
                is Result.Success -> applyRemoteMerchant(res.data)
                is Result.Failure -> Unit
            }
            when (val offersRes = offersRepository.fetchMerchantOffers()) {
                is Result.Success -> {
                    val all = offersRes.data
                    val active = all.filter { it.isPubliclyVisible }
                    updateState {
                        copy(
                            store = store.copy(activeOffersCount = active.size),
                            activeStoreOffers = active,
                            allStoreOffers = all
                        )
                    }
                }
                is Result.Failure -> Unit
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
                store = store.copy(
                    name = profile.nombreComercial.ifBlank { store.name },
                    address = profile.direccion.ifBlank { store.address },
                    postalCode = profile.codigoPostal.ifBlank { store.postalCode },
                    latitude = profile.latitud,
                    longitude = profile.longitud
                )
            )
        }
    }
}

private object NoOpDashboardMerchantRepository : com.example.sensus.domain.repository.MerchantRepository {
    override suspend fun getMyMerchant(): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
    override suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double?,
        longitud: Double?
    ): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<com.example.sensus.domain.model.HorarioDia>
    ): Result<MerchantProfile> = Result.Failure(com.example.sensus.common.AppError.NotFound("No configurado"))
}
