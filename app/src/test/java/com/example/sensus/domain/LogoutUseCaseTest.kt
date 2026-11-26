package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        useCase = LogoutUseCase(authRepository, sessionRepository)
    }

    @Test
    fun invoke_callsRemoteLogoutAndClearsLocalSessionInOrder() = runTest {
        coEvery { authRepository.logout() } returns Result.Success(Unit)

        val result = useCase()

        assertTrue(result is Result.Success)
        coVerifyOrder {
            authRepository.logout()
            sessionRepository.clearLocalSession()
        }
    }

    @Test
    fun invoke_clearsLocalSessionEvenWhenRemoteLogoutFails() = runTest {
        coEvery { authRepository.logout() } returns Result.Failure(AppError.Network("Sin red"))

        val result = useCase()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { sessionRepository.clearLocalSession() }
    }

    @Test
    fun invoke_clearsSessionThroughDefaultRepositoryImplementation() = runTest {
        val repository = object : SessionRepository by sessionRepository {
            override suspend fun clearLocalSession() = super<SessionRepository>.clearLocalSession()
        }
        coEvery { authRepository.logout() } returns Result.Success(Unit)

        assertTrue(LogoutUseCase(authRepository, repository)() is Result.Success)
        coVerify(exactly = 1) { sessionRepository.clearSession() }
    }

    @Test
    fun invoke_clearsLocalSessionEvenWhenRemoteLogoutThrows() = runTest {
        coEvery { authRepository.logout() } throws IOException("Timeout")

        val result = useCase()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { sessionRepository.clearLocalSession() }
    }

    @Test
    fun invoke_sendsInstallationDeviceIdAndForgetsUserScopedPushData() = runTest {
        var cleared = false
        val withDevice = LogoutUseCase(authRepository, sessionRepository, { "install-uuid-fixture" }, { cleared = true })
        coEvery { authRepository.logout("install-uuid-fixture") } returns Result.Success(Unit)

        withDevice()

        coVerifyOrder {
            authRepository.logout("install-uuid-fixture")
            sessionRepository.clearLocalSession()
        }
        assertTrue(cleared)
    }

    @Test
    fun invoke_stillLogsOutWhenDeviceIdIsUnavailable() = runTest {
        val failingDevice = LogoutUseCase(authRepository, sessionRepository, { error("store unavailable") })

        assertTrue(failingDevice() is Result.Success)
        coVerify(exactly = 1) { authRepository.logout(null) }
        coVerify(exactly = 1) { sessionRepository.clearLocalSession() }
    }
}
