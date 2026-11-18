package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.UserProfile
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class BuyerProfileViewState(
    val profile: UserProfile? = null,
    val isGuest: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasActiveMerchant: Boolean = false,
    val merchantStoreName: String? = null,
    val isStoreOpen: Boolean = true,
    val isReopeningStore: Boolean = false
) : ViewState

sealed interface BuyerProfileViewIntent : ViewIntent {
    data object LoadProfile : BuyerProfileViewIntent
    data class UpdateName(val newName: String) : BuyerProfileViewIntent
    data object ReopenStore : BuyerProfileViewIntent
    data object Logout : BuyerProfileViewIntent
}

sealed interface BuyerProfileViewEffect : ViewEffect {
    data object StoreReopened : BuyerProfileViewEffect
    data class ShowMessage(val message: String) : BuyerProfileViewEffect
}
