package com.example.sensus.presentation

import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import com.example.sensus.presentation.buyer.NotificationsViewIntent
import com.example.sensus.presentation.buyer.NotificationsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesConcurrencyTest {
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
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pendingLoadRejectsRetriesAndEditsThenPendingSaveRejectsStaleMutations() = runTest(dispatcher) {
        val load = CompletableDeferred<Result<NotificationsPreferences>>()
        val save = CompletableDeferred<Result<NotificationsPreferences>>()
        val draft = settings.copy(radiusKm = 20)
        coEvery { repository.getSettings() } coAnswers { load.await() }
        coEvery { repository.updateSettings(draft) } coAnswers { save.await() }
        val vm = NotificationsViewModel(
            NotificationsGetSettingsUseCase(repository, session),
            NotificationsUpdateSettingsUseCase(repository, session),
            NotificationsRegisterDeviceUseCase(provider, repository, session, permission),
            io.mockk.mockk<com.example.sensus.domain.usecase.NotificationsLocationUseCase>(relaxed = true)
        )
        vm.processIntent(NotificationsViewIntent.Retry)
        vm.processIntent(NotificationsViewIntent.Edit(draft))
        runCurrent()
        coVerify(exactly = 1) { repository.getSettings() }
        assertEquals(null, vm.viewState.value.preferences)
        load.complete(Result.Success(settings))
        advanceUntilIdle()
        vm.processIntent(NotificationsViewIntent.Edit(draft))
        vm.processIntent(NotificationsViewIntent.Save)
        vm.processIntent(NotificationsViewIntent.Save)
        runCurrent()
        vm.processIntent(NotificationsViewIntent.Edit(settings))
        vm.processIntent(NotificationsViewIntent.Retry)
        runCurrent()
        assertEquals(draft, vm.viewState.value.preferences)
        coVerify(exactly = 1) { repository.updateSettings(draft) }
        coVerify(exactly = 1) { repository.getSettings() }
        save.complete(Result.Success(draft))
        advanceUntilIdle()
        assertEquals(draft, vm.viewState.value.savedPreferences)
        assertFalse(vm.viewState.value.isSaving)
    }

    @Test
    fun logoutClearsLoadedPreferencesAndDraft() = runTest(dispatcher) {
        coEvery { repository.getSettings() } returns Result.Success(settings)
        val vm = NotificationsViewModel(
            NotificationsGetSettingsUseCase(repository, session),
            NotificationsUpdateSettingsUseCase(repository, session),
            NotificationsRegisterDeviceUseCase(provider, repository, session, permission),
            io.mockk.mockk<com.example.sensus.domain.usecase.NotificationsLocationUseCase>(relaxed = true)
        )
        advanceUntilIdle()
        assertEquals(settings, vm.viewState.value.preferences)
        (session.sessionState as MutableStateFlow<SessionState>).value = SessionState()
        advanceUntilIdle()
        assertEquals(null, vm.viewState.value.preferences)
        assertEquals(null, vm.viewState.value.savedPreferences)
        assertFalse(vm.viewState.value.canSave)
    }
}
