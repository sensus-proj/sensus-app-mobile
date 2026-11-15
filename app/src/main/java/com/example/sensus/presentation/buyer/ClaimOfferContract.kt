package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class ClaimOfferViewState(
    val offerId: String? = null,
    val expectExisting: Boolean = false,
    val loading: Boolean = false,
    val result: OfferClaim? = null,
    val error: AppError? = null
) : ViewState {
    val canSubmit: Boolean get() = !loading && result == null && !expectExisting
    val existingCodeUnavailable: Boolean get() = expectExisting && result == null && !loading
}

sealed interface ClaimOfferViewIntent : ViewIntent {
    data class Initialize(val offerId: String, val expectExisting: Boolean = false) : ClaimOfferViewIntent
    data class Submit(val value: String) : ClaimOfferViewIntent
}

sealed interface ClaimOfferViewEffect : ViewEffect {
    data class Confirmed(val offerId: String) : ClaimOfferViewEffect
}
