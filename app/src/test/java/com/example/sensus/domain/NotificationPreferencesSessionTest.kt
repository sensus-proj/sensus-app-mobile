package com.example.sensus.domain

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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPreferencesSessionTest {
    private val repository = mockk<NotificationsRepository>()
    private val provider = mockk<NotificationsPushDeviceProvider>()
    private val permission = mockk<NotificationPermissionChecker> { every { canPostNotifications() } returns true }
    private val state = MutableStateFlow(SessionState())
    private val session = mockk<SessionRepository> { every { sessionState } returns state }
    private val settings = NotificationsPreferences(true, true, false, 5)
    private val unauthorized = Result.Failure(AppError.Unauthorized())

    @Test
    fun guestInactiveAndMissingTokenNeverReachPrivateApiOrSdk() = runTest {
        for (invalid in listOf(
            SessionState(),
            SessionState(isSessionActive = true),
            SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-token")
        )) {
            state.value = invalid
            assertEquals(unauthorized, NotificationsGetSettingsUseCase(repository, session)())
            assertEquals(unauthorized, NotificationsUpdateSettingsUseCase(repository, session)(settings))
            assertEquals(unauthorized, NotificationsRegisterDeviceUseCase(provider, repository, session, permission)())
        }
        coVerify(exactly = 0) { repository.getSettings() }
        coVerify(exactly = 0) { repository.updateSettings(any()) }
        coVerify(exactly = 0) { provider.getDevice() }
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun logoutDuringReadDiscardsPrivateSettings() = runTest {
        state.value = SessionState(isSessionActive = true, activeToken = "account-a")
        coEvery { repository.getSettings() } coAnswers {
            state.value = SessionState()
            Result.Success(settings)
        }
        assertEquals(unauthorized, NotificationsGetSettingsUseCase(repository, session)())
    }

    @Test
    fun accountSwitchDuringWriteDiscardsOldAccountResponse() = runTest {
        state.value = SessionState(isSessionActive = true, activeToken = "account-a")
        coEvery { repository.updateSettings(settings) } coAnswers {
            state.value = SessionState(isSessionActive = true, activeToken = "account-b")
            Result.Success(settings)
        }
        assertEquals(unauthorized, NotificationsUpdateSettingsUseCase(repository, session)(settings))
    }

    @Test
    fun logoutDuringSdkAcquisitionPreventsRegistration() = runTest {
        state.value = SessionState(isSessionActive = true, activeToken = "account-a")
        coEvery { provider.getDevice() } coAnswers {
            state.value = SessionState()
            Result.Success(NotificationsDevice("sdk-installation", "sdk-device-token"))
        }
        assertEquals(unauthorized, NotificationsRegisterDeviceUseCase(provider, repository, session, permission)())
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }
}
