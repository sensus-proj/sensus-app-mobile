package com.example.sensus.presentation.auth

import com.example.sensus.domain.model.UserProfile
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class RegisterViewState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface RegisterViewIntent : ViewIntent {
    data class NameChanged(val name: String) : RegisterViewIntent
    data class EmailChanged(val email: String) : RegisterViewIntent
    data class PasswordChanged(val password: String) : RegisterViewIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterViewIntent
    data object SubmitRegister : RegisterViewIntent
    data object DismissError : RegisterViewIntent
}

sealed interface RegisterViewEffect : ViewEffect {
    data class NavigateToHome(val userProfile: UserProfile) : RegisterViewEffect
    data class ShowToast(val message: String) : RegisterViewEffect
}
