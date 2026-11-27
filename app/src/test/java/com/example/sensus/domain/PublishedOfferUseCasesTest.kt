package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.repository.OfferDetailsRepository
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.PrepareCartClaimUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishedOfferUseCasesTest {
    private val offer = ProductOffer("offer-1", "Pan", "Tienda", "Abarrotes", 100.0, 50.0, 50, 2, 100, expiresAtMillis = 1000)
    private val detail = OfferDetails(offer, 1, false, 1)

    @Test
    fun getAndViewValidateIdentifiersAndDelegate() = runTest {
        val repository = mockk<OfferDetailsRepository>()
        coEvery { repository.getDetail("offer-1") } returns Result.Success(detail)
        coEvery { repository.registerView("offer-1") } returns Result.Success(Unit)
        val get = GetOfferDetailUseCase(repository)
        val view = RegisterOfferViewUseCase(repository)
        assertTrue(get(" ") is Result.Failure)
        assertTrue(view("") is Result.Failure)
        assertEquals(detail, (get("offer-1") as Result.Success).data)
        assertTrue(view("offer-1") is Result.Success)
        coVerify(exactly = 1) { repository.getDetail(any()) }
        coVerify(exactly = 1) { repository.registerView(any()) }
    }

    @Test
    fun availabilityRequiresStockDatesPublishedStateAndNoActiveClaim() {
        assertTrue(detail.canClaim(500))
        assertFalse(detail.canClaim(0))
        assertFalse(detail.canClaim(1000))
        assertFalse(detail.copy(hasActiveClaim = true).canClaim(500))
        assertFalse(detail.copy(offer = offer.copy(unitsLeft = 0)).canClaim(500))
        assertFalse(detail.copy(offer = offer.copy(status = OfferLifecycleStatus.PAUSED)).canClaim(500))
        assertFalse(detail.copy(offer = offer.copy(expiresAtMillis = null)).canClaim(500))
    }

    @Test
    fun cartPreparesSingleStoreMultiItemAndMultiQuantityWithoutGeneratingLocalQr() {
        val action = PrepareCartClaimUseCase()
        val single = StoreCart(items = listOf(CartItem(offer, 1)))
        assertEquals(single.items, (action(single) as Result.Success).data)

        val multiQty = StoreCart(items = listOf(CartItem(offer, 4)))
        assertEquals(multiQty.items, (action(multiQty) as Result.Success).data)

        val sameStoreMultiProduct = StoreCart(
            items = listOf(CartItem(offer, 2), CartItem(offer.copy(id = "2"), 3))
        )
        assertEquals(sameStoreMultiProduct.items, (action(sameStoreMultiProduct) as Result.Success).data)

        assertTrue(action(StoreCart()) is Result.Failure)
        assertTrue(action(StoreCart(items = listOf(CartItem(offer, 0)))) is Result.Failure)
        assertTrue(
            action(
                StoreCart(
                    items = listOf(
                        CartItem(offer.copy(merchantId = "m-1"), 1),
                        CartItem(offer.copy(id = "2", merchantId = "m-2"), 1)
                    )
                )
            ) is Result.Failure
        )
    }
}
