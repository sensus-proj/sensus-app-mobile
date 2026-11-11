package com.example.sensus.presentation.merchant

import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val DASHBOARD_OFFERS_LIMIT = 3

@HiltViewModel
class MerchantDashboardViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val offersRepository: OffersRepository
) : MviViewModel<MerchantDashboardViewState, MerchantDashboardViewIntent, MerchantDashboardViewEffect>(
    MerchantDashboardViewState()
) {

    init {
        loadData()
    }

    override fun processIntent(intent: MerchantDashboardViewIntent) {
        when (intent) {
            is MerchantDashboardViewIntent.Refresh -> loadData()
        }
    }

    private fun loadData() {
        val store = storeRepository.getSampleStore()
        val offers = offersRepository.getOffers().take(DASHBOARD_OFFERS_LIMIT)
        updateState { copy(store = store, activeStoreOffers = offers) }
    }
}
