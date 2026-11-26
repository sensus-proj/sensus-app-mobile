package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsDeviceSyncTest {
    private val registered = SessionState(isSessionActive = true, activeToken = "session-fixture")
    private val sessionState = MutableStateFlow(SessionState())
    private val session = mockk<SessionRepository> { every { sessionState } returns this@NotificationsDeviceSyncTest.sessionState }
    private val register = mockk<NotificationsRegisterDeviceUseCase>()
    private val permission = mockk<NotificationPermissionChecker> { every { canPostNotifications() } returns true }
    private val unavailable = Result.Failure(AppError.Generic(NotificationsRepository.PUSH_UNAVAILABLE))

    private fun TestScope.sync() = NotificationsDeviceSync(register, session, permission, backgroundScope, listOf(1_000L, 2_000L))

    /** Las corrutinas de `backgroundScope` no cuentan para `advanceUntilIdle`; se avanza el reloj virtual. */
    private fun TestScope.settle() {
        advanceTimeBy(60_000L)
        runCurrent()
    }

    @Test
    fun registersOnceWhenARegisteredSessionStarts() = runTest {
        coEvery { register(null) } returns Result.Success(Unit)
        sync().start()
        runCurrent()
        coVerify(exactly = 0) { register(any()) }
        sessionState.value = registered
        runCurrent()
        sessionState.value = registered.copy(activeName = "same user, same token")
        settle()
        coVerify(exactly = 1) { register(null) }
    }

    @Test
    fun guestsAndMissingPermissionNeverRegister() = runTest {
        val sync = sync()
        sync.start()
        sessionState.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-fixture")
        settle()
        sessionState.value = registered
        every { permission.canPostNotifications() } returns false
        sync.sync("token-fixture")
        settle()
        coVerify(exactly = 0) { register(any()) }
    }

    @Test
    fun serviceUnavailableIsRetriedUntilSuccess() = runTest {
        sessionState.value = registered
        coEvery { register("token-fixture") } returnsMany listOf(unavailable, Result.Failure(AppError.Network()), Result.Success(Unit))
        sync().sync("token-fixture")
        runCurrent()
        coVerify(exactly = 1) { register("token-fixture") }
        advanceTimeBy(1_001L)
        coVerify(exactly = 2) { register("token-fixture") }
        settle()
        coVerify(exactly = 3) { register("token-fixture") }
    }

    @Test
    fun validationOrSessionErrorsAreNotRetried() = runTest {
        sessionState.value = registered
        coEvery { register(null) } returns Result.Failure(AppError.Validation())
        sync().sync()
        settle()
        coVerify(exactly = 1) { register(null) }
    }

    @Test
    fun retriesStopAfterTheConfiguredAttempts() = runTest {
        sessionState.value = registered
        coEvery { register(null) } returns unavailable
        sync().sync()
        settle()
        coVerify(exactly = 3) { register(null) }
    }

    @Test
    fun logoutCancelsPendingRetries() = runTest {
        coEvery { register(null) } returns unavailable
        val sync = sync()
        sync.start()
        sessionState.value = registered
        runCurrent()
        sessionState.value = SessionState()
        settle()
        coVerify(exactly = 1) { register(null) }
    }
}
