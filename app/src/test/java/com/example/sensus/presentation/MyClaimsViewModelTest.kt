package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.usecase.GetMyClaimsUseCase
import com.example.sensus.presentation.buyer.MyClaimsViewIntent
import com.example.sensus.presentation.buyer.MyClaimsViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MyClaimsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ClaimsRepository>()
    private val active = ClaimSummary("c1", "offer", "Pan", ClaimStatus.ACTIVE, Long.MAX_VALUE, "qr")
    private val redeemed = ClaimSummary("c2", "offer2", "Leche", ClaimStatus.REDEEMED, 10L, null)
    private val expired = ClaimSummary("c3", "offer3", "Cafe", ClaimStatus.EXPIRED, 5L, null)

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun viewModel() = MyClaimsViewModel(GetMyClaimsUseCase(repository))

    @Test
    fun loadEmitsLoadingThenSortedClaimsAndHandlesEmptyAndFailure() = runTest(dispatcher) {
        coEvery { repository.getMyClaims() } returns Result.Success(listOf(redeemed, expired, active))
        val vm = viewModel()
        vm.viewState.test {
            assertFalse(awaitItem().loading)
            vm.processIntent(MyClaimsViewIntent.Load)
            assertTrue(awaitItem().loading)
            advanceUntilIdle()
            val loadedState = awaitItem()
            assertFalse(loadedState.loading)
            assertTrue(loadedState.loaded)
            assertEquals(listOf("c1", "c2", "c3"), loadedState.claims.map { it.claimId })

            coEvery { repository.getMyClaims() } returns Result.Success(emptyList())
            vm.processIntent(MyClaimsViewIntent.Load)
            assertTrue(awaitItem().loading)
            advanceUntilIdle()
            val emptyState = awaitItem()
            assertTrue(emptyState.isEmpty)

            coEvery { repository.getMyClaims() } returns Result.Failure(AppError.NotFound())
            vm.processIntent(MyClaimsViewIntent.Load)
            assertTrue(awaitItem().loading)
            advanceUntilIdle()
            val errorState = awaitItem()
            assertTrue(errorState.error is AppError.NotFound)
            assertFalse(errorState.isEmpty)
        }
    }

    @Test
    fun summaryHelpersReflectStatusAndPayload() {
        assertTrue(active.isActiveAt(0L))
        assertFalse(active.copy(expiresAtEpochMs = 5L).isActiveAt(10L))
        assertEquals("qr", active.toOfferClaim()?.qrPayload)
        assertNull(redeemed.toOfferClaim())
        assertEquals(ClaimStatus.UNKNOWN, ClaimStatus.from("Pending"))
    }
}
