package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantNewOfferViewState(
    val isPublishing: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface MerchantNewOfferViewIntent : ViewIntent {
    data class PublishOffer(val offer: ProductOffer) : MerchantNewOfferViewIntent
}

sealed interface MerchantNewOfferViewEffect : ViewEffect {
    data class OfferCreated(val offer: ProductOffer) : MerchantNewOfferViewEffect
}
