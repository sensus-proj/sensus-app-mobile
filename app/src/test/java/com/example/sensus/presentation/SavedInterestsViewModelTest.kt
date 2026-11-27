package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.data.repository.SavedOffersRepositoryImpl
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.presentation.buyer.SavedInterestsViewEffect
import com.example.sensus.presentation.buyer.SavedInterestsViewIntent
import com.example.sensus.presentation.buyer.SavedInterestsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedInterestsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessionFlow = MutableStateFlow(
        SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com")
    )
    private val sessionRepo = mockk<SessionRepository>(relaxed = true) {
        every { sessionState } returns sessionFlow
    }

    private val offer = ProductOffer(
        id = "offer-1",
        name = "Pan Integral",
        storeName = "Panadería Sol",
        category = "Panadería",
        originalPrice = 40.0,
        discountPrice = 25.0,
        discountPercent = 37,
        unitsLeft = 3,
        distanceMeters = 120
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authenticatedUserCanPinAndUnpinOffers() = runTest(dispatcher) {
        val repo = SavedOffersRepositoryImpl(
            userPreferenceDao = null,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        val vm = SavedInterestsViewModel(repo, sessionRepo)

        vm.viewEffect.test {
            vm.dispatchIntent(SavedInterestsViewIntent.ToggleOffer(offer))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(SavedInterestsViewEffect.SavedStateChanged(isSaved = true), awaitItem())
            assertTrue(offer.id in vm.viewState.value.savedOfferIds)
            assertEquals(1, vm.viewState.value.totalCount)

            vm.dispatchIntent(SavedInterestsViewIntent.RemoveOffer(offer.id))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(SavedInterestsViewEffect.SavedStateChanged(isSaved = false), awaitItem())
            assertEquals(0, vm.viewState.value.totalCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestUserEmitsRequireAuthWithoutSavingOffer() = runTest(dispatcher) {
        sessionFlow.value = SessionState(isSessionActive = true, isGuest = true)
        val repo = SavedOffersRepositoryImpl(
            userPreferenceDao = null,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        val vm = SavedInterestsViewModel(repo, sessionRepo)

        vm.viewEffect.test {
            vm.dispatchIntent(SavedInterestsViewIntent.ToggleOffer(offer))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(SavedInterestsViewEffect.RequireAuth, awaitItem())
            assertEquals(0, vm.viewState.value.totalCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
