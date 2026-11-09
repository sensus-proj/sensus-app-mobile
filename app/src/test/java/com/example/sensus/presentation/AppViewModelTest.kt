package com.example.sensus.presentation

import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepository: SessionRepository
    private val sessionFlow = MutableStateFlow(SessionState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        coEvery { sessionRepository.sessionState } returns sessionFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun splashFinished_navigatesToOnboarding_whenNoLocationPermission() = runTest {
        val viewModel = AppViewModel(sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.setSplashCompleted(true) }
        assertEquals(AppScreen.ONBOARDING, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun deepLinkReceived_navigatesToForgotPassword() = runTest {
        val viewModel = AppViewModel(sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.DeepLinkReceived("token123"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppScreen.FORGOT_PASSWORD, viewModel.viewState.value.currentScreen)
        assertEquals("token123", viewModel.viewState.value.activeResetToken)
    }

    @Test
    fun locationPermissionSet_navigatesToLogin() = runTest {
        val viewModel = AppViewModel(sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.LocationPermissionSet(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.setLocationPermission(true) }
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun logout_clearsSession_andNavigatesToLogin() = runTest {
        val viewModel = AppViewModel(sessionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.clearSession() }
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }
}
