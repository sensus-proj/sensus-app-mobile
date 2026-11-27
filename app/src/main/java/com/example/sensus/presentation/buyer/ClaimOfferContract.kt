package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class ClaimOfferViewState(
    val offerId: String? = null,
    val items: List<CartItem> = emptyList(),
    val expectExisting: Boolean = false,
    val loading: Boolean = false,
    val result: OfferClaim? = null,
    val error: AppError? = null
) : ViewState {
    val canSubmit: Boolean get() = !loading && result == null && !expectExisting
    val existingCodeUnavailable: Boolean get() = expectExisting && result == null && !loading
    val totalUnits: Int get() = result?.totalUnits ?: items.sumOf { it.quantity }.coerceAtLeast(1)
}

sealed interface ClaimOfferViewIntent : ViewIntent {
    data class Initialize(
        val offerId: String,
        val expectExisting: Boolean = false,
        val items: List<CartItem> = emptyList()
    ) : ClaimOfferViewIntent

    data class Submit(val value: String, val items: List<CartItem> = emptyList()) : ClaimOfferViewIntent
}

sealed interface ClaimOfferViewEffect : ViewEffect {
    data class Confirmed(val offerId: String) : ClaimOfferViewEffect
}
