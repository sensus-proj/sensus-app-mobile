package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.presentation.buyer.ClaimOfferViewEffect
import com.example.sensus.presentation.buyer.ClaimOfferViewIntent
import com.example.sensus.presentation.buyer.ClaimOfferViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimsMemoryRestoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ClaimsRepository>()
    private val sessions = mockk<SessionRepository>()
    private val session = MutableStateFlow(SessionState(isSessionActive = true, activeToken = "test"))
    private val claim = OfferClaim("claim", "offer", "server-code", "server-payload", Long.MAX_VALUE)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { sessions.sessionState } returns session
        coEvery { repository.createClaim("offer") } returns Result.Success(claim)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun switchingOffersRestoresCodeWithoutPostOrRepeatingConfirmationEffect() = runTest(dispatcher) {
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.viewEffect.test {
            vm.processIntent(ClaimOfferViewIntent.Initialize("offer"))
            vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
            dispatcher.scheduler.runCurrent()
            assertEquals(ClaimOfferViewEffect.Confirmed("offer"), awaitItem())
            vm.processIntent(ClaimOfferViewIntent.Initialize("other"))
            assertNull(vm.viewState.value.result)
            vm.processIntent(ClaimOfferViewIntent.Initialize("offer", expectExisting = true))
            assertEquals(claim, vm.viewState.value.result)
            assertFalse(vm.viewState.value.canSubmit)
            vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
            dispatcher.scheduler.runCurrent()
            expectNoEvents()
        }
        coVerify(exactly = 1) { repository.createClaim("offer") }
        coVerify(exactly = 0) { repository.createClaim("other") }
    }

    @Test
    fun expectedClaimWithoutMemoryNeverPostsEvenAfterSameOfferReentry() = runTest(dispatcher) {
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Initialize("offer", expectExisting = true))
        assertTrue(vm.viewState.value.existingCodeUnavailable)
        assertFalse(vm.viewState.value.canSubmit)
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        vm.processIntent(ClaimOfferViewIntent.Initialize("offer"))
        vm.processIntent(ClaimOfferViewIntent.Initialize("other"))
        vm.processIntent(ClaimOfferViewIntent.Initialize("offer"))
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        assertNull(vm.viewState.value.result)
        assertFalse(vm.viewState.value.loading)
        coVerify(exactly = 0) { repository.createClaim(any()) }
    }

    @Test
    fun authenticationIdentityChangeClearsCachedCodes() = runTest(dispatcher) {
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        session.value = session.value.copy(activeToken = "new-session")
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Initialize("offer", expectExisting = true))
        assertNull(vm.viewState.value.result)
        assertTrue(vm.viewState.value.existingCodeUnavailable)
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.createClaim("offer") }
    }

    @Test
    fun ordinarySessionSyncPreservesCachedCodeAcrossOffers() = runTest(dispatcher) {
        val vm = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Submit("offer"))
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Initialize("other"))
        session.value = session.value.copy(hasLocationPermission = true, hasAcceptedTerms = true)
        dispatcher.scheduler.runCurrent()
        vm.processIntent(ClaimOfferViewIntent.Initialize("offer", expectExisting = true))
        assertEquals(claim, vm.viewState.value.result)
        coVerify(exactly = 1) { repository.createClaim("offer") }
    }
}
