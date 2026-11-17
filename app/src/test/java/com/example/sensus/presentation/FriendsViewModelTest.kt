package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.data.remote.api.FriendsApiService
import com.example.sensus.data.remote.dto.FriendItemDto
import com.example.sensus.data.remote.dto.FriendSharedOffersThreadDto
import com.example.sensus.data.remote.dto.FriendsOverviewDto
import com.example.sensus.data.remote.dto.PendingFriendRequestDto
import com.example.sensus.data.remote.dto.SendFriendRequestBody
import com.example.sensus.data.remote.dto.SendFriendRequestResponseDto
import com.example.sensus.data.remote.dto.SentFriendRequestDto
import com.example.sensus.data.remote.dto.ShareOfferRequestBody
import com.example.sensus.data.remote.dto.ShareOfferResultDto
import com.example.sensus.data.remote.dto.SharedOfferItemDto
import com.example.sensus.data.repository.FriendsRepositoryImpl
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.domain.model.MAX_OFFER_SHARE_FRIENDS
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.presentation.buyer.FriendsViewEffect
import com.example.sensus.presentation.buyer.FriendsViewIntent
import com.example.sensus.presentation.buyer.FriendsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var testScope: CoroutineScope
    private val sessionFlow = MutableStateFlow(
        SessionState(
            isSessionActive = true,
            isGuest = false,
            hasAcceptedTerms = true,
            activeEmail = "buyer@sensus.com",
            activeName = "Alberto"
        )
    )
    private val sessionRepo = mockk<SessionRepository>(relaxed = true) {
        every { sessionState } returns sessionFlow
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        testScope = CoroutineScope(SupervisorJob() + dispatcher)
    }

    @After
    fun teardown() {
        testScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun qrSendPendingNotifyAcceptAndRejectEmitExpectedStatesAndEffects() = runTest(dispatcher) {
        val mockApi = mockk<FriendsApiService>()
        coEvery { mockApi.getFriendsOverview() } returns Response.success(
            FriendsOverviewDto(
                myInviteCode = "SNS-SELF01",
                friends = emptyList(),
                pendingReceived = listOf(
                    PendingFriendRequestDto("req-10", "SNS-112233", "Karla", "QR", "2026-11-16T12:00:00Z"),
                    PendingFriendRequestDto("req-11", "SNS-445566", "Luis", "Enlace", "2026-11-16T12:01:00Z")
                ),
                pendingSent = emptyList()
            )
        )
        coEvery { mockApi.sendFriendRequest(any<SendFriendRequestBody>()) } returns Response.success(
            SendFriendRequestResponseDto(
                outcome = "Sent",
                pendingRequest = SentFriendRequestDto("req-12", "SNS-9988AA", "Elena", "QR", "2026-11-16T12:02:00Z")
            )
        )
        coEvery { mockApi.acceptFriendRequest("req-10") } returns Response.success(
            FriendItemDto("u-karla", "SNS-112233", "Karla", "QR", "2026-11-16T12:03:00Z")
        )
        coEvery { mockApi.rejectFriendRequest("req-11") } returns Response.success(Unit)

        val repo = FriendsRepositoryImpl(
            userPreferenceDao = null,
            sessionRepository = sessionRepo,
            friendsApi = mockApi,
            ioDispatcher = dispatcher,
            scope = testScope
        )
        val vm = FriendsViewModel(repo, sessionRepo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.viewState.value.pendingRequests.size)
        assertEquals("req-10", vm.viewState.value.newlyArrivedRequest?.requestId)

        vm.dispatchIntent(FriendsViewIntent.NotificationShown)
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.newlyArrivedRequest)

        val friendUrl = FriendInviteCodec.buildInviteUrl("SNS-9988AA", "Elena")
        vm.viewEffect.test {
            vm.dispatchIntent(FriendsViewIntent.AddFromInput(friendUrl, "QR"))
            dispatcher.scheduler.advanceUntilIdle()
            val sentMsg = awaitItem() as FriendsViewEffect.ShowMessage
            assertTrue(sentMsg.message.contains("Elena"))

            vm.dispatchIntent(FriendsViewIntent.AcceptPendingRequest("req-10"))
            dispatcher.scheduler.advanceUntilIdle()
            val acceptMsg = awaitItem() as FriendsViewEffect.ShowMessage
            assertTrue(acceptMsg.message.contains("aceptada"))
            assertEquals(1, vm.viewState.value.friends.size)
            assertEquals(1, vm.viewState.value.pendingRequests.size)

            vm.dispatchIntent(FriendsViewIntent.RejectPendingRequest("req-11"))
            dispatcher.scheduler.advanceUntilIdle()
            val rejectMsg = awaitItem() as FriendsViewEffect.ShowMessage
            assertTrue(rejectMsg.message.contains("cancelada"))
            assertTrue(vm.viewState.value.pendingRequests.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestCannotAddFriendAndEmitsRequireAuth() = runTest(dispatcher) {
        sessionFlow.value = SessionState(isSessionActive = true, isGuest = true)
        val repo = FriendsRepositoryImpl(
            userPreferenceDao = null,
            sessionRepository = sessionRepo,
            friendsApi = null,
            ioDispatcher = dispatcher,
            scope = testScope
        )
        val vm = FriendsViewModel(repo, sessionRepo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.viewEffect.test {
            vm.dispatchIntent(FriendsViewIntent.AddFromInput("SNS-123456", "QR"))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(FriendsViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadSharedOffersAndShareOfferUpToTenEmitExpectedStateAndEffect() = runTest(dispatcher) {
        val mockApi = mockk<FriendsApiService>()
        val friendDto = FriendItemDto("u-karla", "SNS-112233", "Karla", "QR", "2026-11-16T12:00:00Z", "https://cdn.sensus.com/k.jpg")
        coEvery { mockApi.getFriendsOverview() } returns Response.success(
            FriendsOverviewDto(myInviteCode = "SNS-SELF01", friends = listOf(friendDto))
        )
        coEvery { mockApi.getFriendSharedOffers("u-karla") } returns Response.success(
            FriendSharedOffersThreadDto(
                friend = friendDto,
                offers = listOf(
                    SharedOfferItemDto(
                        id = "sh-10",
                        offerId = "offer-10",
                        senderUserId = "u-karla",
                        recipientUserId = "u-self",
                        offerTitle = "Café latte",
                        storeName = "Cafetería Sur",
                        originalPrice = 55.0,
                        discountedPrice = 30.0,
                        discountPercentage = 45,
                        sharedAtUtc = "2026-11-16T12:05:00Z"
                    )
                )
            )
        )
        coEvery { mockApi.shareOfferWithFriends("offer-10", any<ShareOfferRequestBody>()) } returns Response.success(
            ShareOfferResultDto(sharedCount = 1)
        )

        val repo = FriendsRepositoryImpl(
            userPreferenceDao = null,
            sessionRepository = sessionRepo,
            friendsApi = mockApi,
            ioDispatcher = dispatcher,
            scope = testScope
        )
        val vm = FriendsViewModel(repo, sessionRepo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(FriendsViewIntent.LoadSharedOffers("u-karla"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.viewState.value.sharedOffers.size)
        assertEquals("Café latte", vm.viewState.value.sharedOffers.first().offerTitle)

        vm.viewEffect.test {
            val excessiveRecipients = (1..(MAX_OFFER_SHARE_FRIENDS + 1)).map { "u-$it" }
            vm.dispatchIntent(FriendsViewIntent.ShareOffer("offer-10", excessiveRecipients))
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem() is FriendsViewEffect.ShowMessage)

            vm.dispatchIntent(FriendsViewIntent.ShareOffer("offer-10", listOf("u-karla")))
            dispatcher.scheduler.advanceUntilIdle()
            val sharedEffect = awaitItem() as FriendsViewEffect.OfferShared
            assertEquals(1, sharedEffect.count)
            val sharedMsg = awaitItem() as FriendsViewEffect.ShowMessage
            assertTrue(sharedMsg.message.contains("1 amigo"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
