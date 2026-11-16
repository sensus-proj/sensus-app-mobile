package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.presentation.merchant.MerchantDashboardViewIntent
import com.example.sensus.presentation.merchant.MerchantDashboardViewModel
import com.example.sensus.presentation.merchant.MerchantNewOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantNewOfferViewModel
import com.example.sensus.presentation.merchant.MerchantOfferFilter
import com.example.sensus.presentation.merchant.MerchantProfileViewIntent
import com.example.sensus.presentation.merchant.MerchantProfileViewModel
import com.example.sensus.presentation.merchant.MerchantRegisterViewEffect
import com.example.sensus.presentation.merchant.MerchantRegisterViewIntent
import com.example.sensus.presentation.merchant.MerchantRegisterViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class MerchantViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    @Test
    fun merchantNewOffer_publishesOffer() = runTest {
        val useCase = mockk<CreateOfferUseCase>()
        val input = CreateOfferInput(
            "Oferta",
            "",
            StoreCategory.FOOD,
            10.0,
            5.0,
            3,
            Long.MAX_VALUE
        )
        val offer = ProductOffer("1", "Oferta", "Mi Tienda", "Comida", 10.0, 5.0, 50, 3, 0)
        coEvery { useCase(input, null) } returns Result.Success(offer)
        val vm = MerchantNewOfferViewModel(useCase)

        vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { useCase(input, null) }
        assertFalse(vm.viewState.value.isPublishing)
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
    fun merchantDashboardAndProfile_loadRealMerchantFromBackend() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateMerchantProfileUseCase>(relaxed = true)
        val getMyMerchantUseCase = mockk<GetMyMerchantUseCase>()
        val realMerchant = MerchantProfile(
            id = "m_1",
            usuarioId = "u_1",
            nombreComercial = "Papelería UTTT",
            direccion = "Blvd. Tula-Iturbe 10",
            codigoPostal = "42800",
            latitud = 20.0536,
            longitud = -99.3436
        )
        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Sample")
        coEvery { offersRepo.getOffers() } returns emptyList()
        coEvery { getMyMerchantUseCase() } returns Result.Success(realMerchant)

        val dashVm = MerchantDashboardViewModel(storeRepo, offersRepo, getMyMerchantUseCase)
        val profileVm = MerchantProfileViewModel(storeRepo, updateUseCase, getMyMerchantUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Papelería UTTT", dashVm.viewState.value.store.name)
        assertEquals("Blvd. Tula-Iturbe 10", dashVm.viewState.value.store.address)
        assertEquals("Papelería UTTT", profileVm.viewState.value.store.name)
        assertEquals("42800", profileVm.viewState.value.store.postalCode)
    }

    @Test
    fun merchantRegister_opensMapPicker_movesPin_andAppliesResolvedAddress() = runTest {
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val activateUseCase = mockk<ActivateMerchantUseCase>(relaxed = true)
        val getLocationUseCase = mockk<GetUserLocationUseCase>()
        val resolveUseCase = mockk<ResolveAddressFromCoordinatesUseCase>()

        every { getLocationUseCase.hasPermission() } returns true
        coEvery { getLocationUseCase() } returns UserLocation(20.0536, -99.3436)
        coEvery { getLocationUseCase.fresh() } returns UserLocation(20.0536, -99.3436)
        coEvery { resolveUseCase(20.0536, -99.3436) } returns Result.Success(
            ResolvedAddress("Av. Colegio Militar 12, Centro", "42800", 20.0536, -99.3436)
        )
        coEvery { resolveUseCase(20.0550, -99.3450) } returns Result.Success(
            ResolvedAddress("Calle Hidalgo 45, El Salitre", "42801", 20.0550, -99.3450)
        )

        val vm = MerchantRegisterViewModel(sessionRepo, activateUseCase, getLocationUseCase, resolveUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.hasRealLocation)

        vm.dispatchIntent(MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.showMapPicker)
        assertTrue(vm.viewState.value.recenterSignal >= 2)
        assertEquals("Av. Colegio Militar 12, Centro", vm.viewState.value.resolvedAddressPreview)
        assertEquals("42800", vm.viewState.value.resolvedPostalPreview)

        vm.dispatchIntent(MerchantRegisterViewIntent.MoveMapPin(20.0550, -99.3450))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Calle Hidalgo 45, El Salitre", vm.viewState.value.resolvedAddressPreview)

        vm.viewEffect.test {
            vm.dispatchIntent(MerchantRegisterViewIntent.ConfirmMapLocation(20.0550, -99.3450))
            val effect = awaitItem()
            assertTrue(effect is MerchantRegisterViewEffect.ApplyResolvedAddress)
            effect as MerchantRegisterViewEffect.ApplyResolvedAddress
            assertEquals("Calle Hidalgo 45, El Salitre", effect.address)
            assertEquals("42801", effect.postalCode)
        }

        assertFalse(vm.viewState.value.showMapPicker)
        assertEquals(20.0550, vm.viewState.value.selectedLatitude!!, 0.0001)
        assertEquals(-99.3450, vm.viewState.value.selectedLongitude!!, 0.0001)
    }

    @Test
    fun merchantRegister_manualFallback_recenter_and_dismiss() = runTest {
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val activateUseCase = mockk<ActivateMerchantUseCase>(relaxed = true)
        val getLocationUseCase = mockk<GetUserLocationUseCase>()
        val resolveUseCase = mockk<ResolveAddressFromCoordinatesUseCase>()

        every { getLocationUseCase.hasPermission() } returns false
        coEvery { resolveUseCase(20.0536, -99.3436) } returns Result.Failure(
            AppError.NotFound("Sin resultados de geocodificador")
        )
        coEvery { getLocationUseCase.fresh() } returns UserLocation(20.0540, -99.3440)
        coEvery { resolveUseCase(20.0540, -99.3440) } returns Result.Success(
            ResolvedAddress("Carretera Tula-Tepeji Km 3, El Carmen", "42830", 20.0540, -99.3440)
        )

        val vm = MerchantRegisterViewModel(sessionRepo, activateUseCase, getLocationUseCase, resolveUseCase)

        vm.dispatchIntent(MerchantRegisterViewIntent.OpenMapPickerManualFallback)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.showMapPicker)
        assertEquals("Sin resultados de geocodificador", vm.viewState.value.locationHelperHint)

        vm.dispatchIntent(MerchantRegisterViewIntent.RecenterPickerOnCurrentLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(20.0540, vm.viewState.value.pickerLatitude, 0.0001)
        assertEquals("Carretera Tula-Tepeji Km 3, El Carmen", vm.viewState.value.resolvedAddressPreview)

        vm.dispatchIntent(MerchantRegisterViewIntent.DismissMapPicker)
        assertFalse(vm.viewState.value.showMapPicker)
    }

    @Test
    fun merchantRegister_activatesMerchant_withCoordinates_andHandlesError() = runTest {
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val activateUseCase = mockk<ActivateMerchantUseCase>()
        val getLocationUseCase = mockk<GetUserLocationUseCase>(relaxed = true)
        val resolveUseCase = mockk<ResolveAddressFromCoordinatesUseCase>()

        coEvery { resolveUseCase(19.435, -99.141) } returns Result.Success(
            ResolvedAddress("Av. Juárez 45", "06010", 19.435, -99.141)
        )
        val createdProfile = MerchantProfile(
            id = "m1",
            usuarioId = "u1",
            nombreComercial = "Panadería Sol",
            direccion = "Av. Juárez 45",
            codigoPostal = "06010",
            latitud = 19.435,
            longitud = -99.141
        )
        coEvery {
            activateUseCase("Panadería Sol", "Av. Juárez 45", "06010", 19.435, -99.141)
        } returns Result.Success(createdProfile)

        val vm = MerchantRegisterViewModel(sessionRepo, activateUseCase, getLocationUseCase, resolveUseCase)

        vm.viewEffect.test {
            vm.dispatchIntent(MerchantRegisterViewIntent.ConfirmMapLocation(19.435, -99.141))
            awaitItem() // ApplyResolvedAddress

            vm.dispatchIntent(
                MerchantRegisterViewIntent.ActivateMerchant(
                    storeName = "Panadería Sol",
                    address = "Av. Juárez 45",
                    postalCode = "06010"
                )
            )
            val successEffect = awaitItem()
            assertTrue(successEffect is MerchantRegisterViewEffect.ActivationSuccess)
            assertEquals("Panadería Sol", (successEffect as MerchantRegisterViewEffect.ActivationSuccess).storeName)
        }

        coVerify(exactly = 1) { sessionRepo.setMerchantStatus(true, "Panadería Sol") }
        coVerify(exactly = 1) { sessionRepo.syncSession() }

        coEvery {
            activateUseCase("Tienda Error", "Calle X 123", "06600", 19.435, -99.141)
        } returns Result.Failure(AppError.Validation("Ya cuentas con un comercio."))

        vm.dispatchIntent(
            MerchantRegisterViewIntent.ActivateMerchant(
                storeName = "Tienda Error",
                address = "Calle X 123",
                postalCode = "06600"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Ya cuentas con un comercio.", vm.viewState.value.errorMessage)

        vm.dispatchIntent(MerchantRegisterViewIntent.ClearError)
        assertNull(vm.viewState.value.errorMessage)
    }

    @Test
    fun merchantDashboard_adjustingStockToZeroPausesOffer_andDoesNotRevivePausedOnRefresh() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val offersRepo = mockk<OffersRepository>()
        val getMyMerchantUseCase = mockk<GetMyMerchantUseCase>()
        val activeOffer = ProductOffer("o_1", "Pan Dulce", "Mi Tienda", "Panadería", 40.0, 20.0, 50, 5, 0)
        val pausedOffer = activeOffer.copy(unitsLeft = 0, status = OfferLifecycleStatus.PAUSED)

        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Mi Tienda")
        coEvery { getMyMerchantUseCase() } returns Result.Failure(AppError.Network("offline"))
        coEvery { offersRepo.getOffers() } returns listOf(activeOffer)
        coEvery { offersRepo.fetchMerchantOffers() } returns Result.Success(listOf(activeOffer))
        coEvery { offersRepo.updateOfferStock(activeOffer, 0) } returns Result.Success(pausedOffer)

        val vm = MerchantDashboardViewModel(storeRepo, offersRepo, getMyMerchantUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.viewState.value.activeStoreOffers.size)

        vm.dispatchIntent(MerchantDashboardViewIntent.AdjustOfferStock(activeOffer, 0))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.activeStoreOffers.isEmpty())
        assertEquals(0, vm.viewState.value.store.activeOffersCount)
        assertTrue(vm.viewState.value.filteredOffers.isEmpty())

        vm.dispatchIntent(MerchantDashboardViewIntent.SelectOfferFilter(MerchantOfferFilter.PAUSED))
        assertEquals(1, vm.viewState.value.filteredOffers.size)

        coEvery { offersRepo.fetchMerchantOffers() } returns Result.Success(
            listOf(activeOffer.copy(unitsLeft = 5, status = OfferLifecycleStatus.PAUSED))
        )
        vm.dispatchIntent(MerchantDashboardViewIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.activeStoreOffers.isEmpty())
        assertEquals(1, vm.viewState.value.filteredOffers.size)
    }
}
