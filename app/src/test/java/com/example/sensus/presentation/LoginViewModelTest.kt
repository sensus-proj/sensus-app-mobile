package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.presentation.auth.LoginViewEffect
import com.example.sensus.presentation.auth.LoginViewIntent
import com.example.sensus.presentation.auth.LoginViewModel
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_shouldBeDefault() {
        val state = viewModel.viewState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun emailChanged_shouldUpdateStateAndClearError() = runTest(testDispatcher) {
        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("test@sensus.com"))
        assertEquals("test@sensus.com", viewModel.viewState.value.email)
    }

    @Test
    fun passwordChanged_shouldUpdateStateAndClearError() = runTest(testDispatcher) {
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("Secret123#"))
        assertEquals("Secret123#", viewModel.viewState.value.password)
    }

    @Test
    fun submitLogin_success_shouldEmitNavigateToHomeEffect() = runTest(testDispatcher) {
        val user = UserProfile(id = "u1", nombreCompleto = "Juan Perez", correo = "juan@sensus.com")
        coEvery { loginUseCase("juan@sensus.com", "Pass123#") } returns Result.Success(user)

        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("juan@sensus.com"))
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("Pass123#"))

        viewModel.effect.test {
            viewModel.dispatchIntent(LoginViewIntent.SubmitLogin)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is LoginViewEffect.NavigateToHome)
            assertEquals(user, (effect as LoginViewEffect.NavigateToHome).userProfile)
            assertFalse(viewModel.viewState.value.isLoading)
        }
    }

    @Test
    fun submitLogin_failure_shouldUpdateErrorAndEmitToastEffect() = runTest(testDispatcher) {
        coEvery { loginUseCase(any(), any()) } returns Result.Failure(AppError.Validation("Credenciales inválidas"))

        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("bad@sensus.com"))
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("wrong"))

        viewModel.effect.test {
            viewModel.dispatchIntent(LoginViewIntent.SubmitLogin)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is LoginViewEffect.ShowToast)
            assertEquals("Credenciales inválidas", (effect as LoginViewEffect.ShowToast).message)
            assertEquals("Credenciales inválidas", viewModel.viewState.value.errorMessage)
            assertFalse(viewModel.viewState.value.isLoading)
        }
    }
}
