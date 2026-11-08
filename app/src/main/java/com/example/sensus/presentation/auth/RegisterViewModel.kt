package com.example.sensus.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : MviViewModel<RegisterViewState, RegisterViewIntent, RegisterViewEffect>(RegisterViewState()) {

    override fun processIntent(intent: RegisterViewIntent) {
        when (intent) {
            is RegisterViewIntent.NameChanged -> updateState { copy(name = intent.name, errorMessage = null) }
            is RegisterViewIntent.EmailChanged -> updateState { copy(email = intent.email, errorMessage = null) }
            is RegisterViewIntent.PasswordChanged -> updateState { copy(password = intent.password, errorMessage = null) }
            is RegisterViewIntent.ConfirmPasswordChanged -> updateState { copy(confirmPassword = intent.confirmPassword, errorMessage = null) }
            is RegisterViewIntent.DismissError -> updateState { copy(errorMessage = null) }
            is RegisterViewIntent.SubmitRegister -> performRegister()
        }
    }

    private fun performRegister() {
        val currentState = viewState.value
        if (currentState.password != currentState.confirmPassword) {
            updateState { copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = registerUseCase(currentState.name, currentState.email, currentState.password)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false) }
                    emitEffect(RegisterViewEffect.NavigateToHome(result.data))
                }
                is Result.Failure -> {
                    val message = result.error.message.ifBlank { "Error al registrarse" }
                    updateState { copy(isLoading = false, errorMessage = message) }
                    emitEffect(RegisterViewEffect.ShowToast(message))
                }
            }
        }
    }
}
