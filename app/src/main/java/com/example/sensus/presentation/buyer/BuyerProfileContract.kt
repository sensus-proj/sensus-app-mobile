package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.UserProfile
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class BuyerProfileViewState(
    val profile: UserProfile? = null,
    val isGuest: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface BuyerProfileViewIntent : ViewIntent {
    data object LoadProfile : BuyerProfileViewIntent
    data class UpdateName(val newName: String) : BuyerProfileViewIntent
    data object Logout : BuyerProfileViewIntent
}

sealed interface BuyerProfileViewEffect : ViewEffect
