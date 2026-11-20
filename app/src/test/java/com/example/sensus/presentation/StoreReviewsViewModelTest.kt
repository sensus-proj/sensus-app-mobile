package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.RatingBreakdownItem
import com.example.sensus.domain.model.ReviewLikeStatus
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.model.StoreReviewReply
import com.example.sensus.domain.model.StoreReviewsSummary
import com.example.sensus.domain.repository.StoreReviewRepository
import com.example.sensus.domain.usecase.CreateStoreReviewUseCase
import com.example.sensus.domain.usecase.GetStoreReviewsUseCase
import com.example.sensus.domain.usecase.ReplyStoreReviewUseCase
import com.example.sensus.domain.usecase.ToggleStoreReviewLikeUseCase
import com.example.sensus.presentation.buyer.StoreReviewsViewIntent
import com.example.sensus.presentation.buyer.StoreReviewsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreReviewsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<StoreReviewRepository>()
    private val initialReview = StoreReview(
        id = "rev-1",
        merchantId = "m-1",
        authorUserId = "u-1",
        authorName = "Ana López",
        authorPhotoUrl = null,
        rating = 5,
        comment = "Excelente tienda",
        createdAtUtc = "2026-11-19T12:00:00Z",
        likesCount = 1,
        isLikedByMe = false,
        replies = emptyList()
    )
    private val summary = StoreReviewsSummary(
        merchantId = "m-1",
        merchantName = "Café Central",
        averageRating = 4.4,
        totalReviews = 5,
        breakdown = listOf(RatingBreakdownItem(5, 3), RatingBreakdownItem(4, 2)),
        reviews = listOf(initialReview)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getReviews("m-1") } returns Result.Success(summary)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildVm() = StoreReviewsViewModel(
        getReviews = GetStoreReviewsUseCase(repository),
        createReview = CreateStoreReviewUseCase(repository),
        toggleReviewLike = ToggleStoreReviewLikeUseCase(repository),
        replyReview = ReplyStoreReviewUseCase(repository)
    )

    @Test
    fun submitReview_blocksProfanityLocally_andPublishesValidReview() = runTest(dispatcher) {
        val vm = buildVm()
        vm.dispatchIntent(StoreReviewsViewIntent.Load("m-1", "Café Central", "Av. Reforma", null))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(4.4, vm.viewState.value.summary?.averageRating ?: 0.0, 0.01)

        vm.dispatchIntent(StoreReviewsViewIntent.SelectRating(4))
        vm.dispatchIntent(StoreReviewsViewIntent.CommentChanged("Es una puta estafa"))
        vm.dispatchIntent(StoreReviewsViewIntent.SubmitReview("m-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.viewState.value.errorMessage)
        coVerify(exactly = 0) { repository.createReview(any(), any(), any()) }

        coEvery { repository.createReview("m-1", 4, "Muy rico todo") } returns Result.Success(
            summary.copy(averageRating = 4.3, totalReviews = 6)
        )
        vm.dispatchIntent(StoreReviewsViewIntent.CommentChanged("Muy rico todo"))
        vm.dispatchIntent(StoreReviewsViewIntent.SubmitReview("m-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.errorMessage)
        assertEquals(6, vm.viewState.value.summary?.totalReviews)
    }

    @Test
    fun toggleLikeAndReply_updateReviewCardInState() = runTest(dispatcher) {
        val vm = buildVm()
        vm.dispatchIntent(StoreReviewsViewIntent.Load("m-1", "Café Central", "Av. Reforma", null))
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.toggleReviewLike("rev-1") } returns Result.Success(ReviewLikeStatus("rev-1", 2, true))
        vm.dispatchIntent(StoreReviewsViewIntent.ToggleLikeReview("rev-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, vm.viewState.value.summary?.reviews?.single()?.likesCount)
        assertTrue(vm.viewState.value.summary?.reviews?.single()?.isLikedByMe == true)

        val updatedWithReply = initialReview.copy(
            likesCount = 2,
            isLikedByMe = true,
            replies = listOf(StoreReviewReply("rep-1", "u-2", "Beto", null, "Gracias por la recomendación", "2026-11-19T14:00:00Z"))
        )
        coEvery { repository.replyToReview("rev-1", "Gracias por la recomendación") } returns Result.Success(updatedWithReply)
        vm.dispatchIntent(StoreReviewsViewIntent.OpenReply("rev-1"))
        vm.dispatchIntent(StoreReviewsViewIntent.ReplyDraftChanged("Gracias por la recomendación"))
        vm.dispatchIntent(StoreReviewsViewIntent.SubmitReply("rev-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.viewState.value.replyingToReviewId)
        assertEquals(1, vm.viewState.value.summary?.reviews?.single()?.replies?.size)
    }

    @Test
    fun loadFailure_surfacesFriendlyError() = runTest(dispatcher) {
        coEvery { repository.getReviews("m-1") } returns Result.Failure(AppError.Network("Sin conexión"))
        val vm = buildVm()
        vm.dispatchIntent(StoreReviewsViewIntent.Load("m-1", "Café", "Calle 1", null))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Sin conexión", vm.viewState.value.errorMessage)
    }
}
