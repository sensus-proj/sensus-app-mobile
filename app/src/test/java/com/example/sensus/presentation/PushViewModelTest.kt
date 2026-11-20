package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.PushTarget
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.presentation.push.PushOpenRequests
import com.example.sensus.presentation.push.PushViewEffect
import com.example.sensus.presentation.push.PushViewIntent
import com.example.sensus.presentation.push.PushViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val requests = PushOpenRequests()
    private val getOffer = mockk<GetOfferDetailUseCase>()
    private val location = mockk<NotificationsLocationUseCase>(relaxed = true)
    private val deviceSync = mockk<NotificationsDeviceSync> { every { sync(any()) } just runs }
    private val prompts = mutableSetOf<NotificationsPrompt>()
    private val store = mockk<NotificationsLocalStore> {
        every { wasPromptShown(any()) } answers { firstArg<NotificationsPrompt>() in prompts }
        every { markPromptShown(any()) } answers { prompts += firstArg<NotificationsPrompt>() }
    }
    private val offer = ProductOffer("offer-1", "Pan", "Panaderia", "Panaderia", 30.0, 15.0, 50, 3, 200)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { location.isEnabled() } returns false
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(bus: PushOpenRequests = requests) = PushViewModel(bus, getOffer, location, store, deviceSync)

    @Test
    fun registeredUserSeesNotificationRationaleThenLocationRationale() = runTest(dispatcher) {
        val vm = viewModel()
        vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = true))
        assertTrue(vm.viewState.value.showNotificationRationale)
        assertFalse(vm.viewState.value.showLocationRationale)

        vm.viewEffect.test {
            vm.processIntent(PushViewIntent.RationaleAnswered(NotificationsPrompt.NOTIFICATIONS, accepted = true))
            assertEquals(PushViewEffect.RequestNotificationPermission, awaitItem())
        }
        vm.processIntent(PushViewIntent.PermissionResult(NotificationsPrompt.NOTIFICATIONS, granted = true))
        verify(exactly = 1) { deviceSync.sync(null) }
        assertTrue(vm.viewState.value.showLocationRationale)

        vm.processIntent(PushViewIntent.RationaleAnswered(NotificationsPrompt.LOCATION, accepted = false))
        assertFalse(vm.viewState.value.showLocationRationale)
        assertEquals(setOf(NotificationsPrompt.NOTIFICATIONS, NotificationsPrompt.LOCATION), prompts)
    }

    @Test
    fun promptsAreShownOnlyOnceAndNeverToGuests() = runTest(dispatcher) {
        val guest = viewModel()
        guest.processIntent(PushViewIntent.Started(isGuest = true, needsNotificationPermission = true))
        assertFalse(guest.viewState.value.showNotificationRationale)
        assertFalse(guest.viewState.value.showLocationRationale)

        prompts += NotificationsPrompt.NOTIFICATIONS
        prompts += NotificationsPrompt.LOCATION
        val returning = viewModel()
        returning.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = true))
        assertFalse(returning.viewState.value.showNotificationRationale)
        assertFalse(returning.viewState.value.showLocationRationale)
    }

    @Test
    fun grantedLocationEnablesSharingAndResumeRefreshes() = runTest(dispatcher) {
        val vm = viewModel()
        vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = false))
        vm.processIntent(PushViewIntent.PermissionResult(NotificationsPrompt.LOCATION, granted = true))
        vm.processIntent(PushViewIntent.Resumed)
        advanceUntilIdle()
        coVerify(exactly = 1) { location.enable() }
        coVerify(exactly = 2) { location.refreshIfDue() }
    }

    @Test
    fun pendingOfferNotificationOpensProductDetail() = runTest(dispatcher) {
        coEvery { getOffer("offer-1") } returns Result.Success(OfferDetails(offer, 0, false, 0L))
        requests.submit(PushTarget.Offer("offer-1"))
        val vm = viewModel()
        vm.viewEffect.test {
            vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = false))
            assertEquals(PushViewEffect.OpenOffer(offer), awaitItem())
        }
        assertNull(requests.pending.value)
    }

    @Test
    fun claimNotificationOpensClaimScreenOnlyForRegisteredUsers() = runTest(dispatcher) {
        coEvery { getOffer("offer-1") } returns Result.Success(OfferDetails(offer, 0, true, 0L))
        val vm = viewModel()
        vm.viewEffect.test {
            vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = false))
            requests.submit(PushTarget.Claim("offer-1"))
            assertEquals(PushViewEffect.OpenClaim(offer), awaitItem())
        }

        val guestRequests = PushOpenRequests()
        val guest = viewModel(guestRequests)
        guest.viewEffect.test {
            guest.processIntent(PushViewIntent.Started(isGuest = true, needsNotificationPermission = false))
            guestRequests.submit(PushTarget.Claim("offer-1"))
            assertEquals(PushViewEffect.GuestRequired, awaitItem())
        }
        coVerify(exactly = 1) { getOffer("offer-1") }
    }

    @Test
    fun socialMerchantAndMissingOfferTargets() = runTest(dispatcher) {
        coEvery { getOffer("gone") } returns Result.Failure(AppError.NotFound())
        val vm = viewModel()
        vm.viewEffect.test {
            vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = false))
            requests.submit(PushTarget.Friends)
            assertEquals(PushViewEffect.OpenFriends, awaitItem())
            requests.submit(PushTarget.MerchantClaims)
            assertEquals(PushViewEffect.OpenMerchantClaims, awaitItem())
            requests.submit(PushTarget.Offer("gone"))
            assertEquals(PushViewEffect.OfferUnavailable, awaitItem())
            requests.submit(PushTarget.Home)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun targetsWaitUntilTheMainScreenStarts() = runTest(dispatcher) {
        val vm = viewModel()
        requests.submit(PushTarget.Friends)
        advanceUntilIdle()
        assertEquals(PushTarget.Friends, requests.pending.value)
        vm.viewEffect.test {
            vm.processIntent(PushViewIntent.Started(isGuest = false, needsNotificationPermission = false))
            assertEquals(PushViewEffect.OpenFriends, awaitItem())
        }
    }
}
