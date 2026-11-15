package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimsUseCasesTest {
    private val repository = mockk<ClaimsRepository>()

    @Test
    fun blankInputNeverWrites() = runTest {
        assertTrue(CreateClaimUseCase(repository)(" ") is Result.Failure)
        assertTrue(RedeemClaimUseCase(repository)(" ") is Result.Failure)
        coVerify(exactly = 0) { repository.createClaim(any()) }
        coVerify(exactly = 0) { repository.redeemClaim(any()) }
    }

    @Test
    fun redeemPreservesSignedCodeCaseAndPropagatesServerResult() = runTest {
        val expected = Result.Success(ClaimRedemption("claim", "offer", "merchant", 1L))
        coEvery { repository.redeemClaim("AbC.signed-code") } returns expected
        assertEquals(expected, RedeemClaimUseCase(repository)("  AbC.signed-code  "))
        coVerify(exactly = 1) { repository.redeemClaim("AbC.signed-code") }
    }

    @Test
    fun createForwardsOneOfferWithoutQuantity() = runTest {
        val expected = Result.Success(OfferClaim("claim", "offer", "code", "payload", 1L))
        coEvery { repository.createClaim("offer") } returns expected
        assertEquals(expected, CreateClaimUseCase(repository)("offer"))
    }
}
