package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.domain.model.FriendNoticeType
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.domain.model.ShareOfferOutcome
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.AddFeedCommentUseCase
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.DeleteFeedPostUseCase
import com.example.sensus.domain.usecase.GetFeedCommentsUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.TogglePostLikeUseCase
import com.example.sensus.presentation.buyer.CommunityViewEffect
import com.example.sensus.presentation.buyer.CommunityViewIntent
import com.example.sensus.presentation.buyer.CommunityViewModel
import com.example.sensus.presentation.buyer.FriendsViewEffect
import com.example.sensus.presentation.buyer.FriendsViewIntent
import com.example.sensus.presentation.buyer.FriendsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class FriendsAndCommunityGapCoverageTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun friendsRepoMock(pending: List<PendingFriendRequest> = emptyList()): FriendsRepository = mockk<FriendsRepository>(relaxed = true) {
        every { friends } returns MutableStateFlow(emptyList())
        every { pendingRequests } returns MutableStateFlow(pending)
        every { sentRequests } returns MutableStateFlow(emptyList())
        every { newlyArrivedRequest } returns MutableStateFlow(null)
        every { newlyAcceptedFriend } returns MutableStateFlow(null)
        every { myInviteCode } returns MutableStateFlow("SNS-MYCODE")
        every { myInviteUrl } returns MutableStateFlow("https://rodnix.com.mx/sensus?code=SNS-MYCODE")
        every { pendingInviteTrigger } returns MutableStateFlow(0)
    }

    @Test
    fun friendsViewModel_removeFriendAcceptedNoticeReloadThreadAndDeepLinks() = runTest(dispatcher) {
        val pending = listOf(PendingFriendRequest("r1", "SNS-AAA111", "Ana", "QR", 1000L))
        val friendsRepo = friendsRepoMock(pending)
        val sessionFlow = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
        val sessionRepo = mockk<SessionRepository>(relaxed = true) { every { sessionState } returns sessionFlow }
        coEvery { friendsRepo.getSharedOffersWithFriend("u-ana") } returns emptyList()
        coEvery { friendsRepo.shareOfferWithFriends("o1", listOf("u-ana")) } returns ShareOfferOutcome.NetworkFailure("Sin red")
        coEvery { friendsRepo.consumePendingInviteIfAuthenticated() } returns null andThen AddFriendResult.SelfInviteNotAllowed

        val vm = FriendsViewModel(friendsRepo, sessionRepo)
        advanceUntilIdle()

        vm.viewEffect.test {
            vm.dispatchIntent(FriendsViewIntent.RemoveFriend("SNS-AAA111"))
            advanceUntilIdle()
            assertEquals("Amigo eliminado de tu red.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            coVerify { friendsRepo.removeFriend("SNS-AAA111") }

            vm.dispatchIntent(FriendsViewIntent.AcceptedNotificationShown)
            verify { friendsRepo.markNoticeHandled(FriendNoticeType.ACCEPTED_FRIEND) }

            vm.dispatchIntent(FriendsViewIntent.LoadSharedOffers("u-ana"))
            advanceUntilIdle()
            vm.dispatchIntent(FriendsViewIntent.LoadSharedOffers("u-ana"))
            advanceUntilIdle()
            assertFalse(vm.viewState.value.isLoadingThread)

            vm.dispatchIntent(FriendsViewIntent.ShareOffer("o1", listOf("u-ana")))
            advanceUntilIdle()
            assertEquals("Sin red", (awaitItem() as FriendsViewEffect.ShowMessage).message)

            vm.dispatchIntent(FriendsViewIntent.ConsumePendingDeepLink)
            advanceUntilIdle()
            assertEquals(FriendsViewEffect.OpenFriendsTab, awaitItem())

            vm.dispatchIntent(FriendsViewIntent.ConsumePendingDeepLink)
            advanceUntilIdle()
            assertEquals("No puedes agregarte a ti mismo como amigo.", (awaitItem() as FriendsViewEffect.ShowMessage).message)

            sessionFlow.value = SessionState(isSessionActive = true, isGuest = true)
            advanceUntilIdle()
            vm.dispatchIntent(FriendsViewIntent.ShareOffer("o1", listOf("u-ana")))
            assertEquals(FriendsViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun friendsViewModel_addFromInputEmitsAllOutcomesAndCodecRejectsBadUri() = runTest(dispatcher) {
        val friendsRepo = friendsRepoMock()
        val sessionRepo = mockk<SessionRepository>(relaxed = true) {
            every { sessionState } returns MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
        }
        val friend = ConnectedFriend("SNS-223344", "Karla", 1000L, "QR", "u-karla")
        val sent = SentFriendRequest("s1", "SNS-334455", "Elena", "QR", 1000L)
        coEvery { friendsRepo.addFriendFromRaw("1", any()) } returns AddFriendResult.Added(friend)
        coEvery { friendsRepo.addFriendFromRaw("2", any()) } returns AddFriendResult.RequestSent(sent)
        coEvery { friendsRepo.addFriendFromRaw("3", any()) } returns AddFriendResult.AlreadyPending("Luis")
        coEvery { friendsRepo.addFriendFromRaw("4", any()) } returns AddFriendResult.AlreadyConnected(friend)
        coEvery { friendsRepo.addFriendFromRaw("5", any()) } returns AddFriendResult.InvalidCode
        coEvery { friendsRepo.addFriendFromRaw("6", any()) } returns AddFriendResult.NetworkFailure("Timeout")
        coEvery { friendsRepo.addFriendFromRaw("7", any()) } returns AddFriendResult.GuestNotAllowed

        val vm = FriendsViewModel(friendsRepo, sessionRepo)
        advanceUntilIdle()
        vm.viewEffect.test {
            listOf("1", "2", "3", "4", "5", "6", "7").forEach { code ->
                vm.dispatchIntent(FriendsViewIntent.AddFromInput(code))
            }
            advanceUntilIdle()
            assertEquals("Karla ahora está en tus amigos.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals("Solicitud enviada a Elena.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals("Ya enviaste una solicitud pendiente a Luis.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals("Karla ya es tu amigo.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals("Código o QR de invitación inválido.", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals("Timeout", (awaitItem() as FriendsViewEffect.ShowMessage).message)
            assertEquals(FriendsViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(FriendInviteCodec.parseInvite("sensus://invitacion?code=bad"))
    }

    private data class CommunityErrorDeps(
        val commentsUc: GetFeedCommentsUseCase? = mockk(),
        val toggleUc: TogglePostLikeUseCase? = mockk(),
        val addCommentUc: AddFeedCommentUseCase? = mockk(),
        val deleteUc: DeleteFeedPostUseCase? = mockk(),
        val reportUc: SubmitContentReportUseCase? = mockk()
    )

    private fun buildCommunityVm(isGuest: Boolean, hasPermission: Boolean, deps: CommunityErrorDeps = CommunityErrorDeps()): CommunityViewModel {
        val samplePost = FeedPost("p1", "Sofía", "2x1", "Hoy", likesCount = 1, isLikedByMe = false, commentsCount = 1, isMine = true)
        val getFeed = mockk<GetNearbyFeedUseCase>()
        val locations = mockk<LocationRepository>()
        val session = mockk<SessionRepository> {
            every { sessionState } returns MutableStateFlow(SessionState(isSessionActive = !isGuest, isGuest = isGuest))
        }
        every { locations.hasLocationPermission() } returns hasPermission
        coEvery { locations.getCurrentLocation() } returns UserLocation(20.0, -99.0)
        coEvery { getFeed(any()) } returns Result.Success(PagedFeed(listOf(samplePost), 1, false))
        return CommunityViewModel(
            getFeed,
            mockk<CreateFeedPostUseCase>(),
            locations,
            session,
            mockk<OffersRepository>(relaxed = true),
            deps.toggleUc,
            deps.commentsUc,
            deps.addCommentUc,
            deps.deleteUc,
            deps.reportUc
        ).also { it.ioDispatcher = dispatcher }
    }

    @Test
    fun communityViewModel_guestActionsEmitRequireAuthAndMissingLocationBlocksSubmit() = runTest(dispatcher) {
        val samplePost = FeedPost("p1", "Sofía", "2x1", "Hoy", isMine = true)
        val guestVm = buildCommunityVm(isGuest = true, hasPermission = true)
        advanceUntilIdle()
        guestVm.viewEffect.test {
            guestVm.dispatchIntent(CommunityViewIntent.Submit)
            guestVm.dispatchIntent(CommunityViewIntent.ToggleLike("p1"))
            guestVm.dispatchIntent(CommunityViewIntent.SubmitComment)
            guestVm.dispatchIntent(CommunityViewIntent.OpenReportPost(samplePost))
            repeat(4) { assertEquals(CommunityViewEffect.RequireAuth, awaitItem()) }
            cancelAndIgnoreRemainingEvents()
        }

        val noLocVm = buildCommunityVm(isGuest = false, hasPermission = false, deps = CommunityErrorDeps(commentsUc = null))
        advanceUntilIdle()
        noLocVm.dispatchIntent(CommunityViewIntent.ImageReadFailed)
        assertTrue(noLocVm.viewState.value.publishError is AppError.Validation)
        noLocVm.dispatchIntent(CommunityViewIntent.Submit)
        assertTrue(noLocVm.viewState.value.publishError is AppError.Validation)
        noLocVm.dispatchIntent(CommunityViewIntent.OpenComments(samplePost))
        assertFalse(noLocVm.viewState.value.isLoadingComments)
        noLocVm.dispatchIntent(CommunityViewIntent.CloseComments)
        assertNull(noLocVm.viewState.value.activeCommentPost)
    }

    @Test
    fun communityViewModel_errorPathsEmitRequireAuthAndErrorMessages() = runTest(dispatcher) {
        val samplePost = FeedPost("p1", "Sofía", "2x1", "Hoy", isMine = true)
        val commentsUc = mockk<GetFeedCommentsUseCase>()
        val toggleUc = mockk<TogglePostLikeUseCase>()
        val addCommentUc = mockk<AddFeedCommentUseCase>()
        val deleteUc = mockk<DeleteFeedPostUseCase>()
        val reportUc = mockk<SubmitContentReportUseCase>()
        coEvery { commentsUc("p1") } returns Result.Failure(AppError.Network("Err comentarios"))
        coEvery { toggleUc("p1", any()) } returns Result.Failure(AppError.Unauthorized("Auth like"))
        coEvery { addCommentUc("p1", any()) } returns Result.Failure(AppError.Unauthorized("Auth comment"))
        coEvery { deleteUc("p1") } returns Result.Failure(AppError.Network("Err delete")) andThen Result.Failure(AppError.Unauthorized("Auth delete"))
        coEvery { reportUc(any()) } returns
            Result.Failure(AppError.Network("Err report")) andThen
            Result.Failure(AppError.Conflict("Ya reportado")) andThen
            Result.Failure(AppError.Unauthorized("Auth report"))

        val vm = buildCommunityVm(
            isGuest = false,
            hasPermission = true,
            deps = CommunityErrorDeps(commentsUc, toggleUc, addCommentUc, deleteUc, reportUc)
        )
        advanceUntilIdle()
        vm.viewEffect.test {
            vm.dispatchIntent(CommunityViewIntent.OpenComments(samplePost))
            advanceUntilIdle()
            assertEquals("Err comentarios", vm.viewState.value.commentError)

            vm.dispatchIntent(CommunityViewIntent.ToggleLike("p1"))
            advanceUntilIdle()
            assertEquals(CommunityViewEffect.RequireAuth, awaitItem())

            vm.dispatchIntent(CommunityViewIntent.CommentDraftChanged("Hola"))
            vm.dispatchIntent(CommunityViewIntent.SubmitComment)
            advanceUntilIdle()
            assertEquals(CommunityViewEffect.RequireAuth, awaitItem())

            vm.dispatchIntent(CommunityViewIntent.RequestDeletePost(samplePost))
            vm.dispatchIntent(CommunityViewIntent.ConfirmDeletePost)
            advanceUntilIdle()
            assertEquals("Err delete", (awaitItem() as CommunityViewEffect.ShowMessage).message)

            vm.dispatchIntent(CommunityViewIntent.RequestDeletePost(samplePost))
            vm.dispatchIntent(CommunityViewIntent.ConfirmDeletePost)
            advanceUntilIdle()
            assertEquals(CommunityViewEffect.RequireAuth, awaitItem())

            vm.dispatchIntent(CommunityViewIntent.OpenReportPost(samplePost))
            vm.dispatchIntent(CommunityViewIntent.SubmitReport(ReportReason.SPAM, ""))
            advanceUntilIdle()
            assertEquals("Err report", vm.viewState.value.reportError)

            vm.dispatchIntent(CommunityViewIntent.SubmitReport(ReportReason.SPAM, ""))
            advanceUntilIdle()
            assertEquals("Ya reportado", (awaitItem() as CommunityViewEffect.ShowMessage).message)

            vm.dispatchIntent(CommunityViewIntent.OpenReportPost(samplePost))
            vm.dispatchIntent(CommunityViewIntent.SubmitReport(ReportReason.SPAM, ""))
            advanceUntilIdle()
            assertEquals(CommunityViewEffect.RequireAuth, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
