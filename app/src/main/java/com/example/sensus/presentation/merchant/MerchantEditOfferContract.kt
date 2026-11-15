package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferValidationError
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantEditOfferViewState(
    val currentOffer: ProductOffer? = null,
    val republishFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isChangingStatus: Boolean = false,
    val fieldError: OfferValidationError? = null,
    val errorMessage: String? = null
) : ViewState

sealed interface MerchantEditOfferViewIntent : ViewIntent {
    data class Initialize(val offer: ProductOffer) : MerchantEditOfferViewIntent
    data class Republish(val input: RepublishOfferInput) : MerchantEditOfferViewIntent
    data class SaveChanges(val input: UpdateOfferInput) : MerchantEditOfferViewIntent
    data class ChangeStatus(val offer: ProductOffer, val newStatus: OfferLifecycleStatus) : MerchantEditOfferViewIntent
    data object FieldEdited : MerchantEditOfferViewIntent
}

sealed interface MerchantEditOfferViewEffect : ViewEffect {
    data class OfferRepublished(val offer: ProductOffer) : MerchantEditOfferViewEffect
    data class OfferUpdated(val offer: ProductOffer, val message: String) : MerchantEditOfferViewEffect
    data class OfferStatusChanged(val offer: ProductOffer, val message: String) : MerchantEditOfferViewEffect
}
