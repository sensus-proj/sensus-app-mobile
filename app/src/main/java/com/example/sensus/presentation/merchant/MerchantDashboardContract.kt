package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

enum class MerchantOfferFilter(val label: String) {
    ACTIVE("Activas"),
    PAUSED("Pausadas"),
    ARCHIVED("De baja")
}

data class MerchantDashboardViewState(
    val store: MerchantStore = MerchantStore(),
    val activeStoreOffers: List<ProductOffer> = emptyList(),
    val allStoreOffers: List<ProductOffer> = emptyList(),
    val selectedFilter: MerchantOfferFilter = MerchantOfferFilter.ACTIVE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState {
    val filteredOffers: List<ProductOffer>
        get() = when (selectedFilter) {
            MerchantOfferFilter.ACTIVE -> allStoreOffers.filter { it.isPubliclyVisible }
            MerchantOfferFilter.PAUSED -> allStoreOffers.filter {
                it.status == OfferLifecycleStatus.PAUSED ||
                    (it.status == OfferLifecycleStatus.ACTIVE && it.unitsLeft <= 0) ||
                    it.status == OfferLifecycleStatus.SOLD_OUT
            }
            MerchantOfferFilter.ARCHIVED -> allStoreOffers.filter {
                it.status == OfferLifecycleStatus.ARCHIVED || it.status == OfferLifecycleStatus.EXPIRED
            }
        }
}

sealed interface MerchantDashboardViewIntent : ViewIntent {
    data object Refresh : MerchantDashboardViewIntent
    data class AdjustOfferStock(val offer: ProductOffer, val newStock: Int) : MerchantDashboardViewIntent
    data class SelectOfferFilter(val filter: MerchantOfferFilter) : MerchantDashboardViewIntent
}

sealed interface MerchantDashboardViewEffect : ViewEffect
