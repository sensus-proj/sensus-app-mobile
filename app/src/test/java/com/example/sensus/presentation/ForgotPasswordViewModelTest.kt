package com.example.sensus.presentation

import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.presentation.modals.ForgotPasswordViewIntent
import com.example.sensus.presentation.modals.ForgotPasswordViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test

class ForgotPasswordViewModelTest {

    @Test
    fun forgotPasswordViewModel_instantiation() = runTest {
        val reqUseCase = mockk<RequestPasswordResetUseCase>(relaxed = true)
        val resetUseCase = mockk<ResetPasswordUseCase>(relaxed = true)
        coEvery { reqUseCase("test@sensus.com") } returns Result.Success(Unit)
        coEvery { resetUseCase("token", "NewPass123!") } returns Result.Success(Unit)

        val vm = ForgotPasswordViewModel(reqUseCase, resetUseCase)
        assertNotNull(vm.viewState.value)
        vm.dispatchIntent(ForgotPasswordViewIntent.Init)
    }
}
