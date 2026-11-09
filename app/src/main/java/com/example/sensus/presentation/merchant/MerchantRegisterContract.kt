package com.example.sensus.presentation.merchant

import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantRegisterViewState(
    val isRegistering: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface MerchantRegisterViewIntent : ViewIntent {
    data class ActivateMerchant(
        val storeName: String,
        val category: String = "",
        val address: String = "",
        val schedule: String = "",
        val phone: String = "",
        val postalCode: String = ""
    ) : MerchantRegisterViewIntent
}

sealed interface MerchantRegisterViewEffect : ViewEffect {
    data object ActivationSuccess : MerchantRegisterViewEffect
}
