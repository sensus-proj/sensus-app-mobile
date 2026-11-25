package com.example.sensus.presentation.merchant

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantSalesViewState(
    val input: String = "",
    val loading: Boolean = false,
    val result: ClaimRedemption? = null,
    val error: AppError? = null,
    val scanned: Boolean = false
) : ViewState

sealed interface MerchantSalesViewIntent : ViewIntent {
    data class Submit(val value: String) : MerchantSalesViewIntent
    data class Scan(val value: String) : MerchantSalesViewIntent
    data class Edit(val value: String) : MerchantSalesViewIntent
}

sealed interface MerchantSalesViewEffect : ViewEffect
