package com.example.sensus.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : MviViewModel<LoginViewState, LoginViewIntent, LoginViewEffect>(LoginViewState()) {

    override fun processIntent(intent: LoginViewIntent) {
        when (intent) {
            is LoginViewIntent.EmailChanged -> updateState { copy(email = intent.email, errorMessage = null) }
            is LoginViewIntent.PasswordChanged -> updateState { copy(password = intent.password, errorMessage = null) }
            is LoginViewIntent.DismissError -> updateState { copy(errorMessage = null) }
            is LoginViewIntent.SubmitLogin -> performLogin()
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
                    val message = result.error.message.ifBlank { "Error al iniciar sesión" }
                    updateState { copy(isLoading = false, errorMessage = message) }
                    emitEffect(LoginViewEffect.ShowToast(message))
                }
            }
        }
    }
}
