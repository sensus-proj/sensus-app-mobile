package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ToggleOfferLikeResult
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.domain.usecase.ToggleOfferLikeUseCase
import com.example.sensus.presentation.buyer.ProductDetailViewIntent
import com.example.sensus.presentation.buyer.ProductDetailViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getDetail = mockk<GetOfferDetailUseCase>()
    private val registerView = mockk<RegisterOfferViewUseCase>()
    private val offer = ProductOffer("offer-1", "Pan", "Tienda", "Abarrotes", 100.0, 50.0, 50, 2, 100, expiresAtMillis = Long.MAX_VALUE)
    private val details = OfferDetails(offer, 3, false, 0)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { getDetail(any()) } returns Result.Success(details)
        coEvery { registerView(any()) } returns Result.Success(Unit)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshGetsCurrentStockButRegistersOnlyOneView() = runTest(dispatcher) {
        val vm = ProductDetailViewModel(getDetail, registerView)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        vm.dispatchIntent(ProductDetailViewIntent.ChangeQuantity(20))
        assertEquals(2, vm.viewState.value.quantity)
        vm.dispatchIntent(ProductDetailViewIntent.ChangeQuantity(0))
        assertEquals(1, vm.viewState.value.quantity)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 2) { getDetail(offer.id) }
        coVerify(exactly = 1) { registerView(offer.id) }
        assertEquals(details, vm.viewState.value.details)
    }

    @Test
    fun viewFailureDoesNotHideOfferAndNewOfferGetsItsOwnView() = runTest(dispatcher) {
        coEvery { registerView(any()) } returns Result.Failure(AppError.Network())
        val vm = ProductDetailViewModel(getDetail, registerView)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.error)
        assertEquals(details, vm.viewState.value.details)
        vm.dispatchIntent(ProductDetailViewIntent.Load("offer-2"))
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { registerView("offer-2") }
    }

    @Test
    fun loadFailureCannotEnableCheckoutOrRegisterViewAndRetryRecovers() = runTest(dispatcher) {
        coEvery { getDetail(any()) } returns Result.Failure(AppError.NotFound())
        val vm = ProductDetailViewModel(getDetail, registerView)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.details)
        assertFalse(vm.viewState.value.isLoading)
        assertTrue(vm.viewState.value.error is AppError.NotFound)
        coVerify(exactly = 0) { registerView(any()) }
        coEvery { getDetail(any()) } returns Result.Success(details)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.error)
        assertEquals(details, vm.viewState.value.details)
    }

    @Test
    fun toggleLike_updatesPublicCountOptimisticallyAndRollsBackOnFailure() = runTest(dispatcher) {
        val toggleLike = mockk<ToggleOfferLikeUseCase>()
        val vm = ProductDetailViewModel(getDetail, registerView, toggleLike)
        vm.dispatchIntent(ProductDetailViewIntent.Load(offer.id))
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { toggleLike(offer.id) } returns Result.Success(ToggleOfferLikeResult(offer.id, 3, true))
        vm.dispatchIntent(ProductDetailViewIntent.ToggleLike(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, vm.viewState.value.likesCount)
        assertTrue(vm.viewState.value.isLiked)

        coEvery { toggleLike(offer.id) } returns Result.Failure(AppError.Network())
        vm.dispatchIntent(ProductDetailViewIntent.ToggleLike(offer.id))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, vm.viewState.value.likesCount)
        assertTrue(vm.viewState.value.isLiked)
    }
}
