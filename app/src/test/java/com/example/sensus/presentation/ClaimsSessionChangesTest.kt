package com.example.sensus.presentation

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import com.example.sensus.domain.usecase.RestoreClaimUseCase
import com.example.sensus.presentation.buyer.ClaimOfferViewIntent
import com.example.sensus.presentation.buyer.ClaimOfferViewModel
import com.example.sensus.presentation.merchant.MerchantSalesViewIntent
import com.example.sensus.presentation.merchant.MerchantSalesViewModel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimsSessionChangesTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ClaimsRepository>()
    private val cart = mockk<CartRepository>(relaxed = true)
    private val sessions = mockk<SessionRepository>()
    private val initial = SessionState(isSessionActive = true, activeToken = "test", activeEmail = "test@example.invalid")
    private val session = MutableStateFlow(initial)
    private val claim = OfferClaim("claim", "offer", "code", "payload", Long.MAX_VALUE)
    private val redemption = ClaimRedemption("claim", "offer", "merchant", 1L)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { sessions.sessionState } returns session
        coEvery { repository.restoreClaim(any()) } returns Result.Failure(AppError.NotFound())
        coEvery { repository.createClaim("offer") } returns Result.Success(claim)
        coEvery { repository.redeemClaim("code") } returns Result.Success(redemption)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun locationTermsAndProfileSyncPreserveBothConfirmedResults() = runTest(dispatcher) {
        val buyer = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions, cart, RestoreClaimUseCase(repository))
        val merchant = MerchantSalesViewModel(RedeemClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        buyer.processIntent(ClaimOfferViewIntent.Submit("offer"))
        merchant.processIntent(MerchantSalesViewIntent.Submit("code"))
        dispatcher.scheduler.runCurrent()
        session.value = initial.copy(hasLocationPermission = true, hasAcceptedTerms = true, activeName = "Updated")
        dispatcher.scheduler.runCurrent()
        assertEquals(claim, buyer.viewState.value.result)
        assertEquals(redemption, merchant.viewState.value.result)
    }

    @Test
    fun everyAuthenticationIdentityChangeClearsConfirmedResults() = runTest(dispatcher) {
        val changes = listOf(
            initial.copy(activeEmail = "other@example.invalid"),
            initial.copy(activeToken = "other"),
            initial.copy(isSessionActive = false),
            initial.copy(isGuest = true),
            initial.copy(isMerchantRegistered = true)
        )
        val buyer = ClaimOfferViewModel(CreateClaimUseCase(repository), sessions, cart, RestoreClaimUseCase(repository))
        val merchant = MerchantSalesViewModel(RedeemClaimUseCase(repository), sessions)
        dispatcher.scheduler.runCurrent()
        for (changed in changes) {
            session.value = initial
            dispatcher.scheduler.runCurrent()
            buyer.processIntent(ClaimOfferViewIntent.Submit("offer"))
            merchant.processIntent(MerchantSalesViewIntent.Submit("code"))
            dispatcher.scheduler.runCurrent()
            session.value = changed
            dispatcher.scheduler.runCurrent()
            assertNull(buyer.viewState.value.result)
            assertNull(merchant.viewState.value.result)
        }
    }
}
