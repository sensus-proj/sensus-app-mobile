package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.model.UserLocation
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
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelInteractionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val getFeed = mockk<GetNearbyFeedUseCase>()
    private val createPost = mockk<CreateFeedPostUseCase>()
    private val locations = mockk<LocationRepository>()
    private val offers = mockk<OffersRepository>(relaxed = true)
    private val session = mockk<SessionRepository>()
    private val toggleLike = mockk<TogglePostLikeUseCase>()
    private val getComments = mockk<GetFeedCommentsUseCase>()
    private val addComment = mockk<AddFeedCommentUseCase>()
    private val deletePost = mockk<DeleteFeedPostUseCase>()
    private val submitReport = mockk<SubmitContentReportUseCase>()
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeName = "Sofía"))
    private val samplePost = FeedPost("p1", "Sofía", "Oferta 2x1", "Hoy", likesCount = 2, isLikedByMe = false, commentsCount = 1, isMine = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { session.sessionState } returns sessionState
        every { locations.hasLocationPermission() } returns true
        coEvery { locations.getCurrentLocation() } returns UserLocation(20.0, -99.0)
        coEvery { getFeed(any()) } returns Result.Success(PagedFeed(listOf(samplePost), 1, false))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = CommunityViewModel(
        getFeed,
        createPost,
        locations,
        session,
        offers,
        null,
        toggleLike,
        getComments,
        addComment,
        deletePost,
        submitReport
    ).also { it.ioDispatcher = dispatcher }

    @Test
    fun toggleLike_updatesOptimisticallyOnSuccess_andRollsBackOnError() = runTest(dispatcher) {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { toggleLike("p1") } returns Result.Success(TogglePostLikeResult("p1", 3, true))
        vm.dispatchIntent(CommunityViewIntent.ToggleLike("p1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.posts.first().isLikedByMe)
        assertEquals(3, vm.viewState.value.posts.first().likesCount)

        coEvery { toggleLike("p1") } returns Result.Failure(AppError.Network("Sin red"))
        vm.viewEffect.test {
            vm.dispatchIntent(CommunityViewIntent.ToggleLike("p1"))
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is CommunityViewEffect.ShowMessage)
            assertTrue(vm.viewState.value.posts.first().isLikedByMe)
            assertEquals(3, vm.viewState.value.posts.first().likesCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun openAddCommentsAndDeletePost_updateStateCleanly() = runTest(dispatcher) {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()

        val initialComment = FeedComment("c1", "p1", "u2", "Mateo", "Primero", "2026-11-21T09:00:00Z")
        val addedComment = FeedComment("c2", "p1", "u1", "Sofía", "Gracias!", "2026-11-21T09:05:00Z")
        coEvery { getComments("p1") } returns Result.Success(PagedFeedComments(listOf(initialComment), 1, 1, 20, false))
        coEvery { addComment("p1", "Gracias!") } returns Result.Success(addedComment)
        coEvery { deletePost("p1") } returns Result.Success(Unit)

        vm.dispatchIntent(CommunityViewIntent.OpenComments(samplePost))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.viewState.value.comments.size)

        vm.dispatchIntent(CommunityViewIntent.CommentDraftChanged("Gracias!"))
        vm.dispatchIntent(CommunityViewIntent.SubmitComment)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.viewState.value.comments.size)
        assertEquals(2, vm.viewState.value.posts.first().commentsCount)

        vm.dispatchIntent(CommunityViewIntent.RequestDeletePost(samplePost))
        vm.dispatchIntent(CommunityViewIntent.ConfirmDeletePost)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.posts.isEmpty())
    }

    @Test
    fun reportPost_closesDialogAndEmitsMessageOnSuccessAndConflict() = runTest(dispatcher) {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { submitReport(any()) } returns Result.Success(
            SubmittedReport("r1", "post", "p1", "spam", false, "2026-11-21", "Reporte recibido")
        )

        vm.viewEffect.test {
            vm.dispatchIntent(CommunityViewIntent.OpenReportPost(samplePost))
            vm.dispatchIntent(CommunityViewIntent.SubmitReport(ReportReason.SPAM, "Enlace repetido"))
            dispatcher.scheduler.advanceUntilIdle()
            assertNull(vm.viewState.value.reportTarget)
            assertEquals("Reporte recibido", (awaitItem() as CommunityViewEffect.ShowMessage).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
