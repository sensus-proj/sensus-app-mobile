package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.AiTurn
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.OfferDiscoveryFilters
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SavedOffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetNearbyOffersUseCase
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.ToggleOfferLikeUseCase
import com.example.sensus.presentation.buyer.BuyerHomeViewIntent
import com.example.sensus.presentation.buyer.BuyerHomeViewModel
import com.example.sensus.presentation.buyer.BuyerMapViewIntent
import com.example.sensus.presentation.buyer.ClaimOfferViewState
import com.example.sensus.presentation.buyer.FriendsViewIntent
import com.example.sensus.presentation.buyer.FriendsViewModel
import com.example.sensus.presentation.buyer.NotificationsError
import com.example.sensus.presentation.buyer.NotificationsLocationIntent
import com.example.sensus.presentation.buyer.NotificationsViewIntent
import com.example.sensus.presentation.buyer.NotificationsViewModel
import com.example.sensus.presentation.buyer.ProductDetailViewEffect
import com.example.sensus.presentation.buyer.ProductDetailViewIntent
import com.example.sensus.presentation.buyer.ProductDetailViewModel
import com.example.sensus.presentation.buyer.SensiChatViewEffect
import com.example.sensus.presentation.buyer.SensiChatViewIntent
import com.example.sensus.presentation.buyer.SensiChatViewModel
import com.example.sensus.presentation.buyer.StoreReviewsViewEffect
import com.example.sensus.presentation.buyer.StoreReviewsViewIntent
import com.example.sensus.presentation.buyer.StoreReviewsViewModel
import com.example.sensus.presentation.buyer.toProductOffer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
class BuyerPresentationGapTest {

    private val testDispatcher = StandardTestDispatcher()

    private val baseOffer = ProductOffer(
        id = "offer-1",
        name = "Pan dulce",
        storeName = "Panadería Sol",
        address = "Centro 12",
        postalCode = "42800",
        category = "Panadería",
        originalPrice = 40.0,
        discountPrice = 20.0,
        discountPercent = 50,
        unitsLeft = 3,
        distanceMeters = 1200,
        merchantId = "m-1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun buyerContractsAndSensiZeroDiscountWork() {
        val state = ClaimOfferViewState(items = listOf(CartItem(baseOffer, 3), CartItem(baseOffer.copy(id = "o2"), 2)))
        assertEquals(5, state.totalUnits)
        assertNotNull(BuyerHomeViewIntent.ChangeFilters(OfferDiscoveryFilters()))
        assertNull(BuyerMapViewIntent.MerchantSelected(null).merchant)
        assertEquals("ok", StoreReviewsViewEffect.ShowMessage("ok").message)

        val zeroMapped = CitedOffer("o1", "Oferta", "", "Tienda", 10.0, 0.0, 2, true).toProductOffer()
        assertEquals(0, zeroMapped.discountPercent)
    }

    @Test
    fun notificationsErrorPathsWork() = runTest {
        val getSettings = mockk<NotificationsGetSettingsUseCase>(relaxed = true) {
            every { sessionChanges } returns emptyFlow()
            coEvery { this@mockk.invoke() } returns Result.Success(NotificationsPreferences(true, true, true, 5))
        }
        val registerDevice = mockk<NotificationsRegisterDeviceUseCase>(relaxed = true) {
            coEvery { this@mockk.invoke() } returnsMany listOf(
                Result.Failure(AppError.Forbidden(NotificationsRegisterDeviceUseCase.PERMISSION_REQUIRED)),
                Result.Failure(
                    AppError.NotFound(
                        com.example.sensus.domain.repository.NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE
                    )
                ),
                Result.Failure(AppError.Generic("boom"))
            )
        }
        val locationUc = mockk<NotificationsLocationUseCase>(relaxed = true) {
            every { isEnabled() } returns false
            coEvery { enable() } returnsMany listOf(
                Result.Failure(AppError.NotFound(NotificationsLocationUseCase.LOCATION_UNAVAILABLE)),
                Result.Failure(AppError.Validation("Inválido"))
            )
            coEvery { disable() } returns Result.Failure(AppError.Network())
        }

        val vm = NotificationsViewModel(getSettings, mockk<NotificationsUpdateSettingsUseCase>(relaxed = true), registerDevice, locationUc)
        testDispatcher.scheduler.advanceUntilIdle()
        listOf(NotificationsError.PERMISSION, NotificationsError.FIREBASE, NotificationsError.GENERIC).forEach { expected ->
            vm.dispatchIntent(NotificationsViewIntent.ActivateDevice)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(expected, vm.viewState.value.error)
        }
        vm.dispatchIntent(NotificationsLocationIntent.PermissionResult(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(NotificationsError.LOCATION_UNAVAILABLE, vm.viewState.value.error)
        vm.dispatchIntent(NotificationsLocationIntent.Toggled(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(NotificationsError.NETWORK, vm.viewState.value.error)
    }

    @Test
    fun productDetailAndStoreReviewsHandleAuthConflictAndEmptyStock() = runTest {
        val detailUc = mockk<GetOfferDetailUseCase> {
            coEvery { this@mockk.invoke("o-0") } returns Result.Success(
                OfferDetails(baseOffer.copy(id = "o-0", unitsLeft = 0), 2, true, 0L)
            )
        }
        val altUc = mockk<GetOfferAlternativesUseCase> {
            coEvery { this@mockk.invoke("o-0") } returns Result.Success(
                listOf(OfferAlternativeItem("o-2", "Pan", "PAN", "Pan", null, 30.0, 15.0, 2, 100, "", "m1", "T"))
            )
        }
        val likeUc = mockk<ToggleOfferLikeUseCase> { coEvery { this@mockk.invoke("o-0") } returns Result.Failure(AppError.Unauthorized()) }
        val reportUc = mockk<SubmitContentReportUseCase> {
            coEvery { this@mockk.invoke(any()) } returnsMany listOf(
                Result.Failure(AppError.Conflict("Ya reportado")),
                Result.Failure(AppError.Network("Sin red"))
            )
        }

        val pdVm = ProductDetailViewModel(detailUc, mockk<RegisterOfferViewUseCase>(relaxed = true), altUc, likeUc, reportUc)
        pdVm.viewEffect.test {
            pdVm.dispatchIntent(ProductDetailViewIntent.Load("o-0"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, pdVm.viewState.value.alternatives.size)
            pdVm.dispatchIntent(ProductDetailViewIntent.ToggleLike("o-0"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ProductDetailViewEffect.RequireAuth, awaitItem())
            pdVm.dispatchIntent(ProductDetailViewIntent.SubmitReport("o-0", ReportReason.SPAM, "spam"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ProductDetailViewEffect.ShowMessage("Ya reportado"), awaitItem())
            pdVm.dispatchIntent(ProductDetailViewIntent.SubmitReport("o-0", ReportReason.SPAM, "spam2"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("Sin red", pdVm.viewState.value.reportError)
            cancelAndIgnoreRemainingEvents()
        }

        val reviewsVm = StoreReviewsViewModel(
            mockk(relaxed = true),
            mockk(relaxed = true) { coEvery { this@mockk.invoke(any(), any(), any()) } returns Result.Failure(AppError.Unauthorized()) },
            mockk(relaxed = true),
            mockk(relaxed = true)
        )
        reviewsVm.viewEffect.test {
            reviewsVm.dispatchIntent(StoreReviewsViewIntent.ClearError)
            reviewsVm.dispatchIntent(StoreReviewsViewIntent.Load("m1", "T", "D", null))
            testDispatcher.scheduler.advanceUntilIdle()
            reviewsVm.dispatchIntent(StoreReviewsViewIntent.SubmitReview("m1"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(StoreReviewsViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sensiChatBuyerHomeSavedInterestsCommunityAndFriendsCoverGaps() = runTest {
        val sessionFlow = MutableStateFlow(SessionState(isSessionActive = false, isGuest = true))
        val sessionRepo = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns sessionFlow }
        val loadConv = mockk<LoadAiConversationUseCase>()
        val sendMsg = mockk<SendSensiMessageUseCase>()
        val locRepo = mockk<LocationRepository>(relaxed = true) { coEvery { getCurrentLocation() } throws IllegalStateException("no gps") }
        val chatVm = SensiChatViewModel(loadConv, sendMsg, mockk<AiChatRepository>(relaxed = true), sessionRepo, locRepo)
        chatVm.viewEffect.test {
            chatVm.dispatchIntent(SensiChatViewIntent.Init(ScreenContext.HOME))
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(chatVm.viewState.value.isGuest)
            sessionFlow.value = SessionState(isSessionActive = true, isGuest = false)
            coEvery { loadConv.createOrResume(false, ScreenContext.HOME) } returns Result.Success(AiConversation("c-1", "Active", "gpt", 0))
            coEvery { loadConv.getMessages("c-1") } returns Result.Success(emptyList())
            chatVm.dispatchIntent(SensiChatViewIntent.Init(ScreenContext.HOME))
            testDispatcher.scheduler.advanceUntilIdle()
            chatVm.dispatchIntent(SensiChatViewIntent.UpdateRetryCountdown(5))
            chatVm.dispatchIntent(SensiChatViewIntent.SendMessage("hola"))
            assertTrue(awaitItem() is SensiChatViewEffect.ShowToast)
            chatVm.dispatchIntent(SensiChatViewIntent.UpdateRetryCountdown(null))
            coEvery { sendMsg(any(), any(), any(), any()) } returns SensiResult.Success(AiTurn("c-1", "m1", "listo", correlationId = "c1"))
            chatVm.dispatchIntent(SensiChatViewIntent.SendMessage("ofertas"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(chatVm.viewState.value.isSending)
            cancelAndIgnoreRemainingEvents()
        }
        androidx.lifecycle.ViewModelStore().apply {
            put("chat", chatVm)
            clear()
        }
    }

    @Test
    fun buyerHomeSavedInterestsCommunityAndFriendsCoverGaps() = runTest {
        val sessionFlow = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
        val sessionRepo = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns sessionFlow }
        val offersRepo = mockk<OffersRepository>(relaxed = true) {
            coEvery { fetchNearbyOffersPage(any()) } returns Result.Success(PagedOffers(emptyList(), nextPage = null))
        }
        val okLoc = mockk<LocationRepository>(relaxed = true) {
            every { hasLocationPermission() } returns true
            every { isLocationEnabled() } returns true
            coEvery { getCurrentLocation() } returns com.example.sensus.domain.model.UserLocation(20.0, -99.3)
        }
        val bhVm = BuyerHomeViewModel(
            okLoc,
            sessionRepo,
            GetNearbyOffersUseCase(offersRepo, mockk<MapPreferencesRepository>(relaxed = true)),
            GetNearbyMerchantsUseCase(mockk(relaxed = true))
        )
        testDispatcher.scheduler.advanceUntilIdle()
        bhVm.dispatchIntent(BuyerHomeViewIntent.RadiusSelected(5f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5f, bhVm.viewState.value.selectedRadiusKm)

        val savedRepo = mockk<SavedOffersRepository>(relaxed = true) {
            every { savedOffers } returns MutableStateFlow(emptyList())
        }
        val savedVm = com.example.sensus.presentation.buyer.SavedInterestsViewModel(savedRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, savedVm.viewState.value.totalCount)

        val friendsVm = FriendsViewModel(mockk<FriendsRepository>(relaxed = true), sessionRepo)
        friendsVm.dispatchIntent(FriendsViewIntent.NotificationShown)
        friendsVm.dispatchIntent(FriendsViewIntent.AcceptedNotificationShown)
    }
}
