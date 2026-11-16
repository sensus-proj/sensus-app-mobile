package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import com.example.sensus.presentation.buyer.ClaimOfferViewEffect
import com.example.sensus.presentation.buyer.ClaimOfferViewIntent
import com.example.sensus.presentation.buyer.ClaimOfferViewModel
import com.example.sensus.presentation.merchant.MerchantSalesViewIntent
import com.example.sensus.presentation.merchant.MerchantSalesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimsViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ClaimsRepository>()
    private val sessions = mockk<SessionRepository>()
    private val session = MutableStateFlow(SessionState(isSessionActive = true, activeToken = "test"))
    private val claim = OfferClaim("claim", "offer", "code", "payload", 1L)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { sessions.sessionState } returns session
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun buyerRequiresExplicitSubmitAndBlocksDuplicateInFlightAndAfterConfirmation() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<OfferClaim>>()
        coEvery { repository.createClaim("offer") } coAnswers { pending.await() }
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 0) { repository.createClaim(any()) }
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.viewState.value.loading)
        pending.complete(Result.Success(claim))
        dispatcher.scheduler.runCurrent()
        assertEquals(claim, vm.viewState.value.result)
        assertFalse(vm.viewState.value.loading)
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.createClaim("offer") }
    }

    @Test
    fun sessionChangeRemovesConfirmedCode() = runTest(dispatcher) {
        coEvery { repository.createClaim("offer") } returns Result.Success(claim)
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        session.value = SessionState()
        dispatcher.scheduler.runCurrent()
        assertNull(vm.viewState.value.result)
        assertTrue(vm.viewState.value.canSubmit)
    }

    @Test
    fun merchantFailureNeverShowsSuccessAndEditClearsPreviousResult() = runTest(dispatcher) {
        coEvery { repository.redeemClaim("bad") } returns Result.Failure(AppError.Forbidden())
        coEvery { repository.redeemClaim("good") } returns Result.Success(ClaimRedemption("c", "o", "m", 1L))
        val vm = MerchantSalesViewModel(RedeemClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(MerchantSalesViewIntent.Submit("bad"))
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.viewState.value.error is AppError.Forbidden)
        assertNull(vm.viewState.value.result)
        vm.processIntent(MerchantSalesViewIntent.Edit("good"))
        vm.processIntent(MerchantSalesViewIntent.Submit("good"))
        dispatcher.scheduler.runCurrent()
        assertEquals("m", vm.viewState.value.result?.merchantId)
        assertEquals("", vm.viewState.value.input)
        vm.processIntent(MerchantSalesViewIntent.Edit("next"))
        assertNull(vm.viewState.value.result)
    }

    @Test
    fun logoutCancelsPendingClaimAndCannotRestoreCode() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<OfferClaim>>()
        coEvery { repository.createClaim("offer") } coAnswers { pending.await() }
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        session.value = SessionState()
        dispatcher.scheduler.runCurrent()
        pending.complete(Result.Success(claim))
        dispatcher.scheduler.runCurrent()
        assertNull(vm.viewState.value.result)
        assertFalse(vm.viewState.value.loading)
    }

    @Test
    fun sameOfferInitializationPreservesSuccessAndEmitsConfirmationOnlyOnce() = runTest(dispatcher) {
        coEvery { repository.createClaim("offer") } returns Result.Success(claim)
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.viewEffect.test {
            vm.processIntent(ClaimOfferViewIntent.Initialize("offer"))
            vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
            dispatcher.scheduler.runCurrent()
            assertEquals(ClaimOfferViewEffect.Confirmed("offer"), awaitItem())
            vm.processIntent(ClaimOfferViewIntent.Initialize("offer"))
            vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
            dispatcher.scheduler.runCurrent()
            assertEquals(claim, vm.viewState.value.result)
            expectNoEvents()
            vm.processIntent(ClaimOfferViewIntent.Initialize("other-offer"))
            assertNull(vm.viewState.value.result)
            assertEquals("other-offer", vm.viewState.value.offerId)
            expectNoEvents()
        }
        coVerify(exactly = 1) { repository.createClaim("offer") }
    }
}
