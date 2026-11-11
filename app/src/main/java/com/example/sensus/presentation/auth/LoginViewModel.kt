package com.example.sensus.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val startGuestSessionUseCase: StartGuestSessionUseCase
) : MviViewModel<LoginViewState, LoginViewIntent, LoginViewEffect>(LoginViewState()) {

    override fun processIntent(intent: LoginViewIntent) {
        when (intent) {
            is LoginViewIntent.EmailChanged -> updateState { copy(email = intent.email, errorMessage = null) }
            is LoginViewIntent.PasswordChanged -> updateState { copy(password = intent.password, errorMessage = null) }
            is LoginViewIntent.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            is LoginViewIntent.DismissError -> updateState { copy(errorMessage = null) }
            is LoginViewIntent.SubmitLogin -> performLogin()
            is LoginViewIntent.StartGuestSession -> performGuestLogin()
        }
    }

    private fun performLogin() {
        val currentState = viewState.value
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(currentState.email, currentState.password)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    emitEffect(LoginViewEffect.NavigateToHome(result.data))
                }
                is Result.Failure -> {
                    val message = result.error.message.ifBlank { "Error al iniciar sesin" }
                    updateState { copy(isLoading = false, errorMessage = message) }
                    emitEffect(LoginViewEffect.ShowToast(message))
                }
            }
        }
    }

    private fun performGuestLogin() {
        updateState { copy(isGuestLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = startGuestSessionUseCase()) {
                is Result.Success -> {
                    updateState { copy(isGuestLoading = false) }
                    emitEffect(LoginViewEffect.NavigateToHomeAsGuest)
                }
                is Result.Failure -> {
                    val message = result.error.message.ifBlank { "Error al iniciar sesin como invitado" }
                    updateState { copy(isGuestLoading = false, errorMessage = message) }
                    emitEffect(LoginViewEffect.ShowToast(message))
                }
            }
        }
    }
}
