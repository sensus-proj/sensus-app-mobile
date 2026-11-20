package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import com.example.sensus.presentation.buyer.NotificationsError
import com.example.sensus.presentation.buyer.NotificationsLocationIntent
import com.example.sensus.presentation.buyer.NotificationsViewEffect
import com.example.sensus.presentation.buyer.NotificationsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
class NotificationsLocationToggleTest {
    private val dispatcher = StandardTestDispatcher()
    private val getSettings = mockk<NotificationsGetSettingsUseCase> {
        every { sessionChanges } returns emptyFlow()
        coEvery { this@mockk.invoke() } returns Result.Success(NotificationsPreferences(true, true, true, 5))
    }
    private val location = mockk<NotificationsLocationUseCase>()
    private var enabled = false

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { location.isEnabled() } answers { enabled }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NotificationsViewModel(
        getSettings,
        mockk<NotificationsUpdateSettingsUseCase>(),
        mockk<NotificationsRegisterDeviceUseCase>(),
        location
    )

    @Test
    fun enablingAsksConsentThenPermissionThenShares() = runTest(dispatcher) {
        coEvery { location.enable() } answers {
            enabled = true
            Result.Success(Unit)
        }
        val vm = viewModel()
        advanceUntilIdle()
        vm.processIntent(NotificationsLocationIntent.Toggled(true))
        advanceUntilIdle()
        assertTrue(vm.viewState.value.showLocationConsent)
        coVerify(exactly = 0) { location.enable() }

        vm.viewEffect.test {
            vm.processIntent(NotificationsLocationIntent.ConsentAnswered(true))
            assertEquals(NotificationsViewEffect.RequestLocationPermission, awaitItem())
        }
        assertFalse(vm.viewState.value.showLocationConsent)

        vm.processIntent(NotificationsLocationIntent.PermissionResult(true))
        advanceUntilIdle()
        assertTrue(vm.viewState.value.locationSharingEnabled)
        assertFalse(vm.viewState.value.isUpdatingLocation)
    }

    @Test
    fun declinedConsentOrPermissionNeverShares() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.processIntent(NotificationsLocationIntent.Toggled(true))
        vm.processIntent(NotificationsLocationIntent.ConsentAnswered(false))
        vm.processIntent(NotificationsLocationIntent.PermissionResult(false))
        advanceUntilIdle()
        assertFalse(vm.viewState.value.showLocationConsent)
        assertEquals(NotificationsError.LOCATION_PERMISSION, vm.viewState.value.error)
        coVerify(exactly = 0) { location.enable() }
    }

    @Test
    fun disablingDeletesAndMissingGpsIsReported() = runTest(dispatcher) {
        enabled = true
        coEvery { location.disable() } answers {
            enabled = false
            Result.Success(Unit)
        }
        coEvery { location.enable() } returns Result.Failure(AppError.NotFound(NotificationsLocationUseCase.LOCATION_UNAVAILABLE))
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.viewState.value.locationSharingEnabled)
        vm.processIntent(NotificationsLocationIntent.Toggled(false))
        advanceUntilIdle()
        assertFalse(vm.viewState.value.locationSharingEnabled)
        coVerify(exactly = 1) { location.disable() }

        vm.processIntent(NotificationsLocationIntent.PermissionResult(true))
        advanceUntilIdle()
        assertEquals(NotificationsError.LOCATION_UNAVAILABLE, vm.viewState.value.error)
    }
}
