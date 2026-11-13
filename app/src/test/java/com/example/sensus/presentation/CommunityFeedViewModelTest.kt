package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import com.example.sensus.presentation.buyer.CommunityViewEffect
import com.example.sensus.presentation.buyer.CommunityViewIntent
import com.example.sensus.presentation.buyer.CommunityViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityFeedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getFeed = mockk<GetNearbyFeedUseCase>()
    private val createPost = mockk<CreateFeedPostUseCase>()
    private val locations = mockk<LocationRepository>()
    private val offers = mockk<OffersRepository>(relaxed = true)
    private val session = mockk<SessionRepository>()
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, activeName = "Vecina"))
    private val firstPost = FeedPost("1", "Vecina", "Pan", "2026-11-12")

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { session.sessionState } returns sessionState
        every { locations.hasLocationPermission() } returns true
        coEvery { locations.getCurrentLocation() } returns UserLocation(20.0, -99.0)
        coEvery { getFeed(any()) } returns Result.Success(PagedFeed(listOf(firstPost), 1, true))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CommunityViewModel(getFeed, createPost, locations, session, offers)

    @Test
    fun loadAndPagination_deduplicatePostsAndStopAtLastPage() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        coEvery { getFeed(match { it.page == 2 }) } returns Result.Success(PagedFeed(listOf(firstPost, firstPost.copy(id = "2")), 2, false))
        vm.dispatchIntent(CommunityViewIntent.LoadMore)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.viewState.value.posts.map { it.id })
        assertFalse(vm.viewState.value.hasMore)
        vm.dispatchIntent(CommunityViewIntent.LoadMore)
        coVerify(exactly = 2) { getFeed(any()) }
    }

    @Test
    fun refresh_replacesPreviousPageAndResetsPagination() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        coEvery { getFeed(any()) } returns Result.Success(PagedFeed(listOf(firstPost.copy(id = "new")), 1, false))
        vm.dispatchIntent(CommunityViewIntent.Refresh)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("new", vm.viewState.value.posts.single().id)
        assertEquals(2, vm.viewState.value.nextPage)
        assertFalse(vm.viewState.value.isLoading)
    }

    @Test
    fun failedNextPage_preservesPostsAndAllowsRetryOfSamePage() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        coEvery { getFeed(match { it.page == 2 }) } returns Result.Failure(AppError.Network("Sin conexión"))
        vm.dispatchIntent(CommunityViewIntent.LoadMore)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.viewState.value.posts.size)
        assertEquals(2, vm.viewState.value.nextPage)
        assertEquals("Sin conexión", vm.viewState.value.errorMessage)
    }

    @Test
    fun missingLocationPermission_doesNotQueryWithInventedCoordinates() = runTest(dispatcher) {
        every { locations.hasLocationPermission() } returns false
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.viewState.value.isLoading)
        assertTrue(vm.viewState.value.errorMessage!!.contains("ubicación"))
        coVerify(exactly = 0) { getFeed(any()) }
    }

    @Test
    fun guestNewPost_emitsAuthenticationEffectAndDoesNotOpenComposer() = runTest(dispatcher) {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.viewEffect.test {
            vm.dispatchIntent(CommunityViewIntent.NewPost)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(CommunityViewEffect.RequireAuth, awaitItem())
        }
        assertFalse(vm.viewState.value.showComposer)
    }

    @Test
    fun successfulPost_insertsOnlyAfterServerConfirmationAndClearsDraft() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        coEvery { createPost(any(), any()) } returns Result.Success(firstPost.copy(id = "created"))
        vm.dispatchIntent(CommunityViewIntent.TextChanged("Oferta vecinal"))
        vm.dispatchIntent(CommunityViewIntent.MerchantSelected("shop-1"))
        vm.dispatchIntent(CommunityViewIntent.Submit)
        vm.dispatchIntent(CommunityViewIntent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("created", vm.viewState.value.posts.first().id)
        assertEquals("", vm.viewState.value.draftText)
        assertFalse(vm.viewState.value.isPublishing)
        coVerify(exactly = 1) { createPost(match { it.text == "Oferta vecinal" && it.merchantId == "shop-1" }, null) }
    }

    @Test
    fun moderationFailure_preservesDraftAndDoesNotInsertPost() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        coEvery { createPost(any(), any()) } returns Result.Failure(AppError.ContentRejected("Contenido bloqueado"))
        vm.dispatchIntent(CommunityViewIntent.TextChanged("Mi texto"))
        vm.dispatchIntent(CommunityViewIntent.ImageSelected(byteArrayOf(1)))
        vm.dispatchIntent(CommunityViewIntent.Submit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Mi texto", vm.viewState.value.draftText)
        assertEquals(1, vm.viewState.value.posts.size)
        assertEquals("Contenido bloqueado", vm.viewState.value.errorMessage)
        assertTrue(vm.viewState.value.hasImage)
    }

    @Test
    fun cachedPage_isIdentifiedAsOfflineContent() = runTest(dispatcher) {
        coEvery { getFeed(any()) } returns Result.Success(PagedFeed(listOf(firstPost), 1, false, isCached = true))
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.isCached)
    }
}
