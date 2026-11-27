package com.example.sensus.presentation

import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.presentation.modals.ForgotPasswordViewIntent
import com.example.sensus.presentation.modals.ForgotPasswordViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
