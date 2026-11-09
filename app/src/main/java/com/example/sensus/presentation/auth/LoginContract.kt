package com.example.sensus.presentation.auth

import com.example.sensus.domain.model.UserProfile
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class LoginViewState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGuestLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface LoginViewIntent : ViewIntent {
    data class EmailChanged(val email: String) : LoginViewIntent
    data class PasswordChanged(val password: String) : LoginViewIntent
    data object TogglePasswordVisibility : LoginViewIntent
    data object SubmitLogin : LoginViewIntent
    data object StartGuestSession : LoginViewIntent
    data object DismissError : LoginViewIntent
}

sealed interface LoginViewEffect : ViewEffect {
    data class NavigateToHome(val userProfile: UserProfile) : LoginViewEffect
    data object NavigateToHomeAsGuest : LoginViewEffect
    data class ShowToast(val message: String) : LoginViewEffect
}
