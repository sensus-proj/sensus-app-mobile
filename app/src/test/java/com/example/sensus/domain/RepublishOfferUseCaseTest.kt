package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.RepublishOfferUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepublishOfferUseCaseTest {
    private val repository = mockk<OffersRepository>()
    private val input = RepublishOfferInput("offer-1", 2000L, 3000L, 4)
    private val offer = ProductOffer(
        id = "offer-1", name = "Pan", storeName = "Tienda", category = "Panadería",
        originalPrice = 100.0, discountPrice = 60.0, discountPercent = 40, unitsLeft = 0, distanceMeters = 0,
        status = OfferLifecycleStatus.EXPIRED
    )

    @Test
    fun historicalStatusesDelegateAndReturnServerOffer() = runTest {
        val confirmed = offer.copy(status = OfferLifecycleStatus.ACTIVE, unitsLeft = 3, version = 7)
        coEvery { repository.republishOffer(input) } returns Result.Success(confirmed)
        val useCase = RepublishOfferUseCase(repository) { 1000L }
        for (status in listOf(OfferLifecycleStatus.EXPIRED, OfferLifecycleStatus.ARCHIVED)) {
            assertEquals(Result.Success(confirmed), useCase(offer.copy(status = status), input))
        }
        coVerify(exactly = 2) { repository.republishOffer(input) }
    }

    @Test
    fun unsupportedStatusesNeverReachRepository() = runTest {
        val useCase = RepublishOfferUseCase(repository) { 1000L }
        for (status in OfferLifecycleStatus.entries.filterNot { RepublishOfferUseCase.supports(it) }) {
            assertTrue(useCase(offer.copy(status = status), input) is Result.Failure)
        }
        coVerify(exactly = 0) { repository.republishOffer(any()) }
    }

    @Test
    fun invalidScheduleStockOrIdentityNeverReachRepository() = runTest {
        val useCase = RepublishOfferUseCase(repository) { 1000L }
        val invalid = listOf(
            input.copy(newStartsAtMillis = 1000L),
            input.copy(newStartsAtMillis = 999L),
            input.copy(newEndsAtMillis = 2000L),
            input.copy(newEndsAtMillis = 1999L),
            input.copy(newStock = 0),
            input.copy(newStock = -1),
            input.copy(offerId = ""),
            input.copy(offerId = "other")
        )
        invalid.forEach { assertTrue(useCase(offer, it) is Result.Failure) }
        coVerify(exactly = 0) { repository.republishOffer(any()) }
    }
}
