package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferValidationError
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantNewOfferViewState(
    val isPublishing: Boolean = false,
    val fieldError: OfferValidationError? = null,
    val errorMessage: String? = null,
    val moderationMessage: String? = null
) : ViewState

sealed interface MerchantNewOfferViewIntent : ViewIntent {
    /** No es data class: `imageBytes` es un arreglo y no debe participar en equals/hashCode. */
    class PublishOffer(val input: CreateOfferInput, val imageBytes: ByteArray? = null) : MerchantNewOfferViewIntent
    data object FieldEdited : MerchantNewOfferViewIntent
    data object DismissModeration : MerchantNewOfferViewIntent
}

sealed interface MerchantNewOfferViewEffect : ViewEffect {
    data class OfferCreated(val offer: ProductOffer) : MerchantNewOfferViewEffect
}
