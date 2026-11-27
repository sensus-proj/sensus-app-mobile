package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class SavedInterestsViewState(
    val savedOffers: List<ProductOffer> = emptyList()
) : ViewState {
    val savedOfferIds: Set<String>
        get() = savedOffers.mapTo(mutableSetOf()) { it.id }

    val totalCount: Int
        get() = savedOffers.size
}

sealed interface SavedInterestsViewIntent : ViewIntent {
    data class ToggleOffer(val offer: ProductOffer) : SavedInterestsViewIntent
    data class RemoveOffer(val offerId: String) : SavedInterestsViewIntent
}

sealed interface SavedInterestsViewEffect : ViewEffect {
    data class SavedStateChanged(val isSaved: Boolean) : SavedInterestsViewEffect
    data object RequireAuth : SavedInterestsViewEffect
}
