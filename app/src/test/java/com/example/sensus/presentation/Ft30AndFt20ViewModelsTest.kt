package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
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
import com.example.sensus.presentation.support.FaqCatalog
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Ft30AndFt20ViewModelsTest {

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

    private fun sessionRepo(isGuest: Boolean): SessionRepository {
        val repo = mockk<SessionRepository>(relaxed = true)
        every { repo.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true, isGuest = isGuest))
        return repo
    }

    @Test
    fun offerFeedViewModel_loadsPagesAndHandlesGuestBlockAndEmptyInterests() = runTest {
        val feedRepo = mockk<OfferFeedRepository>()
        val interestsRepo = mockk<InterestsRepository>()
        val session = sessionRepo(isGuest = false)

        coEvery { interestsRepo.getMyInterests() } returns Result.Success(emptyList())
        coEvery {
            feedRepo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 1, 20, null, forceRefresh = true))
        } returns Result.Success(
            OfferFeedPage(
                listOf(OfferFeedItem("1", "Pan", "Bakery", "Panadería", null, 50.0, 30.0, 2, 100, "2026-11-21", "m1", "Espiga", FeedSlot.RANKED)),
                1, 20, 2, 2, "s1", "feed-v1"
            )
        )
        coEvery { feedRepo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 2, 20, "s1")) } returns Result.Success(
            OfferFeedPage(
                listOf(OfferFeedItem("2", "Leche", "Dairy", "Lácteos", null, 40.0, 25.0, 2, 200, "2026-11-21", "m2", "Sur", FeedSlot.EXPLORATION)),
                2, 20, 2, 2, "s1", "feed-v1"
            )
        )

        val vm = OfferFeedViewModel(
            GetOfferFeedUseCase(feedRepo, session),
            GetMyInterestsUseCase(interestsRepo, session),
            locationRepo,
            session
        )

        vm.viewState.test {
            awaitItem()
            vm.dispatchIntent(OfferFeedViewIntent.LoadInitial)
            advanceUntilIdle()
            val afterP1 = expectMostRecentItem()
            assertEquals(listOf("1"), afterP1.items.map { it.offerId })
            assertEquals(false, afterP1.hasUserInterests)

            vm.dispatchIntent(OfferFeedViewIntent.LoadNextPage)
            advanceUntilIdle()
            val afterP2 = expectMostRecentItem()
            assertEquals(listOf("1", "2"), afterP2.items.map { it.offerId })
        }

        // Guest nunca llama HTTP y emite RequireAuth
        val guestSession = sessionRepo(isGuest = true)
        val guestFeedRepo = mockk<OfferFeedRepository>()
        val guestVm = OfferFeedViewModel(
            GetOfferFeedUseCase(guestFeedRepo, guestSession),
            GetMyInterestsUseCase(interestsRepo, guestSession),
            locationRepo,
            guestSession
        )
        guestVm.viewEffect.test {
            guestVm.dispatchIntent(OfferFeedViewIntent.LoadInitial)
            advanceUntilIdle()
            assertEquals(OfferFeedViewEffect.RequireAuth, awaitItem())
        }
        coVerify(exactly = 0) { guestFeedRepo.getFeed(any()) }
    }

    @Test
    fun interestsPickerViewModel_capsAtFiveAndSkipDoesNotCallApi() = runTest {
        val repo = mockk<InterestsRepository>()
        val session = sessionRepo(isGuest = false)
        coEvery { repo.updateMyInterests(any()) } returns Result.Success(listOf("Grocery", "Dairy"))

        val vm = InterestsPickerViewModel(
            GetMyInterestsUseCase(repo, session),
            UpdateMyInterestsUseCase(repo, session)
        )

        vm.viewEffect.test {
            vm.dispatchIntent(InterestsPickerViewIntent.Skip)
            advanceUntilIdle()
            assertEquals(InterestsPickerViewEffect.Skipped, awaitItem())
            coVerify(exactly = 0) { repo.updateMyInterests(any()) }

            listOf("Grocery", "Dairy", "Food", "Bakery", "Beverages", "Pets").forEach { code ->
                vm.dispatchIntent(InterestsPickerViewIntent.ToggleCategory(code))
            }
            assertEquals(5, vm.viewState.value.selectedCodes.size)
            assertTrue(vm.viewState.value.errorMessage != null)

            vm.dispatchIntent(InterestsPickerViewIntent.Save)
            advanceUntilIdle()
            assertTrue(awaitItem() is InterestsPickerViewEffect.Saved)
        }
    }

    @Test
    fun searchOffersViewModel_debouncesRapidTypingAndLoadsAlternativesWhenEmpty() = runTest {
        val repo = mockk<DiscoveryRepository>(relaxed = true)
        val session = sessionRepo(isGuest = false)
        coEvery { repo.getSearchHistory() } returns listOf("panadería")
        coEvery { repo.searchOffers(match { it.query == "croissant" }) } returns Result.Success(
            OfferSearchPage(emptyList(), 0, 1, 20, 0)
        )
        coEvery { repo.getAlternatives("ref-1", 5) } returns Result.Success(
            listOf(OfferAlternativeItem("a1", "Bolillo", "Bakery", "Panadería", null, 30.0, 20.0, 3, 200, "2026-11-21", "m1", "Horno"))
        )

        val vm = SearchOffersViewModel(
            SearchOffersUseCase(repo, session),
            GetOfferAlternativesUseCase(repo),
            locationRepo,
            session
        )

        vm.dispatchIntent(SearchOffersViewIntent.Initialize)
        advanceUntilIdle()
        assertEquals(listOf("panadería"), vm.viewState.value.history)

        vm.dispatchIntent(SearchOffersViewIntent.QueryChanged("cr"))
        vm.dispatchIntent(SearchOffersViewIntent.QueryChanged("crois"))
        advanceTimeBy(350)
        advanceUntilIdle()

        // Solo la consulta final tras debounce se ejecutó
        coVerify(exactly = 0) { repo.searchOffers(match { it.query == "cr" }) }
        coVerify(exactly = 1) { repo.searchOffers(match { it.query == "crois" }) }

        vm.dispatchIntent(SearchOffersViewIntent.SubmitSearch("croissant", referenceOfferId = "ref-1"))
        advanceUntilIdle()
        assertEquals(1, vm.viewState.value.alternatives.size)
        assertEquals("Bolillo", vm.viewState.value.alternatives.first().title)

        // FaqCatalog cubre LFPDPPP (RF-23-04) y alcance offline
        assertTrue(FaqCatalog.offlineItems.any { it.id == "feed_privacy" && it.answer.contains("180 días") })
    }
}
