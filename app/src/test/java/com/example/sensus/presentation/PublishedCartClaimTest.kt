package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.usecase.PrepareCartClaimUseCase
import com.example.sensus.presentation.buyer.CartViewEffect
import com.example.sensus.presentation.buyer.CartViewIntent
import com.example.sensus.presentation.buyer.CartViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublishedCartClaimTest {
    private val dispatcher = StandardTestDispatcher()
    private val cart = mockk<CartRepository>(relaxed = true)
    private val offer = ProductOffer("1", "Pan", "Tienda", "Abarrotes", 100.0, 50.0, 50, 2, 100)
    private val state = MutableStateFlow(StoreCart(items = listOf(CartItem(offer, 1))))

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { cart.cart } returns state
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkoutOnlyNavigatesToConfirmationAndNeverCreatesQrOrClearsCart() = runTest(dispatcher) {
        val vm = CartViewModel(cart, PrepareCartClaimUseCase())
        dispatcher.scheduler.advanceUntilIdle()
        vm.viewEffect.test {
            vm.dispatchIntent(CartViewIntent.CheckoutApartado)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(CartViewEffect.OpenClaim(offer), awaitItem())
        }
        verify(exactly = 0) { cart.createApartadoOrder() }
        verify(exactly = 0) { cart.clearCart() }
        assertEquals(state.value, vm.viewState.value.storeCart)
    }

    @Test
    fun checkoutMultiItemSameStoreEmitsAllItemsWithoutGeneratingLocalQr() = runTest(dispatcher) {
        val second = offer.copy(id = "2", name = "Café")
        val multiItems = listOf(CartItem(offer, 4), CartItem(second, 2))
        state.value = StoreCart(items = multiItems)
        val vm = CartViewModel(cart, PrepareCartClaimUseCase())
        dispatcher.scheduler.advanceUntilIdle()
        vm.viewEffect.test {
            vm.dispatchIntent(CartViewIntent.CheckoutApartado)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(CartViewEffect.OpenClaim(offer, multiItems), awaitItem())
        }
        verify(exactly = 0) { cart.createApartadoOrder() }
        verify(exactly = 0) { cart.clearCart() }
    }
}
