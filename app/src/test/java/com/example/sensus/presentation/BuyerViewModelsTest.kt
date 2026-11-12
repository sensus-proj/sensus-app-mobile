package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.buyer.BuyerHomeViewEffect
import com.example.sensus.presentation.buyer.BuyerHomeViewIntent
import com.example.sensus.presentation.buyer.BuyerHomeViewModel
import com.example.sensus.presentation.buyer.BuyerMapViewEffect
import com.example.sensus.presentation.buyer.BuyerMapViewIntent
import com.example.sensus.presentation.buyer.BuyerMapViewModel
import com.example.sensus.presentation.buyer.BuyerProfileViewIntent
import com.example.sensus.presentation.buyer.BuyerProfileViewModel
import com.example.sensus.presentation.buyer.CartViewIntent
import com.example.sensus.presentation.buyer.CartViewModel
import com.example.sensus.presentation.buyer.CommunityViewModel
import com.example.sensus.presentation.buyer.NotificationsViewIntent
import com.example.sensus.presentation.buyer.NotificationsViewModel
import com.example.sensus.presentation.buyer.RouteMapViewIntent
import com.example.sensus.presentation.buyer.RouteMapViewModel
import com.example.sensus.presentation.buyer.SensiChatViewIntent
import com.example.sensus.presentation.buyer.SensiChatViewModel
import com.example.sensus.presentation.common.MapLayerStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuyerViewModelsTest {

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
    fun buyerHome_categorySelection_and_search_updatesState() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())
        coEvery { offersRepo.getOffers() } returns listOf(
            ProductOffer("1", "Offer 1", "Store", "Abarrotes", 10.0, 5.0, 50, 5, 200)
        )
        val vm = BuyerHomeViewModel(offersRepo, locationRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(BuyerHomeViewIntent.CategorySelected(StoreCategory.FOOD))
        assertEquals(StoreCategory.FOOD, vm.viewState.value.selectedCategory)

        vm.dispatchIntent(BuyerHomeViewIntent.SearchQueryChanged("pan"))
        assertEquals("pan", vm.viewState.value.searchQuery)

        vm.dispatchIntent(BuyerHomeViewIntent.RefreshOffers)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.viewState.value.offers.size)
    }

    @Test
    fun buyerHome_guestMode_clampsRadius_and_refreshesLocation() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState(isGuest = true))
        coEvery { locationRepo.hasLocationPermission() } returns true
        coEvery { locationRepo.getCurrentLocation() } returns UserLocation(19.5, -99.2)
        coEvery { locationRepo.getFreshLocation() } returns UserLocation(19.6, -99.3)

        val vm = BuyerHomeViewModel(offersRepo, locationRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.isGuest)
        assertEquals(19.5, vm.viewState.value.userLocation.latitude, 0.001)

        vm.viewEffect.test {
            vm.dispatchIntent(BuyerHomeViewIntent.RadiusSelected(4.5f))
            assertEquals(BuyerHomeViewEffect.RequireAuthForRadius, awaitItem())
            assertEquals(2.0f, vm.viewState.value.selectedRadiusKm, 0.001f)
            cancelAndIgnoreRemainingEvents()
        }

        vm.dispatchIntent(BuyerHomeViewIntent.RefreshLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(19.6, vm.viewState.value.userLocation.latitude, 0.001)
    }

    private fun createMapLayerUseCases(
        cached: MapLayer? = null,
        stored: MapLayer = MapLayer.STANDARD
    ): Pair<GetPreferredMapLayerUseCase, SavePreferredMapLayerUseCase> {
        val getUseCase = mockk<GetPreferredMapLayerUseCase>()
        val saveUseCase = mockk<SavePreferredMapLayerUseCase>(relaxed = true)
        every { getUseCase.peekCached() } returns cached
        coEvery { getUseCase() } returns stored
        return getUseCase to saveUseCase
    }

    @Test
    fun buyerMap_selection_and_permission() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val (getMapLayerUseCase, saveMapLayerUseCase) = createMapLayerUseCases()
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())

        val offer = ProductOffer("1", "Offer 1", "Store", "Abarrotes", 10.0, 5.0, 50, 5, 200)
        coEvery { offersRepo.getOffers() } returns listOf(offer)

        val vm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, getMapLayerUseCase, saveMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.selectedOffer)

        vm.dispatchIntent(BuyerMapViewIntent.OfferSelected(offer))
        assertEquals(offer, vm.viewState.value.selectedOffer)

        vm.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(true))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { sessionRepo.setLocationPermission(true) }
    }

    @Test
    fun buyerMap_guestMode_blocksRadiusAbove2Km_and_registeredAllows5KmAndLayers() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionFlow = MutableStateFlow(SessionState(isGuest = true))
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val (getMapLayerUseCase, saveMapLayerUseCase) = createMapLayerUseCases()
        coEvery { sessionRepo.sessionState } returns sessionFlow

        val vm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, getMapLayerUseCase, saveMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.isGuest)

        vm.viewEffect.test {
            vm.dispatchIntent(BuyerMapViewIntent.RadiusSelected(5.0f))
            assertEquals(BuyerMapViewEffect.RequireAuthForRadius, awaitItem())
            assertEquals(2.0f, vm.viewState.value.selectedRadiusKm, 0.001f)
            cancelAndIgnoreRemainingEvents()
        }

        // Invitado si puede reducir hasta el mínimo de 50 m (0.05 km) y <= 2 km
        vm.dispatchIntent(BuyerMapViewIntent.RadiusSelected(0.05f))
        assertEquals(0.05f, vm.viewState.value.selectedRadiusKm, 0.001f)
        vm.dispatchIntent(BuyerMapViewIntent.RadiusSelected(1.0f))
        assertEquals(1.0f, vm.viewState.value.selectedRadiusKm, 0.001f)

        // Usuario autenticado puede ampliar a 5 km y cambiar capas del mapa
        sessionFlow.value = SessionState(isGuest = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.viewState.value.isGuest)

        vm.dispatchIntent(BuyerMapViewIntent.RadiusSelected(5.0f))
        assertEquals(5.0f, vm.viewState.value.selectedRadiusKm, 0.001f)

        vm.dispatchIntent(BuyerMapViewIntent.LayerSelected(MapLayerStyle.SATELLITE))
        assertEquals(MapLayerStyle.SATELLITE, vm.viewState.value.selectedLayer)
        vm.dispatchIntent(BuyerMapViewIntent.LayerSelected(MapLayerStyle.DARK))
        assertEquals(MapLayerStyle.DARK, vm.viewState.value.selectedLayer)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { saveMapLayerUseCase(MapLayer.SATELLITE) }
        coVerify { saveMapLayerUseCase(MapLayer.DARK) }
    }

    @Test
    fun buyerMap_refreshLocation_fetchesFreshGps_and_warnsWhenGpsDisabled() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val (getMapLayerUseCase, saveMapLayerUseCase) = createMapLayerUseCases()
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())
        coEvery { locationRepo.hasLocationPermission() } returns true
        coEvery { locationRepo.isLocationEnabled() } returns false
        coEvery { locationRepo.getCurrentLocation() } returns UserLocation(20.1, -98.7)
        coEvery { locationRepo.getFreshLocation() } returns UserLocation(20.12, -98.73)

        val vm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, getMapLayerUseCase, saveMapLayerUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(20.1, vm.viewState.value.userLocation.latitude, 0.001)

        vm.viewEffect.test {
            vm.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
            val effect = awaitItem() as BuyerMapViewEffect.ShowLocationMessage
            assertTrue(effect.isGpsDisabled)
            cancelAndIgnoreRemainingEvents()
        }

        coEvery { locationRepo.isLocationEnabled() } returns true
        vm.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.hasRealLocation)
        assertEquals(20.12, vm.viewState.value.userLocation.latitude, 0.001)
    }

    @Test
    fun buyerMap_restoresSavedLayerImmediately_fromWarmCacheAndSqlCipher() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())

        val (warmGet, warmSave) = createMapLayerUseCases(cached = MapLayer.SATELLITE, stored = MapLayer.SATELLITE)
        val warmVm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, warmGet, warmSave)
        assertEquals(MapLayerStyle.SATELLITE, warmVm.viewState.value.selectedLayer)

        val (coldGet, coldSave) = createMapLayerUseCases(cached = null, stored = MapLayer.DARK)
        val coldVm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, coldGet, coldSave)
        assertEquals(MapLayerStyle.STANDARD, coldVm.viewState.value.selectedLayer)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MapLayerStyle.DARK, coldVm.viewState.value.selectedLayer)
    }

    @Test
    fun buyerMap_manualLayerSelection_winsOverLateInitialLoad() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())

        val deferredLayer = CompletableDeferred<MapLayer>()
        val getUseCase = mockk<GetPreferredMapLayerUseCase>()
        val saveUseCase = mockk<SavePreferredMapLayerUseCase>(relaxed = true)
        every { getUseCase.peekCached() } returns null
        coEvery { getUseCase() } coAnswers { deferredLayer.await() }

        val vm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo, getUseCase, saveUseCase)
        vm.dispatchIntent(BuyerMapViewIntent.LayerSelected(MapLayerStyle.TERRAIN))
        deferredLayer.complete(MapLayer.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MapLayerStyle.TERRAIN, vm.viewState.value.selectedLayer)
        coVerify(exactly = 1) { saveUseCase(MapLayer.TERRAIN) }
    }

    @Test
    fun cartViewModel_allOperations() = runTest {
        val cartRepo = mockk<CartRepository>(relaxed = true)
        val cartFlow = MutableStateFlow(StoreCart())
        coEvery { cartRepo.cart } returns cartFlow

        val offer = ProductOffer("1", "Offer 1", "Store", "Abarrotes", 10.0, 5.0, 50, 5, 200)
        val sampleOrder = com.example.sensus.domain.model.ApartadoOrder.fromSingleOffer(offer, 2)
        coEvery { cartRepo.createApartadoOrder() } returns sampleOrder

        val vm = CartViewModel(cartRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(CartViewIntent.AddOffer(offer, 2))
        coVerify { cartRepo.addToCart(offer, 2) }

        vm.dispatchIntent(CartViewIntent.UpdateQuantity("1", 3))
        coVerify { cartRepo.updateQuantity("1", 3) }

        vm.dispatchIntent(CartViewIntent.RemoveOffer("1"))
        coVerify { cartRepo.removeItem("1") }

        vm.dispatchIntent(CartViewIntent.CheckoutApartado)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.viewState.value.activeOrder)

        vm.dispatchIntent(CartViewIntent.ClearCart)
        coVerify { cartRepo.clearCart() }
    }

    @Test
    fun notifications_category_updatesState() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val vm = NotificationsViewModel(storeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(NotificationsViewIntent.CategorySelected("Ofertas"))
        assertEquals("Ofertas", vm.viewState.value.selectedCategory)
    }

    @Test
    fun sensiChat_sendMessage_addsMessage() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val vm = SensiChatViewModel(storeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Hola"))
        assertEquals(1, vm.viewState.value.messages.size)
        assertEquals("Hola", vm.viewState.value.messages.first().text)
    }

    @Test
    fun buyerProfile_loadAndUpdate() = runTest {
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateProfileUseCase>(relaxed = true)
        val user = UserProfile(id = "1", nombreCompleto = "Rodrigo", correo = "r@sensus.com")
        coEvery { profileRepo.getProfile() } returns Result.Success(user)
        coEvery { updateUseCase("Nuevo Nombre", any()) } returns Result.Success(user.copy(nombreCompleto = "Nuevo Nombre"))

        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState(isGuest = false, activeEmail = "r@sensus.com"))
        val vm = BuyerProfileViewModel(profileRepo, updateUseCase, sessionRepo, mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Rodrigo", vm.viewState.value.profile?.nombreCompleto)

        vm.dispatchIntent(BuyerProfileViewIntent.UpdateName("Nuevo Nombre"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Nuevo Nombre", vm.viewState.value.profile?.nombreCompleto)

        vm.dispatchIntent(BuyerProfileViewIntent.LoadProfile)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun buyerProfile_reactsToGuest_Registered_Logout_and_OfflineFallback() = runTest {
        val profileRepo = mockk<ProfileRepository>(relaxed = true)
        val updateUseCase = mockk<UpdateProfileUseCase>(relaxed = true)
        val sessionFlow = MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns sessionFlow

        val vm = BuyerProfileViewModel(profileRepo, updateUseCase, sessionRepo, mockk(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.isGuest)
        assertEquals("Invitado Sensus", vm.viewState.value.profile?.nombreCompleto)

        // Usuario cierra sesión e inicia con cuenta registrada mientras API está offline
        coEvery { profileRepo.getProfile() } returns Result.Failure(AppError.Network("Sin red"))
        sessionFlow.value = SessionState(
            isSessionActive = true,
            isGuest = false,
            activeName = "Rodrigo Pérez",
            activeEmail = "rodrigo@sensus.app",
            activeToken = "jwt-1"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.viewState.value.isGuest)
        assertEquals("Rodrigo Pérez", vm.viewState.value.profile?.nombreCompleto)
        assertEquals("rodrigo@sensus.app", vm.viewState.value.profile?.correo)

        // Cierre de sesión limpia el perfil en memoria
        sessionFlow.value = SessionState(isSessionActive = false, isGuest = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.profile)
    }

    @Test
    fun community_loadsData() = runTest {
        val storeRepo = mockk<StoreRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState(activeEmail = "test@sensus.com"))

        val vm = CommunityViewModel(storeRepo, sessionRepo)
        assertEquals("test@sensus.com", vm.viewState.value.currentUserName)
    }

    @Test
    fun routeMap_fetchesLocation_andChangesTravelMode() = runTest {
        val getUserLocation = mockk<GetUserLocationUseCase>(relaxed = true)
        coEvery { getUserLocation() } returns UserLocation(20.0, -99.0)

        val vm = RouteMapViewModel(getUserLocation)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(20.0, vm.viewState.value.userLocation.latitude, 0.001)

        vm.dispatchIntent(RouteMapViewIntent.TravelModeChanged("drive"))
        assertEquals("drive", vm.viewState.value.selectedTravelMode)

        vm.dispatchIntent(RouteMapViewIntent.FetchLocation)
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
