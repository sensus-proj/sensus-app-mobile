package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences
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
    fun missingFirebaseConfigurationNeverRegistersDevice() = runTest {
        val failure = Result.Failure(AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE))
        coEvery { provider.getDevice() } returns failure
        assertEquals(failure, NotificationsRegisterDeviceUseCase(provider, repository, session)())
        coVerify(exactly = 0) { repository.registerDevice(any()) }
    }

    @Test
    fun realProviderCredentialsAreForwardedAndFailureIsPreserved() = runTest {
        val device = NotificationsDevice("installation-id", "sdk-token-from-provider")
        val failure = Result.Failure(AppError.Unauthorized())
        coEvery { provider.getDevice() } returns Result.Success(device)
        coEvery { repository.registerDevice(device) } returns failure
        assertEquals(failure, NotificationsRegisterDeviceUseCase(provider, repository, session)())
        coVerify(exactly = 1) { repository.registerDevice(device) }
    }
}
