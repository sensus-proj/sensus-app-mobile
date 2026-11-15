package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.RepublishOfferUseCase
import com.example.sensus.presentation.merchant.MerchantEditOfferViewEffect
import com.example.sensus.presentation.merchant.MerchantEditOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantEditOfferViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class RepublishViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<OffersRepository>()
    private val input = RepublishOfferInput("offer-1", 2000L, 3000L, 4)
    private val offer = ProductOffer(
        id = "offer-1", name = "Pan", storeName = "Tienda", category = "Panadería",
        originalPrice = 100.0, discountPrice = 60.0, discountPercent = 40, unitsLeft = 0, distanceMeters = 0,
        status = OfferLifecycleStatus.EXPIRED
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MerchantEditOfferViewModel = MerchantEditOfferViewModel(mockk(), mockk(), RepublishOfferUseCase(repository) { 1000L })

    @Test
    fun duplicateIntentMakesOneRequestAndEmitsConfirmedOffer() = runTest(dispatcher) {
        val confirmed = offer.copy(status = OfferLifecycleStatus.ACTIVE, unitsLeft = 3, version = 9)
        coEvery { repository.republishOffer(input) } returns Result.Success(confirmed)
        val vm = createViewModel()
        vm.dispatchIntent(MerchantEditOfferViewIntent.Initialize(offer))
        vm.viewEffect.test {
            repeat(2) { vm.dispatchIntent(MerchantEditOfferViewIntent.Republish(input)) }
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(confirmed, (awaitItem() as MerchantEditOfferViewEffect.OfferRepublished).offer)
            expectNoEvents()
        }
        assertEquals(confirmed, vm.viewState.value.currentOffer)
        assertFalse(vm.viewState.value.isSaving)
        coVerify(exactly = 1) { repository.republishOffer(input) }
    }

    @Test
    fun failureKeepsHistoricalOfferAndDoesNotEmitSuccessOrRetry() = runTest(dispatcher) {
        coEvery { repository.republishOffer(input) } returns Result.Failure(AppError.Network())
        val vm = createViewModel()
        vm.dispatchIntent(MerchantEditOfferViewIntent.Initialize(offer))
        vm.viewEffect.test {
            vm.dispatchIntent(MerchantEditOfferViewIntent.Republish(input))
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(offer, vm.viewState.value.currentOffer)
        assertTrue(vm.viewState.value.republishFailed)
        assertFalse(vm.viewState.value.isSaving)
        vm.dispatchIntent(MerchantEditOfferViewIntent.FieldEdited)
        assertFalse(vm.viewState.value.republishFailed)
        coVerify(exactly = 1) { repository.republishOffer(input) }
    }

    @Test
    fun activeAndUninitializedOffersNeverRepublish() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.dispatchIntent(MerchantEditOfferViewIntent.Republish(input))
        vm.dispatchIntent(MerchantEditOfferViewIntent.Initialize(offer.copy(status = OfferLifecycleStatus.ACTIVE)))
        vm.dispatchIntent(MerchantEditOfferViewIntent.Republish(input))
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { repository.republishOffer(any()) }
        assertTrue(vm.viewState.value.republishFailed)
    }
}
