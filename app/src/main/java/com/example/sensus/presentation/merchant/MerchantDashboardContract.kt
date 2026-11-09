package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantDashboardViewState(
    val store: MerchantStore = MerchantStore(),
    val activeStoreOffers: List<ProductOffer> = emptyList(),
    val isLoading: Boolean = false
) : ViewState

sealed interface MerchantDashboardViewIntent : ViewIntent {
    data object Refresh : MerchantDashboardViewIntent
}

sealed interface MerchantDashboardViewEffect : ViewEffect
