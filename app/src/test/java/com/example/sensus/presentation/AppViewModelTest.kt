package com.example.sensus.presentation

import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase
    private val sessionFlow = MutableStateFlow(SessionState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        getPreferredMapLayerUseCase = mockk(relaxed = true)
        coEvery { sessionRepository.sessionState } returns sessionFlow
        coEvery { getPreferredMapLayerUseCase() } returns MapLayer.STANDARD
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_prewarmsPreferredMapLayerUseCase() = runTest {
        AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { getPreferredMapLayerUseCase() }
    }

    @Test
    fun invite_resolvesFriendsRepositoryOnlyWhenNeeded() = runTest {
        val friends = mockk<FriendsRepository>(relaxed = true)
        val provider = mockk<Lazy<FriendsRepository>>()
        every { provider.get() } returns friends
        sessionFlow.value = SessionState(isSessionActive = true, hasAcceptedTerms = true)
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase, provider)
        viewModel.ioDispatcher = testDispatcher
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 0) { provider.get() }

        val invite = "sensus://amigos"
        viewModel.dispatchIntent(AppViewIntent.FriendInviteDeepLinkReceived(invite))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { provider.get() }
        coVerify(exactly = 1) { friends.queuePendingInvite(invite) }
        assertEquals(AppScreen.MAIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun splashFinished_navigatesToOnboarding_whenNoLocationPermission() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.setSplashCompleted(true) }
        assertEquals(AppScreen.ONBOARDING, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun splashFinished_navigatesToLogin_whenLocationIsAlreadyReady() = runTest(testDispatcher) {
        sessionFlow.value = SessionState(hasLocationPermission = true)
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun pendingInviteKeepsAnUnauthenticatedUserOnLoginAfterSplash() = runTest(testDispatcher) {
        val friends = mockk<FriendsRepository>(relaxed = true)
        val provider = mockk<Lazy<FriendsRepository>>()
        every { provider.get() } returns friends
        sessionFlow.value = SessionState(hasLocationPermission = true)
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase, provider)
        viewModel.ioDispatcher = testDispatcher

        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()
        val invite = "sensus://amigos"
        viewModel.dispatchIntent(AppViewIntent.FriendInviteDeepLinkReceived(invite))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { friends.queuePendingInvite(invite) }
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun splashFinished_resumesTermsForAuthenticatedAccount() = runTest(testDispatcher) {
        sessionFlow.value = SessionState(isSessionActive = true)
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppScreen.TERMS, viewModel.viewState.value.currentScreen)

        viewModel.dispatchIntent(AppViewIntent.TermsAccepted(true))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { sessionRepository.setTermsAccepted(true) }
        assertEquals(AppScreen.MAIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun deepLinkReceived_navigatesToForgotPassword() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.DeepLinkReceived("token123"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppScreen.FORGOT_PASSWORD, viewModel.viewState.value.currentScreen)
        assertEquals("token123", viewModel.viewState.value.activeResetToken)
    }

    @Test
    fun locationPermissionSet_navigatesToLogin() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.LocationPermissionSet(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.setLocationPermission(true) }
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun logout_clearsSession_andNavigatesToLogin() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { logoutUseCase() }
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun sessionDroppedInMain_redirectsToLogin() = runTest {
        sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, hasAcceptedTerms = true)
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
        assertEquals(AppScreen.MAIN, viewModel.viewState.value.currentScreen)

        sessionFlow.value = SessionState(isSessionActive = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppScreen.LOGIN, viewModel.viewState.value.currentScreen)
    }

    @Test
    fun sessionUpdate_propagatesIsMerchantRegisteredFlag() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.viewState.value.isMerchantRegistered)

        sessionFlow.value = SessionState(isSessionActive = true, isMerchantRegistered = true, merchantStoreName = "Café Sol")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.viewState.value.isMerchantRegistered)
    }
}
