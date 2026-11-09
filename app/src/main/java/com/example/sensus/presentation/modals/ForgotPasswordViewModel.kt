package com.example.sensus.presentation.modals

import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    val resetPasswordUseCase: ResetPasswordUseCase
) : MviViewModel<ForgotPasswordViewState, ForgotPasswordViewIntent, ForgotPasswordViewEffect>(
    ForgotPasswordViewState()
) {
    override fun processIntent(intent: ForgotPasswordViewIntent) = Unit
}
