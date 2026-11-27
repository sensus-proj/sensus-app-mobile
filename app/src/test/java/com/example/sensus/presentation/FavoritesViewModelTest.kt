package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.FavoriteToggleResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.FavoritesRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetFavoritesUseCase
import com.example.sensus.domain.usecase.ObserveFavoritesUseCase
import com.example.sensus.domain.usecase.ToggleFavoriteUseCase
import com.example.sensus.presentation.buyer.FavoritesViewEffect
import com.example.sensus.presentation.buyer.FavoritesViewIntent
import com.example.sensus.presentation.buyer.FavoritesViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val favoritesFlow = MutableStateFlow<List<FavoriteOffer>>(emptyList())
    private val repository = mockk<FavoritesRepository>(relaxed = true) {
        every { favorites } returns favoritesFlow
    }
    private val sessionFlow = MutableStateFlow(
        SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com", activeToken = "jwt-a")
    )
    private val sessionRepo = mockk<SessionRepository> {
        every { sessionState } returns sessionFlow
    }

    private val activeOffer = ProductOffer(
        id = "fav-1",
        name = "Tacos al pastor",
        storeName = "Taquería",
        category = "Comida",
        originalPrice = 100.0,
        discountPrice = 60.0,
        discountPercent = 40,
        unitsLeft = 3,
        distanceMeters = 200,
        expiresAtMillis = Long.MAX_VALUE
    )
    private val soldOutOffer = activeOffer.copy(id = "fav-2", unitsLeft = 0)

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun createViewModel() = FavoritesViewModel(
        getFavoritesUseCase = GetFavoritesUseCase(repository, sessionRepo),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repository, sessionRepo),
        observeFavoritesUseCase = ObserveFavoritesUseCase(repository),
        sessionRepository = sessionRepo
    )

    @Test
    fun sync404WithCachedDataShowsCacheNoticeWithoutWipingListAndFiltersWork() = runTest(dispatcher) {
        favoritesFlow.value = listOf(FavoriteOffer(activeOffer), FavoriteOffer(soldOutOffer))
        coEvery { repository.syncFavorites(1, 20) } returns Result.Failure(AppError.NotFound())

        val vm = createViewModel()
        advanceUntilIdle()

        vm.dispatchIntent(FavoritesViewIntent.Load)
        advanceUntilIdle()

        val state = vm.viewState.value
        assertEquals(2, state.favorites.size)
        assertTrue(state.isUsingOfflineCache)
        assertNull(state.errorMessage)

        vm.dispatchIntent(FavoritesViewIntent.SelectFilter(FavoriteFilter.AVAILABLE_ONLY))
        advanceUntilIdle()
        assertEquals(listOf("fav-1"), vm.viewState.value.favorites.map { it.offer.id })
    }

    @Test
    fun toggleFavoriteGuestEmitsRequireAuthAndAuthenticatedEmitsFeedback() = runTest(dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.viewEffect.test {
            sessionFlow.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-tok")
            vm.dispatchIntent(FavoritesViewIntent.ToggleFavorite(activeOffer))
            advanceUntilIdle()
            assertEquals(FavoritesViewEffect.RequireAuth, awaitItem())

            sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com", activeToken = "jwt-a")
            coEvery { repository.toggleFavorite(activeOffer) } returns Result.Success(FavoriteToggleResult("fav-1", true, 4))
            vm.dispatchIntent(FavoritesViewIntent.ToggleFavorite(activeOffer))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is FavoritesViewEffect.ShowMessage)
            assertFalse(vm.viewState.value.pendingOfferIds.contains("fav-1"))
        }
    }
}
