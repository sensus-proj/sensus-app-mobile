package com.example.sensus.presentation.merchant

import com.example.sensus.R
import com.example.sensus.domain.model.MerchantOfferMetrics
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

enum class MerchantOfferFilter(val labelResource: Int) {
    ACTIVE(R.string.merchant_inventory_active),
    PAUSED(R.string.merchant_inventory_paused),
    FINISHED(R.string.merchant_inventory_finished);

    fun matches(offer: ProductOffer): Boolean = when (this) {
        ACTIVE -> offer.isPubliclyVisible
        PAUSED -> offer.status == OfferLifecycleStatus.PAUSED
        FINISHED -> !offer.isPubliclyVisible && offer.status != OfferLifecycleStatus.PAUSED
    }
}

data class MerchantDashboardViewState(
    val store: MerchantStore = MerchantStore(),
    val activeStoreOffers: List<ProductOffer> = emptyList(),
    val allStoreOffers: List<ProductOffer> = emptyList(),
    val selectedFilter: MerchantOfferFilter = MerchantOfferFilter.ACTIVE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasMerchantProfile: Boolean = false
) : ViewState {
    val filteredOffers: List<ProductOffer>
        get() = allStoreOffers.filter(selectedFilter::matches)

    val metrics: MerchantOfferMetrics
        get() = MerchantOfferMetrics.from(allStoreOffers)
}

sealed interface MerchantDashboardViewIntent : ViewIntent {
    data object Refresh : MerchantDashboardViewIntent
    data class AdjustOfferStock(val offer: ProductOffer, val newStock: Int) : MerchantDashboardViewIntent
    data class SelectOfferFilter(val filter: MerchantOfferFilter) : MerchantDashboardViewIntent
}

sealed interface MerchantDashboardViewEffect : ViewEffect
