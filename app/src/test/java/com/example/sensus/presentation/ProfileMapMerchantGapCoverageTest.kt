package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetNearbyOffersUseCase
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.buyer.BuyerMapViewEffect
import com.example.sensus.presentation.buyer.BuyerMapViewIntent
import com.example.sensus.presentation.buyer.BuyerMapViewModel
import com.example.sensus.presentation.buyer.BuyerProfileViewEffect
import com.example.sensus.presentation.buyer.BuyerProfileViewIntent
import com.example.sensus.presentation.buyer.BuyerProfileViewModel
import com.example.sensus.presentation.merchant.MerchantRegisterViewIntent
import com.example.sensus.presentation.merchant.MerchantRegisterViewModel
import com.example.sensus.presentation.merchant.uploadMerchantFacadePhotoIfProvided
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class ProfileMapMerchantGapCoverageTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun buyerProfile_guestPathsUpdateErrorAndLogout() = runTest(dispatcher) {
        val profileRepo = mockk<ProfileRepository>()
        val updateProfile = mockk<UpdateProfileUseCase>()
        val logout = mockk<LogoutUseCase>(relaxed = true)
        val getMyMerchant = mockk<GetMyMerchantUseCase>()
        val sessionFlow = MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))
        val sessionRepo = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns sessionFlow }

        val guestVm = BuyerProfileViewModel(profileRepo, updateProfile, sessionRepo, logout, getMyMerchant)
        advanceUntilIdle()
        guestVm.dispatchIntent(BuyerProfileViewIntent.LoadProfile)
        assertTrue(guestVm.viewState.value.isGuest)
        guestVm.dispatchIntent(BuyerProfileViewIntent.UpdateName("Nuevo"))
        assertEquals("Inicia sesión para editar tu perfil.", guestVm.viewState.value.errorMessage)
        guestVm.dispatchIntent(BuyerProfileViewIntent.Logout)
        advanceUntilIdle()
        coVerify { logout() }

        coEvery { profileRepo.getProfile() } returns Result.Success(UserProfile("u1", "Ana", "ana@test.com", roles = listOf("Consumidor")))
        coEvery { updateProfile("Ana2", any()) } returns Result.Failure(AppError.Network("Error al actualizar"))
        sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, activeEmail = "ana@test.com")
        advanceUntilIdle()
        guestVm.dispatchIntent(BuyerProfileViewIntent.UpdateName("Ana2"))
        advanceUntilIdle()
        assertEquals("Error al actualizar", guestVm.viewState.value.errorMessage)
    }

    @Test
    fun buyerProfile_reopenStorePathsAndSyncStoreOpenMismatch() = runTest(dispatcher) {
        val profileRepo = mockk<ProfileRepository>()
        val getMyMerchant = mockk<GetMyMerchantUseCase>()
        val updater = mockk<UpdateMerchantProfileUseCase>()
        val merchant = MerchantProfile("m1", "u1", "Espiga", "Calle 1", "42800", activo = false)
        val sessionFlow = MutableStateFlow(
            SessionState(
                isSessionActive = true,
                isGuest = false,
                activeEmail = "m@test.com",
                isMerchantRegistered = true,
                isStoreOpen = true
            )
        )
        val sessionRepo = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns sessionFlow }
        coEvery { profileRepo.getProfile() } returns Result.Success(UserProfile("u1", "Ana", "m@test.com", roles = listOf("Comerciante")))
        coEvery { getMyMerchant() } returns
            Result.Success(merchant) andThen
            Result.Failure(AppError.Network("Sin tienda")) andThen
            Result.Failure(AppError.Network("Sin tienda")) andThen
            Result.Success(merchant) andThen
            Result.Success(merchant)
        coEvery { updater(any()) } returns
            Result.Failure(AppError.Network("Fallo reapertura")) andThen
            Result.Success(merchant.copy(activo = false))

        val vmNullUpdater = BuyerProfileViewModel(profileRepo, mockk(), sessionRepo, mockk(relaxed = true), getMyMerchant)
        advanceUntilIdle()
        coVerify { sessionRepo.setStoreOpen(false) }
        vmNullUpdater.viewEffect.test {
            vmNullUpdater.dispatchIntent(BuyerProfileViewIntent.ReopenStore)
            assertEquals(BuyerProfileViewEffect.StoreReopened, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val vm = BuyerProfileViewModel(profileRepo, mockk(), sessionRepo, mockk(relaxed = true), getMyMerchant, updater)
        advanceUntilIdle()
        vm.viewEffect.test {
            repeat(3) {
                vm.dispatchIntent(BuyerProfileViewIntent.ReopenStore)
                advanceUntilIdle()
            }
            assertEquals("No se pudo cargar tu tienda para reabrirla.", (awaitItem() as BuyerProfileViewEffect.ShowMessage).message)
            assertEquals("Fallo reapertura", (awaitItem() as BuyerProfileViewEffect.ShowMessage).message)
            assertTrue((awaitItem() as BuyerProfileViewEffect.ShowMessage).message.contains("aún no guardó"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun buyerMap_manualLocationMissingPermissionAndLocationExceptions() = runTest(dispatcher) {
        val locations = mockk<LocationRepository>()
        val sessionRepo = mockk<SessionRepository>(relaxed = true) {
            every { sessionState } returns MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
        }
        val getLayer = mockk<GetPreferredMapLayerUseCase> {
            every { peekCached() } returns MapLayer.STANDARD
            coEvery { this@mockk.invoke() } returns MapLayer.STANDARD
        }
        val getMerchants = mockk<GetNearbyMerchantsUseCase> {
            coEvery { this@mockk.invoke(any(), any(), any(), any()) } returns Result.Success(emptyList())
        }
        val getOffers = mockk<GetNearbyOffersUseCase> {
            coEvery { readCachedOffers() } returns emptyList()
            coEvery { getPreferredRadiusKm() } returns null
            coEvery { this@mockk.invoke(any(), any(), any()) } returns Result.Success(emptyList())
        }
        every { locations.hasLocationPermission() } returns false andThen false andThen true andThen true andThen true
        every { locations.isLocationEnabled() } returns true
        coEvery { locations.getFreshLocation() } throws SecurityException("denied") andThenThrows IllegalStateException("gps")

        val vm = BuyerMapViewModel(locations, sessionRepo, getLayer, mockk(relaxed = true), getMerchants, getOffers)
        advanceUntilIdle()
        vm.dispatchIntent(BuyerMapViewIntent.ManualLocationSelected(UserLocation(20.1, -99.2)))
        assertEquals(20.1, vm.viewState.value.userLocation.latitude, 0.001)

        vm.viewEffect.test {
            vm.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
            advanceUntilIdle()
            assertFalse((awaitItem() as BuyerMapViewEffect.ShowLocationMessage).isGpsDisabled)

            vm.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
            advanceUntilIdle()
            assertFalse(vm.viewState.value.isLocating)

            vm.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
            advanceUntilIdle()
            assertFalse(vm.viewState.value.isLocating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun merchantRegister_pickerGpsSavedCoordsBlankPostalAndPhotoErrors() = runTest(dispatcher) {
        val getUserLoc = mockk<GetUserLocationUseCase>()
        val reverseGeo = mockk<ResolveAddressFromCoordinatesUseCase>()
        val mediaRepo = mockk<MediaRepository>()
        val compressor = mockk<ImageCompressor>()
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        every { getUserLoc.hasPermission() } returns false andThen true
        coEvery { getUserLoc() } returns UserLocation(20.0, -99.0)
        coEvery { getUserLoc.fresh() } returns UserLocation(20.01, -99.01)
        coEvery { reverseGeo(any(), any()) } returns Result.Success(ResolvedAddress("Calle Central", "", 20.5, -99.5))
        coEvery { compressor.compressForUpload(any()) } returns null andThen byteArrayOf(1, 2)
        coEvery { mediaRepo.uploadAndModerate(any(), any()) } returns Result.Failure(AppError.Network("Moderación caída"))

        val vm = MerchantRegisterViewModel(
            sessionRepo,
            mockk<ActivateMerchantUseCase>(),
            getUserLoc,
            reverseGeo,
            mediaRepo,
            compressor
        )
        advanceUntilIdle()

        vm.dispatchIntent(MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation)
        advanceUntilIdle()
        assertTrue(vm.viewState.value.hasRealLocation)

        vm.dispatchIntent(MerchantRegisterViewIntent.ConfirmMapLocation(20.5, -99.5))
        advanceUntilIdle()
        assertEquals("", vm.viewState.value.resolvedPostalPreview.orEmpty())

        vm.dispatchIntent(MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation)
        advanceUntilIdle()
        assertEquals(20.5, vm.viewState.value.pickerLatitude, 0.001)

        vm.dispatchIntent(MerchantRegisterViewIntent.ActivateMerchant("Tienda", "Calle 1", "42800", byteArrayOf(9)))
        advanceUntilIdle()
        assertTrue(vm.viewState.value.errorMessage != null)

        assertTrue(uploadMerchantFacadePhotoIfProvided(byteArrayOf(1), mediaRepo, null) is Result.Failure)
        assertTrue(uploadMerchantFacadePhotoIfProvided(byteArrayOf(1), mediaRepo, compressor) is Result.Failure)
        assertNull((uploadMerchantFacadePhotoIfProvided(null, mediaRepo, compressor) as Result.Success).data)
    }
}
