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

class NotificationPreferencesUseCasesTest {
    private val repository = mockk<NotificationsRepository>()
    private val provider = mockk<NotificationsPushDeviceProvider>()
    private val permission = mockk<NotificationPermissionChecker> { every { canPostNotifications() } returns true }
    private val session = mockk<SessionRepository> {
        every { sessionState } returns MutableStateFlow(
            SessionState(isSessionActive = true, activeToken = java.util.UUID.randomUUID().toString())
        )
    }

    @Test
    fun updateRejectsOutOfRangeWithoutNetwork() = runTest {
        val useCase = NotificationsUpdateSettingsUseCase(repository, session)
        for (radius in listOf(0, 51)) {
            assertEquals(Result.Failure(AppError.Validation()), useCase(NotificationsPreferences(true, true, false, radius)))
        }
        coVerify(exactly = 0) { repository.updateSettings(any()) }
    }

    @Test
    fun boundaryValuesAndGetPreserveServerResponse() = runTest {
        val settings = NotificationsPreferences(false, true, false, 50)
        coEvery { repository.getSettings() } returns Result.Success(settings)
        coEvery { repository.updateSettings(any()) } returns Result.Success(settings)
        assertEquals(Result.Success(settings), NotificationsGetSettingsUseCase(repository, session)())
        for (radius in listOf(1, 50)) {
            assertEquals(Result.Success(settings), NotificationsUpdateSettingsUseCase(repository, session)(settings.copy(radiusKm = radius)))
        }
    }

    @Test
    fun blockedAndroidNotificationsNeverReachFirebaseOrRegistration() = runTest {
        every { permission.canPostNotifications() } returns false
        val useCase = NotificationsRegisterDeviceUseCase(provider, repository, session, permission)
        val expected = Result.Failure(AppError.Forbidden(NotificationsRegisterDeviceUseCase.PERMISSION_REQUIRED))

        assertEquals(expected, useCase())
        assertEquals(expected, useCase("refreshed-sdk-token"))
        coVerify(exactly = 0) { provider.getDevice(any()) }
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun permissionRevokedWhileGettingFirebaseTokenPreventsRegistration() = runTest {
        every { permission.canPostNotifications() } returns true andThen false
        coEvery { provider.getDevice() } returns Result.Success(NotificationsDevice("installation-id", "sdk-token-from-provider"))

        val result = NotificationsRegisterDeviceUseCase(provider, repository, session, permission)()

        assertEquals(Result.Failure(AppError.Forbidden(NotificationsRegisterDeviceUseCase.PERMISSION_REQUIRED)), result)
        coVerify(exactly = 1) { provider.getDevice() }
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun missingFirebaseConfigurationNeverRegistersDevice() = runTest {
        val failure = Result.Failure(AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE))
        coEvery { provider.getDevice() } returns failure
        assertEquals(failure, NotificationsRegisterDeviceUseCase(provider, repository, session, permission)())
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun realProviderCredentialsAreForwardedAndFailureIsPreserved() = runTest {
        val device = NotificationsDevice("installation-id", "sdk-token-from-provider")
        val failure = Result.Failure(AppError.Unauthorized())
        coEvery { provider.getDevice() } returns Result.Success(device)
        coEvery { repository.registerDevice(device) } returns failure
        assertEquals(failure, NotificationsRegisterDeviceUseCase(provider, repository, session, permission)())
        coVerify(exactly = 1) { repository.registerDevice(device) }
    }
}
