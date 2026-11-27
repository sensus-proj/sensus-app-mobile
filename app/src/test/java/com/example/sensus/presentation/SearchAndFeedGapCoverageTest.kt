package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.DiscoveryRepository
import com.example.sensus.domain.repository.InterestsRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OfferFeedRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.GetOfferFeedUseCase
import com.example.sensus.domain.usecase.SearchOffersUseCase
import com.example.sensus.domain.usecase.UpdateMyInterestsUseCase
import com.example.sensus.presentation.buyer.InterestsPickerViewEffect
import com.example.sensus.presentation.buyer.InterestsPickerViewIntent
import com.example.sensus.presentation.buyer.InterestsPickerViewModel
import com.example.sensus.presentation.buyer.OfferFeedViewEffect
import com.example.sensus.presentation.buyer.OfferFeedViewIntent
import com.example.sensus.presentation.buyer.OfferFeedViewModel
import com.example.sensus.presentation.buyer.SearchOffersViewIntent
import com.example.sensus.presentation.buyer.SearchOffersViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchAndFeedGapCoverageTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationRepo = mockk<LocationRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { locationRepo.hasLocationPermission() } returns true
        coEvery { locationRepo.getCurrentLocation() } returns UserLocation(19.43, -99.13)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sessionFlow(isGuest: Boolean) = MutableStateFlow(SessionState(isSessionActive = true, isGuest = isGuest))

    private fun sessionRepo(flow: MutableStateFlow<SessionState>): SessionRepository = mockk<SessionRepository>(relaxed = true) {
        every { sessionState } returns flow
    }

    @Test
    fun searchOffers_guestClampsRadiusDebouncesEmptyResetAndClearsHistory() = runTest(testDispatcher) {
        val repo = mockk<DiscoveryRepository>(relaxed = true)
        val flow = sessionFlow(isGuest = false)
        coEvery { repo.getSearchHistory() } returns listOf("panadería")
        coEvery { repo.searchOffers(any()) } returns Result.Success(OfferSearchPage(emptyList(), 0, 1, 20, 0))

        val vm = SearchOffersViewModel(SearchOffersUseCase(repo, sessionRepo(flow)), GetOfferAlternativesUseCase(repo), locationRepo, sessionRepo(flow))
        vm.dispatchIntent(SearchOffersViewIntent.Initialize)
        advanceUntilIdle()
        assertEquals(listOf("panadería"), vm.viewState.value.history)

        vm.dispatchIntent(SearchOffersViewIntent.SelectHistory("café"))
        advanceUntilIdle()
        assertEquals("café", vm.viewState.value.query)
        assertTrue(vm.viewState.value.hasSearched)

        vm.dispatchIntent(SearchOffersViewIntent.QueryChanged("   "))
        advanceTimeBy(400)
        advanceUntilIdle()
        coVerify { repo.searchOffers(match { it.query == null }) }

        vm.dispatchIntent(SearchOffersViewIntent.ClearHistory)
        advanceUntilIdle()
        assertTrue(vm.viewState.value.history.isEmpty())
        coVerify { repo.clearSearchHistory() }

        flow.value = SessionState(isSessionActive = true, isGuest = true)
        advanceUntilIdle()
        assertTrue(vm.viewState.value.isGuest)
        assertTrue(vm.viewState.value.radiusKm <= GuestRadiusPolicy.GUEST_MAX_RADIUS_KM.toDouble())
    }

    @Test
    fun searchOffers_shortQueryFallbackAlternativesAndMissingPermissionWork() = runTest(testDispatcher) {
        val repo = mockk<DiscoveryRepository>(relaxed = true)
        val flow = sessionFlow(isGuest = false)
        every { locationRepo.hasLocationPermission() } returns false
        val nearbyItem = OfferSearchItem(
            offerId = "s1",
            title = "Concha",
            description = "Dulce",
            categoryCode = null,
            categoryDisplayName = "Panadería",
            imageUrl = null,
            originalPrice = 20.0,
            finalPrice = 12.0,
            stock = 4,
            distanceMeters = 150,
            expiresAtUtc = "2026-12-01T12:00:00Z",
            merchantId = "m1",
            merchantName = "Horno",
            score = 0.9
        )
        coEvery { repo.searchOffers(match { it.query == "nada" }) } returns Result.Success(OfferSearchPage(emptyList(), 0, 1, 20, 0))
        coEvery { repo.searchOffers(match { it.query == null }) } returns
            Result.Success(OfferSearchPage(listOf(nearbyItem), 1, 1, 5, 1)) andThen Result.Failure(AppError.Network())
        coEvery { repo.getAlternatives("ref-fail", 5) } returns Result.Failure(AppError.Network())

        val vm = SearchOffersViewModel(SearchOffersUseCase(repo, sessionRepo(flow)), GetOfferAlternativesUseCase(repo), locationRepo, sessionRepo(flow))
        vm.dispatchIntent(SearchOffersViewIntent.SubmitSearch("a"))
        advanceUntilIdle()
        assertEquals("Escribe al menos 2 caracteres para buscar.", vm.viewState.value.errorMessage)

        vm.dispatchIntent(SearchOffersViewIntent.SubmitSearch("nada", referenceOfferId = "ref-fail"))
        advanceUntilIdle()
        assertTrue(vm.viewState.value.alternatives.isEmpty())

        vm.dispatchIntent(SearchOffersViewIntent.SubmitSearch("nada", referenceOfferId = null))
        advanceUntilIdle()
        assertEquals(listOf("Concha"), vm.viewState.value.alternatives.map { it.title })

        vm.dispatchIntent(SearchOffersViewIntent.SubmitSearch("nada", referenceOfferId = null))
        advanceUntilIdle()
        assertTrue(vm.viewState.value.alternatives.isEmpty())
    }

    @Test
    fun offerFeed_expandRadiusOpenInterestsAndHandlesAuthAndNetworkFailures() = runTest(testDispatcher) {
        val feedRepo = mockk<OfferFeedRepository>()
        val interestsRepo = mockk<InterestsRepository>()
        val session = sessionRepo(sessionFlow(isGuest = false))
        every { locationRepo.hasLocationPermission() } returns false
        coEvery { interestsRepo.getMyInterests() } returns Result.Success(listOf("Bakery"))
        val pageOneItem = OfferFeedItem("1", "Pan", "Bakery", "Panadería", null, 50.0, 30.0, 2, 100, "2026-11-28", "m1", "Espiga", FeedSlot.RANKED)
        coEvery { feedRepo.getFeed(match { it.page == 1 && it.radiusKm == 10 }) } returns
            Result.Failure(AppError.Network("Sin red")) andThen
            Result.Failure(AppError.Unauthorized("Expirada"))
        coEvery { feedRepo.getFeed(match { it.page == 1 && it.radiusKm == 25 }) } returns
            Result.Success(OfferFeedPage(listOf(pageOneItem), 1, 20, 2, 2, "s1", "v1"))
        coEvery { feedRepo.getFeed(match { it.page == 2 }) } returns Result.Failure(AppError.Network("Error p2"))

        val vm = OfferFeedViewModel(GetOfferFeedUseCase(feedRepo, session), GetMyInterestsUseCase(interestsRepo, session), locationRepo, session)
        vm.viewEffect.test {
            vm.dispatchIntent(OfferFeedViewIntent.LoadInitial)
            advanceUntilIdle()
            assertEquals("Sin red", vm.viewState.value.errorMessage)

            vm.dispatchIntent(OfferFeedViewIntent.Refresh)
            advanceUntilIdle()
            assertTrue(vm.viewState.value.isGuestBlocked)
            assertEquals(OfferFeedViewEffect.RequireAuth, awaitItem())

            vm.dispatchIntent(OfferFeedViewIntent.OpenInterestsPicker)
            assertEquals(OfferFeedViewEffect.NavigateToInterestsPicker, awaitItem())

            vm.dispatchIntent(OfferFeedViewIntent.ExpandRadius(25))
            advanceUntilIdle()
            assertEquals(25, vm.viewState.value.radiusKm)
            assertFalse(vm.viewState.value.isGuestBlocked)

            vm.dispatchIntent(OfferFeedViewIntent.LoadNextPage)
            advanceUntilIdle()
            assertEquals("Error p2", vm.viewState.value.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun interestsPicker_loadDeselectDismissAndFailurePaths() = runTest(testDispatcher) {
        val repo = mockk<InterestsRepository>()
        val session = sessionRepo(sessionFlow(isGuest = false))
        coEvery { repo.getMyInterests() } returns
            Result.Success(listOf("Bakery", "Dairy")) andThen Result.Failure(AppError.Network("Sin conexión"))
        coEvery { repo.updateMyInterests(any()) } returns
            Result.Failure(AppError.Network("No se pudo guardar")) andThen Result.Failure(AppError.Unauthorized("Sesión expirada"))

        val vm = InterestsPickerViewModel(GetMyInterestsUseCase(repo, session), UpdateMyInterestsUseCase(repo, session))
        vm.viewEffect.test {
            vm.dispatchIntent(InterestsPickerViewIntent.LoadCurrent)
            advanceUntilIdle()
            assertEquals(listOf("Bakery", "Dairy"), vm.viewState.value.selectedCodes)

            vm.dispatchIntent(InterestsPickerViewIntent.ToggleCategory("Bakery"))
            assertEquals(listOf("Dairy"), vm.viewState.value.selectedCodes)

            vm.dispatchIntent(InterestsPickerViewIntent.LoadCurrent)
            advanceUntilIdle()
            assertNull(vm.viewState.value.errorMessage)

            vm.dispatchIntent(InterestsPickerViewIntent.Save)
            advanceUntilIdle()
            assertEquals("No se pudo guardar", vm.viewState.value.errorMessage)

            vm.dispatchIntent(InterestsPickerViewIntent.DismissError)
            assertNull(vm.viewState.value.errorMessage)

            vm.dispatchIntent(InterestsPickerViewIntent.Save)
            advanceUntilIdle()
            assertEquals(InterestsPickerViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
