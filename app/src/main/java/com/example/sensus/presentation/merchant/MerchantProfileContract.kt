package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantProfileViewState(
    val store: MerchantStore = MerchantStore(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface MerchantProfileViewIntent : ViewIntent {
    data class UpdateStore(val store: MerchantStore) : MerchantProfileViewIntent
}

sealed interface MerchantProfileViewEffect : ViewEffect
