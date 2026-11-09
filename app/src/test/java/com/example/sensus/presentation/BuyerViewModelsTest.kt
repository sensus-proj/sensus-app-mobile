package com.example.sensus.presentation

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CartItem
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
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.buyer.BuyerHomeViewIntent
import com.example.sensus.presentation.buyer.BuyerHomeViewModel
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        coEvery { offersRepo.getOffers() } returns listOf(
            ProductOffer("1", "Offer 1", "Store", "Abarrotes", 10.0, 5.0, 50, 5, 200)
        )
        val vm = BuyerHomeViewModel(offersRepo)
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
    fun buyerMap_selection_and_permission() = runTest {
        val offersRepo = mockk<OffersRepository>(relaxed = true)
        val locationRepo = mockk<LocationRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.sessionState } returns MutableStateFlow(SessionState())

        val offer = ProductOffer("1", "Offer 1", "Store", "Abarrotes", 10.0, 5.0, 50, 5, 200)
        coEvery { offersRepo.getOffers() } returns listOf(offer)

        val vm = BuyerMapViewModel(offersRepo, locationRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(BuyerMapViewIntent.OfferSelected(offer))
        assertEquals(offer, vm.viewState.value.selectedOffer)

        vm.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(true))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { sessionRepo.setLocationPermission(true) }
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

        val vm = BuyerProfileViewModel(profileRepo, updateUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Rodrigo", vm.viewState.value.profile?.nombreCompleto)

        vm.dispatchIntent(BuyerProfileViewIntent.UpdateName("Nuevo Nombre"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Nuevo Nombre", vm.viewState.value.profile?.nombreCompleto)

        vm.dispatchIntent(BuyerProfileViewIntent.LoadProfile)
        testDispatcher.scheduler.advanceUntilIdle()
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
