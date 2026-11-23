package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.network.NetworkMonitor
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.sync.SyncManager
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewEffect
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertTrue
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
    fun splashFinished_navigatesToOnboarding_whenNoLocationPermission() = runTest {
        val viewModel = AppViewModel(sessionRepository, logoutUseCase, getPreferredMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dispatchIntent(AppViewIntent.SplashFinished)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.setSplashCompleted(true) }
        assertEquals(AppScreen.ONBOARDING, viewModel.viewState.value.currentScreen)
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

    @Test
    fun networkTransition_offline_emitsSnackbarEffectAndUpdatesState() = runTest {
        val networkMonitor = mockk<NetworkMonitor>()
        val syncManager = mockk<SyncManager>(relaxed = true)
        val networkFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns networkFlow
        every { syncManager.pendingOperationsCount } returns MutableStateFlow(0)

        val viewModel = AppViewModel(
            sessionRepository = sessionRepository,
            logoutUseCase = logoutUseCase,
            getPreferredMapLayerUseCase = getPreferredMapLayerUseCase,
            networkMonitor = networkMonitor,
            syncManager = syncManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffect.test {
            networkFlow.value = false
            testDispatcher.scheduler.advanceTimeBy(350)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is AppViewEffect.ShowNetworkSnackbar)
            assertEquals("Sin conexión a internet. Mostrando datos guardados.", (effect as AppViewEffect.ShowNetworkSnackbar).message)
            assertEquals(false, viewModel.viewState.value.isOnline)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun networkTransition_reconnected_emitsSnackbarEffectAndTriggersSync() = runTest {
        val networkMonitor = mockk<NetworkMonitor>()
        val syncManager = mockk<SyncManager>(relaxed = true)
        val networkFlow = MutableStateFlow(false)
        every { networkMonitor.isOnline } returns networkFlow
        every { syncManager.pendingOperationsCount } returns MutableStateFlow(0)

        val viewModel = AppViewModel(
            sessionRepository = sessionRepository,
            logoutUseCase = logoutUseCase,
            getPreferredMapLayerUseCase = getPreferredMapLayerUseCase,
            networkMonitor = networkMonitor,
            syncManager = syncManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffect.test {
            networkFlow.value = true
            testDispatcher.scheduler.advanceTimeBy(350)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is AppViewEffect.ShowNetworkSnackbar)
            assertEquals("Conexión restablecida.", (effect as AppViewEffect.ShowNetworkSnackbar).message)
            assertEquals(true, viewModel.viewState.value.isOnline)

            coVerify(atLeast = 1) { syncManager.syncPendingOperations() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun networkFlicker_rapidChanges_areDebouncedWithoutMultipleSnackbars() = runTest {
        val networkMonitor = mockk<NetworkMonitor>()
        val syncManager = mockk<SyncManager>(relaxed = true)
        val networkFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns networkFlow
        every { syncManager.pendingOperationsCount } returns MutableStateFlow(0)

        val viewModel = AppViewModel(
            sessionRepository = sessionRepository,
            logoutUseCase = logoutUseCase,
            getPreferredMapLayerUseCase = getPreferredMapLayerUseCase,
            networkMonitor = networkMonitor,
            syncManager = syncManager
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffect.test {
            networkFlow.value = false
            testDispatcher.scheduler.advanceTimeBy(100)
            networkFlow.value = true
            testDispatcher.scheduler.advanceTimeBy(100)
            networkFlow.value = false
            testDispatcher.scheduler.advanceTimeBy(350)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is AppViewEffect.ShowNetworkSnackbar)
            assertEquals("Sin conexión a internet. Mostrando datos guardados.", (effect as AppViewEffect.ShowNetworkSnackbar).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun syncManager_pendingCountFlow_updatesPendingSyncCountInState() = runTest {
        val syncManager = mockk<SyncManager>(relaxed = true)
        val pendingCountFlow = MutableStateFlow(0)
        every { syncManager.pendingOperationsCount } returns pendingCountFlow

        val viewModel = AppViewModel(
            sessionRepository = sessionRepository,
            logoutUseCase = logoutUseCase,
            getPreferredMapLayerUseCase = getPreferredMapLayerUseCase,
            syncManager = syncManager
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.viewState.value.pendingSyncCount)

        pendingCountFlow.value = 4
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(4, viewModel.viewState.value.pendingSyncCount)
    }
}
