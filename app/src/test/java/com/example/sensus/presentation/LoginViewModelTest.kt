package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
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
    private lateinit var startGuestSessionUseCase: StartGuestSessionUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        startGuestSessionUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emailChanged_updatesState() {
        val viewModel = LoginViewModel(loginUseCase, startGuestSessionUseCase)
        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("test@example.com"))

        assertEquals("test@example.com", viewModel.viewState.value.email)
    }

    @Test
    fun passwordChanged_updatesState() {
        val viewModel = LoginViewModel(loginUseCase, startGuestSessionUseCase)
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("secret123"))

        assertEquals("secret123", viewModel.viewState.value.password)
    }

    @Test
    fun submitLogin_success_emitsNavigateEffect() = runTest {
        val mockProfile = UserProfile(
            id = "user1",
            nombreCompleto = "Juan Perez",
            correo = "test@example.com",
            roles = listOf("Consumidor")
        )
        coEvery { loginUseCase("test@example.com", "secret123") } returns Result.Success(mockProfile)

        val viewModel = LoginViewModel(loginUseCase, startGuestSessionUseCase)
        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("test@example.com"))
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("secret123"))
        viewModel.dispatchIntent(LoginViewIntent.SubmitLogin)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.viewState.value.isLoading)
    }

    @Test
    fun submitLogin_failure_updatesErrorState() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.Failure(AppError.Validation("Credenciales invalidas"))

        val viewModel = LoginViewModel(loginUseCase, startGuestSessionUseCase)
        viewModel.dispatchIntent(LoginViewIntent.EmailChanged("test@example.com"))
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged("wrong"))
        viewModel.dispatchIntent(LoginViewIntent.SubmitLogin)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Credenciales invalidas", viewModel.viewState.value.errorMessage)
        assertFalse(viewModel.viewState.value.isLoading)
    }

    @Test
    fun startGuestSession_success_emitsGuestEffect() = runTest {
        coEvery { startGuestSessionUseCase() } returns Result.Success("token")

        val viewModel = LoginViewModel(loginUseCase, startGuestSessionUseCase)
        viewModel.dispatchIntent(LoginViewIntent.StartGuestSession)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.viewState.value.isGuestLoading)
    }
}
