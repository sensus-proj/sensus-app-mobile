package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.OfferSearchRequest
import com.example.sensus.domain.repository.DiscoveryRepository
import com.example.sensus.domain.repository.InterestsRepository
import com.example.sensus.domain.repository.OfferFeedRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.GetOfferFeedUseCase
import com.example.sensus.domain.usecase.SearchOffersUseCase
import com.example.sensus.domain.usecase.UpdateMyInterestsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferFeedAndDiscoveryUseCasesTest {

    private fun registeredSessionRepo(isGuest: Boolean = false): SessionRepository {
        val repo = mockk<SessionRepository>(relaxed = true)
        every { repo.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true, isGuest = isGuest))
        return repo
    }

    private fun feedItem(id: String) = OfferFeedItem(
        offerId = id,
        title = "Oferta $id",
        categoryCode = "Bakery",
        categoryDisplayName = "Panadería",
        imageUrl = null,
        originalPrice = 100.0,
        finalPrice = 60.0,
        stock = 3,
        distanceMeters = 250,
        expiresAtUtc = "2026-11-21T00:00:00Z",
        merchantId = "m1",
        merchantName = "La Espiga",
        slot = FeedSlot.RANKED
    )

    @Test
    fun getOfferFeedUseCase_blocksGuestWithoutCallingApi() = runTest {
        val repo = mockk<OfferFeedRepository>()
        val useCase = GetOfferFeedUseCase(repo, registeredSessionRepo(isGuest = true))

        val res = useCase(OfferFeedRequest(19.43, -99.13))

        assertTrue(res is Result.Failure)
        assertTrue((res as Result.Failure).error is AppError.Unauthorized)
        coVerify(exactly = 0) { repo.getFeed(any()) }
    }

    @Test
    fun getOfferFeedUseCase_reusesSnapshotAndResetsOnMoveOrRefreshAndDedupesNullSnapshot() = runTest {
        val repo = mockk<OfferFeedRepository>()
        coEvery { repo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 1, 20, null)) } returns Result.Success(
            OfferFeedPage(listOf(feedItem("a"), feedItem("b")), 1, 20, 4, 2, "snap-1", "feed-v1")
        )
        coEvery { repo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 2, 20, "snap-1")) } returns Result.Success(
            OfferFeedPage(listOf(feedItem("c"), feedItem("d")), 2, 20, 4, 2, "snap-1", "feed-v1")
        )
        coEvery { repo.getFeed(OfferFeedRequest(19.50, -99.13, 10, 1, 20, null)) } returns Result.Success(
            OfferFeedPage(listOf(feedItem("b"), feedItem("x")), 1, 20, 4, 2, null, "feed-v1")
        )
        coEvery { repo.getFeed(OfferFeedRequest(19.50, -99.13, 10, 2, 20, null)) } returns Result.Success(
            OfferFeedPage(listOf(feedItem("b"), feedItem("y")), 2, 20, 4, 2, null, "feed-v1")
        )

        val useCase = GetOfferFeedUseCase(repo, registeredSessionRepo(isGuest = false))

        val p1 = (useCase(OfferFeedRequest(19.43, -99.13, 10, 1)) as Result.Success).data
        assertEquals("snap-1", p1.snapshotId)

        val p2 = (useCase(OfferFeedRequest(19.43, -99.13, 10, 2)) as Result.Success).data
        assertEquals(listOf("c", "d"), p2.items.map { it.offerId })

        // Movimiento > 500 m reinicia a página 1 con snapshotId = null
        val moved = (useCase(OfferFeedRequest(19.50, -99.13, 10, 2)) as Result.Success).data
        assertNull(moved.snapshotId)

        // Snapshot nulo en página 2 deduplica ("b" ya apareció en p1 tras reset)
        val p2Null = (useCase(OfferFeedRequest(19.50, -99.13, 10, 2)) as Result.Success).data
        assertEquals(listOf("y"), p2Null.items.map { it.offerId })
    }

    @Test
    fun interestsUseCases_validateZeroToFiveAndBlockGuestsAndOthers() = runTest {
        val repo = mockk<InterestsRepository>()
        coEvery { repo.getMyInterests() } returns Result.Success(listOf("Bakery"))
        coEvery { repo.updateMyInterests(listOf("Bakery", "Dairy")) } returns Result.Success(listOf("Bakery", "Dairy"))

        val getUc = GetMyInterestsUseCase(repo, registeredSessionRepo(false))
        val putUc = UpdateMyInterestsUseCase(repo, registeredSessionRepo(false))

        assertEquals(listOf("Bakery"), (getUc() as Result.Success).data)
        assertEquals(listOf("Bakery", "Dairy"), (putUc(listOf("bakery", "Dairy")) as Result.Success).data)

        // Inválido (>5 o "Todo")
        assertTrue(putUc(listOf("Todo")) is Result.Failure)
        assertTrue(putUc(listOf("Grocery", "Dairy", "Food", "Bakery", "Beverages", "Pets")) is Result.Failure)

        // Invitado bloqueado
        val guestPut = UpdateMyInterestsUseCase(repo, registeredSessionRepo(true))
        assertTrue(guestPut(emptyList()) is Result.Failure)
    }

    @Test
    fun searchOffersUseCase_validatesLengthClampsGuest2KmAndSkipsGuestHistory() = runTest {
        val repo = mockk<DiscoveryRepository>(relaxed = true)
        coEvery { repo.searchOffers(OfferSearchRequest("pan", 19.43, -99.13, 2.0, 1, 20)) } returns Result.Success(
            OfferSearchPage(emptyList(), 0, 1, 20, 0)
        )
        coEvery { repo.searchOffers(OfferSearchRequest("pan", 19.43, -99.13, 15.0, 1, 20)) } returns Result.Success(
            OfferSearchPage(emptyList(), 0, 1, 20, 0)
        )

        val guestUc = SearchOffersUseCase(repo, registeredSessionRepo(isGuest = true))
        assertTrue(guestUc(OfferSearchRequest("a", 19.43, -99.13)) is Result.Failure)
        assertTrue(guestUc(OfferSearchRequest("pan\n", 19.43, -99.13)) is Result.Failure)
        assertTrue(guestUc(OfferSearchRequest("x".repeat(81), 19.43, -99.13)) is Result.Failure)

        val gRes = guestUc(OfferSearchRequest("pan", 19.43, -99.13, radiusKm = 15.0))
        assertTrue(gRes is Result.Success)
        coVerify(exactly = 0) { repo.saveSearchQuery(any()) }

        val userUc = SearchOffersUseCase(repo, registeredSessionRepo(isGuest = false))
        assertTrue(userUc(OfferSearchRequest("pan", 19.43, -99.13, radiusKm = 15.0)) is Result.Success)
        coVerify(exactly = 1) { repo.saveSearchQuery("pan") }
    }

    @Test
    fun getOfferAlternativesUseCase_andDomainMappers_mapFieldsCorrectly() = runTest {
        val repo = mockk<DiscoveryRepository>()
        coEvery { repo.getAlternatives("off-1", 10) } returns Result.Success(emptyList())
        val altUc = GetOfferAlternativesUseCase(repo)

        assertTrue(altUc("   ") is Result.Failure)
        assertTrue(altUc("off-1", limit = 25) is Result.Success)

        val mappedFeed = feedItem("f1").toProductOffer()
        assertEquals(40, mappedFeed.discountPercent)
        assertEquals("Panadería", mappedFeed.category)

        val searchMapped = OfferSearchItem(
            offerId = "s1",
            title = "Leche",
            description = "Desc",
            categoryCode = "Dairy",
            categoryDisplayName = "Lácteos",
            imageUrl = null,
            originalPrice = 50.0,
            finalPrice = 25.0,
            stock = 2,
            distanceMeters = 120,
            expiresAtUtc = "2026-11-21",
            merchantId = "m1",
            merchantName = "Tienda",
            score = 0.9
        ).toProductOffer()
        assertEquals(50, searchMapped.discountPercent)

        val altMapped = OfferAlternativeItem(
            offerId = "a1",
            title = "Yogurt",
            categoryCode = "Dairy",
            categoryDisplayName = "Lácteos",
            imageUrl = null,
            originalPrice = 40.0,
            finalPrice = 20.0,
            stock = 2,
            distanceMeters = 180,
            expiresAtUtc = "2026-11-21",
            merchantId = "m2",
            merchantName = "Cremería"
        ).toProductOffer()
        assertEquals(50, altMapped.discountPercent)
    }
}
