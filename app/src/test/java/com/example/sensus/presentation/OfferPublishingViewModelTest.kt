package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.presentation.merchant.MerchantNewOfferViewEffect
import com.example.sensus.presentation.merchant.MerchantNewOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantNewOfferViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfferPublishingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val useCase = mockk<CreateOfferUseCase>()
    private val input = CreateOfferInput("Pan", "", StoreCategory.BAKERY, 100.0, 60.0, 7, Long.MAX_VALUE)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidPrice_stopsAtFieldErrorAndClearsWhenEdited() = runTest(dispatcher) {
        val vm = MerchantNewOfferViewModel(useCase)
        vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input.copy(discountPrice = 100.0)))
        assertEquals(OfferField.PRICE, vm.viewState.value.fieldError?.field)
        coVerify(exactly = 0) { useCase(any(), any()) }
        vm.dispatchIntent(MerchantNewOfferViewIntent.FieldEdited)
        assertNull(vm.viewState.value.fieldError)
    }

    @Test
    fun confirmedOffer_emitsOneNavigationEffectAndPreventsDoubleSubmit() = runTest(dispatcher) {
        val offer = ProductOffer("1", "Pan", "Tienda", "Panadería", 100.0, 60.0, 40, 7, 0)
        coEvery { useCase(any(), any()) } returns Result.Success(offer)
        val vm = MerchantNewOfferViewModel(useCase)
        vm.viewEffect.test {
            vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
            vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(MerchantNewOfferViewEffect.OfferCreated(offer), awaitItem())
            expectNoEvents()
        }
        coVerify(exactly = 1) { useCase(any(), any()) }
    }

    @Test
    fun moderationFailure_showsDialogAndDoesNotNavigate() = runTest(dispatcher) {
        coEvery { useCase(any(), any()) } returns Result.Failure(AppError.ContentRejected("Imagen bloqueada"))
        val vm = MerchantNewOfferViewModel(useCase)
        vm.viewEffect.test {
            vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals("Imagen bloqueada", vm.viewState.value.moderationMessage)
        assertFalse(vm.viewState.value.isPublishing)
        vm.dispatchIntent(MerchantNewOfferViewIntent.DismissModeration)
        assertNull(vm.viewState.value.moderationMessage)
    }

    @Test
    fun networkFailure_retainsFormAndAllowsRetry() = runTest(dispatcher) {
        coEvery { useCase(any(), any()) } returns Result.Failure(AppError.Network("Sin conexión"))
        val vm = MerchantNewOfferViewModel(useCase)
        vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Sin conexión", vm.viewState.value.errorMessage)
        assertFalse(vm.viewState.value.isPublishing)
    }
}
