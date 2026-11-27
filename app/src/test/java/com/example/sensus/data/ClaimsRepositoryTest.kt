package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.EncryptedClaimIndex
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClaimsRepositoryTest {
    private val api = mockk<ClaimsApiService>()
    private val sessions = mockk<SessionRepository>()
    private val state = MutableStateFlow(SessionState(isSessionActive = true, activeToken = "first"))
    private val dto = ClaimOfferResultDto("claim", "offer", "code", "payload", "2026-11-15T00:00:00Z")

    private fun repository(): ClaimsRepositoryImpl {
        every { sessions.sessionState } returns state
        return ClaimsRepositoryImpl(api, sessions)
    }

    @Test
    fun responseFromPreviousSessionIsDiscarded() = runTest {
        coEvery { api.createClaim("offer") } coAnswers {
            state.value = state.value.copy(activeToken = "another-account")
            Response.success(dto)
        }
        val result = repository().createClaim("offer") as Result.Failure
        assertTrue(result.error is AppError.Unauthorized)
    }

    @Test
    fun confirmedReservationRestoresFromAccountIndexThroughReadOnlyDetail() = runTest {
        val index = mockk<EncryptedClaimIndex>()
        state.value = state.value.copy(activeEmail = "owner@example.invalid")
        every { sessions.sessionState } returns state
        val storedIds = mutableMapOf<Pair<String, String>, String>()
        every { index.rememberClaimId(any(), any(), any()) } answers {
            storedIds[firstArg<String>() to secondArg<String>()] = thirdArg()
        }
        every { index.findClaimId(any(), any()) } answers { storedIds[firstArg<String>() to secondArg<String>()] }
        val details = ClaimDetailDto("claim", "offer", "Active", "2099-01-01T00:00:00Z", "signed")
        coEvery { api.getClaim("claim") } returns Response.success(details)
        coEvery { api.createClaim("offer") } returns Response.success(dto)
        val repo = ClaimsRepositoryImpl(api, sessions, index)
        assertTrue(repo.createClaim("offer") is Result.Success)
        assertEquals("signed", repo.restoreClaim("offer").getOrNull()?.qrPayload)
        coEvery { api.getClaim("claim") } returns Response.success(details.copy(status = "Redeemed"))
        assertTrue(repo.restoreClaim("offer") is Result.Failure)
        coEvery { api.getClaim("claim") } returns Response.success(details.copy(offerId = "other"))
        assertTrue(repo.restoreClaim("offer") is Result.Failure)
        state.value = state.value.copy(activeEmail = "other@example.invalid")
        assertTrue(repo.restoreClaim("offer") is Result.Failure)
        coVerify(exactly = 1) { api.createClaim(any()) }
        coVerify(exactly = 3) { api.getClaim("claim") }
    }

    @Test
    fun networkFailureDoesNotSynthesizeCode() = runTest {
        coEvery { api.createClaim("offer") } throws IOException("offline")
        val result = repository().createClaim("offer") as Result.Failure
        assertTrue(result.error is AppError.Network)
    }

    @Test
    fun mismatchingOfferAndBlankPayloadAreRejected() = runTest {
        coEvery { api.createClaim("offer") } returns Response.success(dto.copy(offerId = "different"))
        assertTrue(repository().createClaim("offer") is Result.Failure)
        coEvery { api.createClaim("offer") } returns Response.success(dto.copy(qrPayload = ""))
        assertTrue(repository().createClaim("offer") is Result.Failure)
    }

    @Test(expected = CancellationException::class)
    fun coroutineCancellationIsNotConvertedToError() = runTest {
        coEvery { api.createClaim("offer") } throws CancellationException("cancelled")
        repository().createClaim("offer")
    }

    @Test
    fun unrelatedSessionSyncDoesNotInvalidateResponse() = runTest {
        coEvery { api.createClaim("offer") } coAnswers {
            state.value = state.value.copy(hasLocationPermission = true, hasAcceptedTerms = true, activeName = "Updated")
            Response.success(dto)
        }
        assertTrue(repository().createClaim("offer") is Result.Success)
    }

    @Test
    fun serializationAndHttpExceptionsBecomeFailures() = runTest {
        coEvery { api.createClaim("offer") } throws SerializationException("invalid payload")
        val serialization = repository().createClaim("offer") as Result.Failure
        assertTrue(serialization.error is AppError.Unprocessable)
        coEvery { api.createClaim("offer") } throws HttpException(Response.error<Any>(403, "denied".toResponseBody()))
        val http = repository().createClaim("offer") as Result.Failure
        assertTrue(http.error is AppError.Forbidden)
    }

    @Test
    fun createCartClaim_sendsAllItemsAndIndexesEveryOfferForOneQr() = runTest {
        val index = mockk<EncryptedClaimIndex>(relaxed = true)
        state.value = state.value.copy(activeEmail = "buyer@example.invalid")
        every { sessions.sessionState } returns state
        val offerA = com.example.sensus.domain.model.ProductOffer("o1", "Pan", "Tienda", "Panadería", 20.0, 10.0, 50, 10, 100, merchantId = "m1")
        val offerB = offerA.copy(id = "o2", name = "Café")
        val multiDto = ClaimOfferResultDto(
            claimId = "claim-multi",
            offerId = "o1",
            code = "v1.multi",
            qrPayload = "v1.multi",
            expiresAtUtc = "2026-11-15T00:00:00Z",
            totalUnits = 6,
            items = listOf(
                com.example.sensus.data.remote.dto.OfferClaimItemDto("o1", 4, "Pan"),
                com.example.sensus.data.remote.dto.OfferClaimItemDto("o2", 2, "Café")
            )
        )
        coEvery { api.createCartClaim(any()) } returns Response.success(multiDto)
        val repo = ClaimsRepositoryImpl(api, sessions, index)

        val result = repo.createCartClaim(
            listOf(
                com.example.sensus.domain.model.CartItem(offerA, 4),
                com.example.sensus.domain.model.CartItem(offerB, 2)
            )
        )

        assertTrue(result is Result.Success)
        val claim = (result as Result.Success).data
        assertEquals(6, claim.totalUnits)
        assertEquals(2, claim.items.size)
        io.mockk.verify(exactly = 1) { index.rememberClaimId("buyer@example.invalid", "o1", "claim-multi") }
        io.mockk.verify(exactly = 1) { index.rememberClaimId("buyer@example.invalid", "o2", "claim-multi") }
    }
}
