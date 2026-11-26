package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsLocationUseCaseTest {
    private val registered = SessionState(isSessionActive = true, activeToken = "session-fixture", activeEmail = "a@b.c")
    private val sessionState = MutableStateFlow(registered)
    private val session = mockk<SessionRepository> { every { sessionState } returns this@NotificationsLocationUseCaseTest.sessionState }
    private val repository = mockk<NotificationsRepository>()
    private val location = mockk<LocationRepository>()
    private val store = FakeStore()
    private var now = 100_000_000L
    private val point = UserLocation(19.4326, -99.1332)

    private fun useCase() = NotificationsLocationUseCase(repository, session, location, store) { now }

    @Test
    fun refreshDoesNothingWithoutConsentOrPermission() = runTest {
        every { location.hasLocationPermission() } returns true
        assertEquals(Result.Success(Unit), useCase().refreshIfDue())
        store.enabled = true
        every { location.hasLocationPermission() } returns false
        assertEquals(Result.Success(Unit), useCase().refreshIfDue())
        coVerify(exactly = 0) { repository.shareLocation(any()) }
    }

    @Test
    fun refreshSharesWhenDueAndSkipsWhenRecent() = runTest {
        store.enabled = true
        every { location.hasLocationPermission() } returns true
        coEvery { location.getFreshLocation() } returns point
        coEvery { repository.shareLocation(point) } returns Result.Success(Unit)
        assertEquals(Result.Success(Unit), useCase().refreshIfDue())
        assertEquals(now, store.sharedAt)
        now += NotificationsLocationUseCase.REFRESH_INTERVAL_MILLIS - 1
        useCase().refreshIfDue()
        coVerify(exactly = 1) { repository.shareLocation(point) }
        now += 1
        useCase().refreshIfDue()
        coVerify(exactly = 2) { repository.shareLocation(point) }
    }

    @Test
    fun defaultFallbackLocationIsNeverSent() = runTest {
        store.enabled = true
        every { location.hasLocationPermission() } returns true
        coEvery { location.getFreshLocation() } returns UserLocation()
        val result = useCase().refreshIfDue()
        assertEquals(Result.Failure(AppError.NotFound(NotificationsLocationUseCase.LOCATION_UNAVAILABLE)), result)
        coVerify(exactly = 0) { repository.shareLocation(any()) }
        assertEquals(0L, store.sharedAt)
    }

    @Test
    fun enableRequiresPermissionAndRegisteredSession() = runTest {
        every { location.hasLocationPermission() } returns false
        assertEquals(Result.Failure(AppError.Forbidden(NotificationsLocationUseCase.PERMISSION_REQUIRED)), useCase().enable())
        every { location.hasLocationPermission() } returns true
        sessionState.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-fixture")
        assertEquals(Result.Failure(AppError.Unauthorized()), useCase().enable())
        assertFalse(store.enabled)
        coVerify(exactly = 0) { repository.shareLocation(any()) }
    }

    @Test
    fun enableStoresConsentAndShares() = runTest {
        every { location.hasLocationPermission() } returns true
        coEvery { location.getFreshLocation() } returns point
        coEvery { repository.shareLocation(point) } returns Result.Success(Unit)
        assertEquals(Result.Success(Unit), useCase().enable())
        assertTrue(store.enabled)
        assertTrue(useCase().isEnabled())
    }

    @Test
    fun disableClearsLocallyAndDeletesOnServer() = runTest {
        store.enabled = true
        store.sharedAt = 5L
        coEvery { repository.deleteLocation() } returns Result.Success(Unit)
        assertEquals(Result.Success(Unit), useCase().disable())
        assertFalse(store.enabled)
        assertEquals(0L, store.sharedAt)
        coVerify(exactly = 1) { repository.deleteLocation() }
    }

    @Test
    fun guestNeverReachesTheServer() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-fixture")
        store.enabled = true
        every { location.hasLocationPermission() } returns true
        coEvery { location.getFreshLocation() } returns point
        assertEquals(Result.Failure(AppError.Unauthorized()), useCase().refreshIfDue())
        assertEquals(Result.Failure(AppError.Unauthorized()), useCase().disable())
        coVerify(exactly = 0) { repository.shareLocation(any()) }
        coVerify(exactly = 0) { repository.deleteLocation() }
    }

    private class FakeStore : NotificationsLocalStore {
        var enabled = false
        var sharedAt = 0L
        private val prompts = mutableSetOf<NotificationsPrompt>()
        override fun installationId() = "installation-fixture"
        override fun isLocationSharingEnabled() = enabled
        override fun setLocationSharingEnabled(enabled: Boolean) {
            this.enabled = enabled
        }
        override fun lastLocationSharedAtMillis() = sharedAt
        override fun setLastLocationSharedAtMillis(value: Long) {
            sharedAt = value
        }
        override fun wasPromptShown(prompt: NotificationsPrompt) = prompt in prompts
        override fun markPromptShown(prompt: NotificationsPrompt) {
            prompts += prompt
        }
        override fun clearUserScoped() {
            enabled = false
            sharedAt = 0L
        }
    }
}
