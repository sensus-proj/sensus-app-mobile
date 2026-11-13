package com.example.sensus.presentation

import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.presentation.merchant.MerchantDashboardViewIntent
import com.example.sensus.presentation.merchant.MerchantDashboardViewModel
import com.example.sensus.presentation.merchant.MerchantNewOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantNewOfferViewModel
import com.example.sensus.presentation.merchant.MerchantProfileViewIntent
import com.example.sensus.presentation.merchant.MerchantProfileViewModel
import com.example.sensus.presentation.merchant.MerchantRegisterViewIntent
import com.example.sensus.presentation.merchant.MerchantRegisterViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantViewModelsTest {

    @Test
    fun merchantDashboard_loadsStoreAndOffers() {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Mi Tienda")
        coEvery { offersRepo.getOffers() } returns listOf(
            ProductOffer("1", "Offer 1", "Mi Tienda", "Todo", 10.0, 5.0, 50, 10, 100)
        )

        val vm = MerchantDashboardViewModel(storeRepo, offersRepo)
        assertEquals("Mi Tienda", vm.viewState.value.store.name)
        assertEquals(1, vm.viewState.value.activeStoreOffers.size)

        vm.dispatchIntent(MerchantDashboardViewIntent.Refresh)
        assertEquals("Mi Tienda", vm.viewState.value.store.name)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun merchantNewOffer_publishesOffer() = kotlinx.coroutines.test.runTest {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler))
        try {
            val useCase = mockk<com.example.sensus.domain.usecase.CreateOfferUseCase>()
            val input = com.example.sensus.domain.model.CreateOfferInput(
                "Oferta",
                "",
                com.example.sensus.domain.model.StoreCategory.FOOD,
                10.0,
                5.0,
                3,
                Long.MAX_VALUE
            )
            val offer = ProductOffer("1", "Oferta", "Mi Tienda", "Comida", 10.0, 5.0, 50, 3, 0)
            coEvery { useCase(input, null) } returns com.example.sensus.common.Result.Success(offer)
            val vm = MerchantNewOfferViewModel(useCase)
            vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
            io.mockk.coVerify(exactly = 1) { useCase(input, null) }
            org.junit.Assert.assertFalse(vm.viewState.value.isPublishing)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test
    fun merchantProfile_updatesStore() {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateMerchantProfileUseCase>(relaxed = true)
        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Initial Store")

        val vm = MerchantProfileViewModel(storeRepo, updateUseCase)
        assertEquals("Initial Store", vm.viewState.value.store.name)

        val updated = MerchantStore(name = "Updated Store")
        vm.dispatchIntent(MerchantProfileViewIntent.UpdateStore(updated))
        assertEquals("Updated Store", vm.viewState.value.store.name)
    }

    @Test
    fun merchantRegister_activatesMerchant() {
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val vm = MerchantRegisterViewModel(sessionRepo)

        vm.dispatchIntent(
            MerchantRegisterViewIntent.ActivateMerchant(
                "Tienda",
                "Abarrotes",
                "Dir",
                "9-20",
                "5512345678"
            )
        )
    }
}