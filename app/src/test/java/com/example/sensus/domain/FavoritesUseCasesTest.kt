package com.example.sensus.domain

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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesUseCasesTest {

    private val favoritesFlow = MutableStateFlow<List<FavoriteOffer>>(emptyList())
    private val repository = mockk<FavoritesRepository>(relaxed = true) {
        every { favorites } returns favoritesFlow
    }
    private val sessionFlow = MutableStateFlow(
        SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com", activeToken = "jwt-a")
    )
    private val sessionRepository = mockk<SessionRepository> {
        every { sessionState } returns sessionFlow
    }

    private val sampleOffer = ProductOffer(
        id = "offer-fav-1",
        name = "Caja sorpresa panadería",
        storeName = "Panadería Centro",
        category = "Panadería",
        originalPrice = 100.0,
        discountPrice = 50.0,
        discountPercent = 50,
        unitsLeft = 3,
        distanceMeters = 200,
        expiresAtMillis = 2_000L
    )

    @Test
    fun toggleFavoriteBlocksGuestWithoutCallingRepository() = runTest {
        sessionFlow.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-tok")
        val useCase = ToggleFavoriteUseCase(repository, sessionRepository)

        val result = useCase(sampleOffer)

        assertTrue(result is Result.Failure && result.error is AppError.Forbidden)
        coVerify(exactly = 0) { repository.toggleFavorite(any()) }
    }

    @Test
    fun toggleFavoriteRejectsBlankIdAndDelegatesValidAuthenticatedCall() = runTest {
        val useCase = ToggleFavoriteUseCase(repository, sessionRepository)
        val blankResult = useCase(sampleOffer.copy(id = "  "))
        assertTrue(blankResult is Result.Failure && blankResult.error is AppError.Validation)

        coEvery { repository.toggleFavorite(sampleOffer) } returns Result.Success(FavoriteToggleResult(sampleOffer.id, true, 7))
        val ok = useCase(sampleOffer)
        assertEquals(Result.Success(FavoriteToggleResult(sampleOffer.id, true, 7)), ok)
    }

    @Test
    fun getFavoritesClampsPaginationAndRejectsGuestOrMissingToken() = runTest {
        val useCase = GetFavoritesUseCase(repository, sessionRepository)
        coEvery { repository.syncFavorites(1, 100) } returns Result.Success(emptyList())

        val ok = useCase(page = 0, pageSize = 500)
        assertTrue(ok is Result.Success)
        coVerify(exactly = 1) { repository.syncFavorites(1, 100) }

        sessionFlow.value = SessionState(isSessionActive = true, isGuest = true, activeToken = "guest-tok")
        val guestResult = useCase()
        assertTrue(guestResult is Result.Failure && guestResult.error is AppError.Forbidden)

        sessionFlow.value = SessionState(isSessionActive = false, isGuest = false, activeToken = null)
        val unauthResult = useCase()
        assertTrue(unauthResult is Result.Failure && unauthResult.error is AppError.Unauthorized)
    }

    @Test
    fun observeFavoritesFiltersExpiredAndSoldOutWhenAvailableOnlySelected() = runTest {
        val available = FavoriteOffer(sampleOffer)
        val soldOut = FavoriteOffer(sampleOffer.copy(id = "sold-out", unitsLeft = 0))
        val expired = FavoriteOffer(sampleOffer.copy(id = "expired", expiresAtMillis = 500L))
        favoritesFlow.value = listOf(available, soldOut, expired)

        val useCase = ObserveFavoritesUseCase(repository)

        val allItems = useCase(FavoriteFilter.ALL) { 1_000L }.first()
        assertEquals(listOf("offer-fav-1", "sold-out", "expired"), allItems.map { it.offer.id })

        val availableOnly = useCase(FavoriteFilter.AVAILABLE_ONLY) { 1_000L }.first()
        assertEquals(listOf("offer-fav-1"), availableOnly.map { it.offer.id })

        val noExpiration = FavoriteOffer(sampleOffer.copy(id = "no-exp", expiresAtMillis = null))
        favoritesFlow.value = listOf(noExpiration, soldOut, expired)
        assertEquals(listOf("no-exp"), useCase(FavoriteFilter.AVAILABLE_ONLY) { 1_000L }.first().map { it.offer.id })
    }

    @Test
    fun toggleAndGetFavoritesRejectInactiveOrBlankTokenSessions() = runTest {
        val toggle = ToggleFavoriteUseCase(repository, sessionRepository)
        val get = GetFavoritesUseCase(repository, sessionRepository)

        sessionFlow.value = SessionState(isSessionActive = false, isGuest = false, activeToken = "jwt-a")
        assertTrue((toggle(sampleOffer) as Result.Failure).error is AppError.Unauthorized)
        assertTrue((get() as Result.Failure).error is AppError.Unauthorized)

        sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, activeToken = " ")
        assertTrue((toggle(sampleOffer) as Result.Failure).error is AppError.Unauthorized)
        assertTrue((get() as Result.Failure).error is AppError.Unauthorized)
    }
}
