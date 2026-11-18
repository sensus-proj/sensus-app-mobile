package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.MerchantActivationInput
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.buyer.BuyerProfileViewEffect
import com.example.sensus.presentation.buyer.BuyerProfileViewIntent
import com.example.sensus.presentation.buyer.BuyerProfileViewModel
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
            "Pan dulce fresco",
            StoreCategory.FOOD,
            10.0,
            5.0,
            3,
            Long.MAX_VALUE
        )
        val imageBytes = byteArrayOf(1, 2, 3)
        val offer = ProductOffer("1", "Oferta", "Mi Tienda", "Comida", 10.0, 5.0, 50, 3, 0)
        coEvery { useCase(input, imageBytes) } returns Result.Success(offer)
        val vm = MerchantNewOfferViewModel(useCase)

        vm.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(input, imageBytes))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { useCase(input, imageBytes) }
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
        val createdProfile = MerchantProfile("m1", "u1", "Panadería Sol", "Av. Juárez 45", "06010", latitud = 19.435, longitud = -99.141)
        val coords = MerchantCoordinates(19.435, -99.141)
        coEvery {
            activateUseCase(MerchantActivationInput("Panadería Sol", "Av. Juárez 45", "06010", coords))
        } returns Result.Success(createdProfile)

        val vm = MerchantRegisterViewModel(sessionRepo, activateUseCase, getLocationUseCase, resolveUseCase)

        vm.viewEffect.test {
            vm.dispatchIntent(MerchantRegisterViewIntent.ConfirmMapLocation(19.435, -99.141))
            awaitItem() // ApplyResolvedAddress

            vm.dispatchIntent(MerchantRegisterViewIntent.ActivateMerchant("Panadería Sol", "Av. Juárez 45", "06010"))
            val successEffect = awaitItem()
            assertTrue(successEffect is MerchantRegisterViewEffect.ActivationSuccess)
            assertEquals("Panadería Sol", (successEffect as MerchantRegisterViewEffect.ActivationSuccess).storeName)
        }

        coVerify(exactly = 1) { sessionRepo.setMerchantStatus(true, "Panadería Sol") }
        coVerify(exactly = 1) { sessionRepo.syncSession() }

        coEvery {
            activateUseCase(MerchantActivationInput("Tienda Error", "Calle X 123", "06600", coords))
        } returns Result.Failure(AppError.Validation("Ya cuentas con un comercio."))

        vm.dispatchIntent(MerchantRegisterViewIntent.ActivateMerchant("Tienda Error", "Calle X 123", "06600"))
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

    @Test
    fun merchantRegister_uploadsFacadePhoto_beforeActivatingMerchant() = runTest {
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val activateUseCase = mockk<ActivateMerchantUseCase>()
        val getLocationUseCase = mockk<GetUserLocationUseCase>(relaxed = true)
        val resolveUseCase = mockk<ResolveAddressFromCoordinatesUseCase>(relaxed = true)
        val mediaRepo = mockk<MediaRepository>()
        val compressor = mockk<ImageCompressor>()

        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(7, 8)
        coEvery { mediaRepo.uploadAndModerate(any(), any()) } returns Result.Success(
            UploadedMedia("media-facade", "https://cdn.sensus.com/fachada.jpg")
        )
        coEvery {
            activateUseCase(
                MerchantActivationInput(
                    nombreComercial = "Tienda Foto",
                    direccion = "Av. Juárez 45",
                    codigoPostal = "42800",
                    fotoFachadaUrl = "https://cdn.sensus.com/fachada.jpg"
                )
            )
        } returns Result.Success(
            MerchantProfile("m2", "u2", "Tienda Foto", "Av. Juárez 45", "42800", fotoFachadaUrl = "https://cdn.sensus.com/fachada.jpg")
        )

        val vm = MerchantRegisterViewModel(sessionRepo, activateUseCase, getLocationUseCase, resolveUseCase, mediaRepo, compressor)
        vm.dispatchIntent(
            MerchantRegisterViewIntent.ActivateMerchant(
                storeName = "Tienda Foto",
                address = "Av. Juárez 45",
                postalCode = "42800",
                facadePhotoBytes = byteArrayOf(1, 2, 3)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            activateUseCase(
                MerchantActivationInput(
                    nombreComercial = "Tienda Foto",
                    direccion = "Av. Juárez 45",
                    codigoPostal = "42800",
                    fotoFachadaUrl = "https://cdn.sensus.com/fachada.jpg"
                )
            )
        }
    }

    @Test
    fun merchantProfile_updateFacadePhoto_uploadsAndUpdatesStorePhotoUrl() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateMerchantProfileUseCase>()
        val getMyMerchantUseCase = mockk<GetMyMerchantUseCase>()
        val mediaRepo = mockk<MediaRepository>()
        val compressor = mockk<ImageCompressor>()

        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Mi Tienda", address = "Av. Principal 10", postalCode = "42800")
        coEvery { getMyMerchantUseCase() } returns Result.Failure(AppError.Network("offline"))
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(9, 10)
        coEvery { mediaRepo.uploadAndModerate(any(), any()) } returns Result.Success(
            UploadedMedia("media-up", "https://cdn.sensus.com/nueva-fachada.jpg")
        )
        coEvery {
            updateUseCase(
                MerchantProfileUpdateInput(
                    nombreComercial = "Mi Tienda",
                    direccion = "Av. Principal 10",
                    codigoPostal = "42800",
                    horarios = emptyList(),
                    coordinates = null,
                    fotoFachadaUrl = "https://cdn.sensus.com/nueva-fachada.jpg"
                )
            )
        } returns Result.Success(
            MerchantProfile("m1", "u1", "Mi Tienda", "Av. Principal 10", "42800", fotoFachadaUrl = "https://cdn.sensus.com/nueva-fachada.jpg")
        )

        val vm = MerchantProfileViewModel(storeRepo, updateUseCase, getMyMerchantUseCase, mediaRepo, compressor)
        vm.dispatchIntent(MerchantProfileViewIntent.UpdateFacadePhoto(byteArrayOf(4, 5, 6)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://cdn.sensus.com/nueva-fachada.jpg", vm.viewState.value.store.photoUrl)
        assertFalse(vm.viewState.value.isUpdatingPhoto)
    }

    @Test
    fun merchantProfile_toggleStoreOpen_persistsAndRevertsOnFailure() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateMerchantProfileUseCase>()
        val getMyMerchantUseCase = mockk<GetMyMerchantUseCase>()
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        every { sessionRepo.sessionState } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.example.sensus.domain.repository.SessionState(isMerchantRegistered = true, isStoreOpen = true)
        )
        val profile = MerchantProfile("m1", "u1", "Mi Tienda", "Av. Principal 10", "42800", 20.05, -99.34, activo = false)
        coEvery { storeRepo.getSampleStore() } returns MerchantStore(name = "Mi Tienda", address = "Av. Principal 10", postalCode = "42800")
        coEvery { getMyMerchantUseCase() } returns Result.Success(profile.copy(activo = true))
        coEvery { updateUseCase(any()) } returns Result.Success(profile)

        val vm = MerchantProfileViewModel(storeRepo, updateUseCase, getMyMerchantUseCase, null, null, sessionRepo)
        vm.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.viewState.value.store.isOpen)
        coVerify(exactly = 1) { sessionRepo.setStoreOpen(false) }

        coEvery { updateUseCase(any()) } returns Result.Failure(AppError.Network("Sin red"))
        vm.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.viewState.value.store.isOpen)
        assertEquals("Sin red", vm.viewState.value.errorMessage)
    }

    @Test
    fun buyerProfile_reopenStore_activatesStoreAndEmitsEffect() = runTest {
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val updateProfileUseCase = mockk<UpdateProfileUseCase>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val logoutUseCase = mockk<LogoutUseCase>(relaxed = true)
        val getMyMerchant = mockk<GetMyMerchantUseCase>()
        val updateMerchant = mockk<UpdateMerchantProfileUseCase>()
        every { sessionRepo.sessionState } returns kotlinx.coroutines.flow.MutableStateFlow(
            SessionState(isSessionActive = true, isMerchantRegistered = true, merchantStoreName = "Mi Tienda", isStoreOpen = false)
        )
        val closed = MerchantProfile("m1", "u1", "Mi Tienda", "Av. Principal 10", "42800", 20.05, -99.34, activo = false)
        coEvery { getMyMerchant() } returns Result.Success(closed)
        coEvery { updateMerchant(any()) } returns Result.Success(closed.copy(activo = true))

        val vm = BuyerProfileViewModel(profileRepo, updateProfileUseCase, sessionRepo, logoutUseCase, getMyMerchant, updateMerchant)
        vm.viewEffect.test {
            vm.dispatchIntent(BuyerProfileViewIntent.ReopenStore)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(BuyerProfileViewEffect.StoreReopened, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(vm.viewState.value.isStoreOpen)
        coVerify(exactly = 1) { sessionRepo.setStoreOpen(true) }
    }
}
