package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.presentation.auth.RegisterViewEffect
import com.example.sensus.presentation.auth.RegisterViewIntent
import com.example.sensus.presentation.auth.RegisterViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        registerUseCase = mockk()
        viewModel = RegisterViewModel(registerUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_shouldBeDefault() {
        val state = viewModel.viewState.value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun passwordsMismatch_shouldSetErrorMessageDirectly() = runTest(testDispatcher) {
        viewModel.dispatchIntent(RegisterViewIntent.NameChanged("Rodrigo"))
        viewModel.dispatchIntent(RegisterViewIntent.EmailChanged("rodrigo@sensus.com"))
        viewModel.dispatchIntent(RegisterViewIntent.PasswordChanged("Pass123#"))
        viewModel.dispatchIntent(RegisterViewIntent.ConfirmPasswordChanged("DifferentPass123#"))

        viewModel.dispatchIntent(RegisterViewIntent.SubmitRegister)

        assertEquals("Las contraseñas no coinciden.", viewModel.viewState.value.errorMessage)
        assertFalse(viewModel.viewState.value.isLoading)
    }

    @Test
    fun submitRegister_success_shouldEmitNavigateToHomeEffect() = runTest(testDispatcher) {
        val user = UserProfile(id = "u2", nombreCompleto = "Ana Lopez", correo = "ana@sensus.com")
        coEvery { registerUseCase("Ana Lopez", "ana@sensus.com", "SecurePass123#") } returns Result.Success(user)

        viewModel.dispatchIntent(RegisterViewIntent.NameChanged("Ana Lopez"))
        viewModel.dispatchIntent(RegisterViewIntent.EmailChanged("ana@sensus.com"))
        viewModel.dispatchIntent(RegisterViewIntent.PasswordChanged("SecurePass123#"))
        viewModel.dispatchIntent(RegisterViewIntent.ConfirmPasswordChanged("SecurePass123#"))

        viewModel.effect.test {
            viewModel.dispatchIntent(RegisterViewIntent.SubmitRegister)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is RegisterViewEffect.NavigateToHome)
            assertEquals(user, (effect as RegisterViewEffect.NavigateToHome).userProfile)
            assertFalse(viewModel.viewState.value.isLoading)
        }
    }
}
