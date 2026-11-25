package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.usecase.CancelClaimUseCase
import com.example.sensus.domain.usecase.GetMyClaimsUseCase
import com.example.sensus.presentation.buyer.MyClaimsViewEffect
import com.example.sensus.presentation.buyer.MyClaimsViewIntent
import com.example.sensus.presentation.buyer.MyClaimsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun viewModel() = MyClaimsViewModel(GetMyClaimsUseCase(repository), CancelClaimUseCase(repository))

    @Test
    fun loadSortsActiveFirstAndHandlesEmptyAndFailure() = runTest(dispatcher) {
        coEvery { repository.getMyClaims() } returns Result.Success(listOf(redeemed, active))
        val vm = viewModel()
        vm.processIntent(MyClaimsViewIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf("c1", "c2"), vm.viewState.value.claims.map { it.claimId })

        coEvery { repository.getMyClaims() } returns Result.Success(emptyList())
        vm.processIntent(MyClaimsViewIntent.Load)
        advanceUntilIdle()
        assertTrue(vm.viewState.value.isEmpty)

        coEvery { repository.getMyClaims() } returns Result.Failure(AppError.NotFound())
        vm.processIntent(MyClaimsViewIntent.Load)
        advanceUntilIdle()
        assertTrue(vm.viewState.value.error is AppError.NotFound)
        assertTrue(!vm.viewState.value.isEmpty)
    }

    @Test
    fun cancelRequiresConfirmationAndUpdatesStatus() = runTest(dispatcher) {
        coEvery { repository.getMyClaims() } returns Result.Success(listOf(active))
        coEvery { repository.cancelClaim("c1") } returns Result.Success(active.copy(status = ClaimStatus.CANCELLED, offerTitle = null))
        val vm = viewModel()
        vm.processIntent(MyClaimsViewIntent.Load)
        advanceUntilIdle()
        vm.processIntent(MyClaimsViewIntent.RequestCancel("unknown"))
        assertNull(vm.viewState.value.pendingCancelClaimId)
        vm.processIntent(MyClaimsViewIntent.RequestCancel("c1"))
        vm.processIntent(MyClaimsViewIntent.DismissCancel)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.cancelClaim(any()) }

        vm.viewEffect.test {
            vm.processIntent(MyClaimsViewIntent.RequestCancel("c1"))
            vm.processIntent(MyClaimsViewIntent.ConfirmCancel)
            assertEquals("c1", vm.viewState.value.cancellingClaimId)
            advanceUntilIdle()
            assertEquals(MyClaimsViewEffect.Cancelled("c1"), awaitItem())
        }
        val updated = vm.viewState.value.claims.single()
        assertEquals(ClaimStatus.CANCELLED, updated.status)
        assertEquals("Pan", updated.offerTitle)
        assertNull(vm.viewState.value.cancellingClaimId)
    }

    @Test
    fun cancelFailureKeepsClaimAndReportsError() = runTest(dispatcher) {
        coEvery { repository.getMyClaims() } returns Result.Success(listOf(active))
        coEvery { repository.cancelClaim("c1") } returns Result.Failure(AppError.Network())
        val vm = viewModel()
        vm.processIntent(MyClaimsViewIntent.Load)
        advanceUntilIdle()
        vm.processIntent(MyClaimsViewIntent.RequestCancel("c1"))
        vm.processIntent(MyClaimsViewIntent.ConfirmCancel)
        advanceUntilIdle()
        assertEquals(ClaimStatus.ACTIVE, vm.viewState.value.claims.single().status)
        assertTrue(vm.viewState.value.cancelError is AppError.Network)
    }

    @Test
    fun cancelUseCaseRejectsBlankId() = runTest(dispatcher) {
        val result = CancelClaimUseCase(repository)("  ") as Result.Failure
        assertTrue(result.error is AppError.Validation)
        coVerify(exactly = 0) { repository.cancelClaim(any()) }
    }

    @Test
    fun summaryHelpersReflectStatusAndPayload() {
        assertTrue(active.isActiveAt(0L))
        assertTrue(!active.copy(expiresAtEpochMs = 5L).isActiveAt(10L))
        assertEquals("qr", active.toOfferClaim()?.qrPayload)
        assertNull(redeemed.toOfferClaim())
        assertEquals(ClaimStatus.UNKNOWN, ClaimStatus.from("Pending"))
    }
}
