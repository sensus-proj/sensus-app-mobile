package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CommunityActivityItem
import com.example.sensus.domain.model.CommunityActivityType
import com.example.sensus.domain.repository.FriendsActivityRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetFriendsCommunityActivityUseCase
import com.example.sensus.presentation.buyer.FriendsActivityViewIntent
import com.example.sensus.presentation.buyer.FriendsActivityViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<FriendsActivityRepository>()
    private val sessionRepository = mockk<SessionRepository>()
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeName = "Beto"))

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { sessionRepository.sessionState } returns sessionState
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authenticatedSession_loadsFriendsCommentsAndLikes() = runTest(dispatcher) {
        val sampleItems = listOf(
            CommunityActivityItem(
                id = "offer-like:1",
                type = CommunityActivityType.OFFER_LIKE,
                actorUserId = "u-friend",
                actorName = "Carlos",
                actorPhotoUrl = null,
                offerId = "off-1",
                merchantId = "m-1",
                targetTitle = "Capuchino 2x1",
                targetSubtitle = "Café Central",
                commentText = null,
                rating = null,
                createdAtUtc = "2026-11-19T12:00:00Z"
            )
        )
        coEvery { repository.getFriendsActivity(any()) } returns Result.Success(sampleItems)

        val vm = FriendsActivityViewModel(GetFriendsCommunityActivityUseCase(repository), sessionRepository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.viewState.value.isGuest)
        assertEquals(1, vm.viewState.value.items.size)
        assertEquals("Carlos", vm.viewState.value.items.single().actorName)

        coEvery { repository.getFriendsActivity(any()) } returns Result.Failure(AppError.Network("Error de red"))
        vm.dispatchIntent(FriendsActivityViewIntent.Refresh)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Error de red", vm.viewState.value.errorMessage)
    }

    @Test
    fun guestSession_keepsItemsEmptyAndGuestTrue() = runTest(dispatcher) {
        sessionState.value = SessionState(isSessionActive = false, isGuest = true)
        val vm = FriendsActivityViewModel(GetFriendsCommunityActivityUseCase(repository), sessionRepository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.isGuest)
        assertTrue(vm.viewState.value.items.isEmpty())
    }
}
