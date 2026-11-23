package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import com.example.sensus.presentation.buyer.NotificationsError
import com.example.sensus.presentation.buyer.NotificationsViewEffect
import com.example.sensus.presentation.buyer.NotificationsViewIntent
import com.example.sensus.presentation.buyer.NotificationsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class NotificationPreferencesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<NotificationsRepository>()
    private val provider = mockk<NotificationsPushDeviceProvider>()
    private val permission = mockk<NotificationPermissionChecker> { every { canPostNotifications() } returns true }
    private val session = mockk<SessionRepository> {
        every { sessionState } returns MutableStateFlow(
            SessionState(isSessionActive = true, activeToken = java.util.UUID.randomUUID().toString())
        )
    }
    private val settings = NotificationsPreferences(true, true, false, 5)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getSettings() } returns Result.Success(settings)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NotificationsViewModel(
        NotificationsGetSettingsUseCase(repository, session),
        NotificationsUpdateSettingsUseCase(repository, session),
        NotificationsRegisterDeviceUseCase(provider, repository, session, permission),
        io.mockk.mockk<com.example.sensus.domain.usecase.NotificationsLocationUseCase>(relaxed = true)
    )

    @Test
    fun saveFailurePreservesDraftAndSuccessUsesServerResponse() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(settings, vm.viewState.value.preferences)
        assertFalse(vm.viewState.value.canSave)
        val draft = settings.copy(radiusKm = 20)
        coEvery { repository.updateSettings(draft) } returns Result.Failure(AppError.Network())
        vm.processIntent(NotificationsViewIntent.Edit(draft))
        vm.processIntent(NotificationsViewIntent.Save)
        vm.processIntent(NotificationsViewIntent.Save)
        advanceUntilIdle()
        assertEquals(draft, vm.viewState.value.preferences)
        assertEquals(settings, vm.viewState.value.savedPreferences)
        assertEquals(NotificationsError.NETWORK, vm.viewState.value.error)
        assertTrue(vm.viewState.value.canSave)
        coVerify(exactly = 1) { repository.updateSettings(draft) }
        coEvery { repository.updateSettings(draft) } returns Result.Success(settings.copy(radiusKm = 19))
        vm.processIntent(NotificationsViewIntent.Save)
        advanceUntilIdle()
        assertEquals(19, vm.viewState.value.preferences?.radiusKm)
        assertFalse(vm.viewState.value.canSave)
        assertEquals(NotificationsViewEffect.Saved, vm.viewEffect.first())
    }

    @Test
    fun unauthorizedLoadHasNoDefaultsAndCanRetry() = runTest(dispatcher) {
        coEvery { repository.getSettings() } returns Result.Failure(AppError.Unauthorized())
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(null, vm.viewState.value.preferences)
        assertEquals(NotificationsError.SESSION, vm.viewState.value.error)
        coEvery { repository.getSettings() } returns Result.Success(settings)
        vm.processIntent(NotificationsViewIntent.Retry)
        advanceUntilIdle()
        assertEquals(settings, vm.viewState.value.preferences)
    }

    @Test
    fun permissionDeniedNeverCallsSdkAndMissingConfigurationNeverRegisters() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.processIntent(NotificationsViewIntent.PermissionDenied)
        advanceUntilIdle()
        assertEquals(NotificationsError.PERMISSION, vm.viewState.value.error)
        coVerify(exactly = 0) { provider.getDevice() }
        coEvery { provider.getDevice() } returns Result.Failure(AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE))
        vm.processIntent(NotificationsViewIntent.ActivateDevice)
        vm.processIntent(NotificationsViewIntent.ActivateDevice)
        advanceUntilIdle()
        assertEquals(NotificationsError.FIREBASE, vm.viewState.value.error)
        assertFalse(vm.viewState.value.isRegistering)
        coVerify(exactly = 1) { provider.getDevice() }
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun blockedAndroidNotificationsShowPermissionErrorAndKeepPreferences() = runTest(dispatcher) {
        every { permission.canPostNotifications() } returns false
        val vm = viewModel()
        advanceUntilIdle()

        vm.processIntent(NotificationsViewIntent.ActivateDevice)
        advanceUntilIdle()

        assertEquals(NotificationsError.PERMISSION, vm.viewState.value.error)
        assertEquals(settings, vm.viewState.value.savedPreferences)
        assertFalse(vm.viewState.value.isRegistering)
        coVerify(exactly = 0) { provider.getDevice(any()) }
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun successfulRegistrationEmitsConfirmationWithoutChangingPreferences() = runTest(dispatcher) {
        val device = NotificationsDevice("installation-from-sdk", "token-issued-by-sdk-fixture")
        coEvery { provider.getDevice() } returns Result.Success(device)
        coEvery { repository.registerDevice(device) } returns Result.Success(Unit)
        val vm = viewModel()
        advanceUntilIdle()
        vm.processIntent(NotificationsViewIntent.ActivateDevice)
        advanceUntilIdle()
        assertEquals(NotificationsViewEffect.DeviceRegistered, vm.viewEffect.first())
        assertEquals(settings, vm.viewState.value.preferences)
        assertFalse(vm.viewState.value.isRegistering)
        coVerify(exactly = 1) { repository.registerDevice(device) }
    }
}
