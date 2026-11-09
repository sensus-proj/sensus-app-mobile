package com.example.sensus.presentation.modals

import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class ForgotPasswordViewState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface ForgotPasswordViewIntent : ViewIntent {
    data object Init : ForgotPasswordViewIntent
}

sealed interface ForgotPasswordViewEffect : ViewEffect
