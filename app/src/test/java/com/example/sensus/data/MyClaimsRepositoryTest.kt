package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.EncryptedClaimIndex
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MyClaimsRepositoryTest {
    private val api = mockk<ClaimsApiService>()
    private val sessions = mockk<SessionRepository>()
    private val index = mockk<EncryptedClaimIndex>(relaxed = true)
    private val state = MutableStateFlow(SessionState(isSessionActive = true, activeToken = "token", activeEmail = "owner@example.invalid"))
    private val active = ClaimDetailDto("c1", "offer", "Active", "2099-01-01T00:00:00Z", "signed-1", " Pan dulce ")

    private fun repository(): ClaimsRepositoryImpl {
        every { sessions.sessionState } returns state
        every { index.findClaimId(any(), any()) } returns null
        return ClaimsRepositoryImpl(api, sessions, index)
    }

    @Test
    fun listMapsStatusesTitlesAndDropsMalformedRows() = runTest {
        val rows = listOf(
            active,
            active.copy(claimId = "c2", status = "Redeemed", qrPayload = null, offerTitle = " "),
            active.copy(claimId = "c3", status = "Canceled"),
            active.copy(claimId = "c4", expiresAtUtc = "not-a-date"),
            active.copy(claimId = "", status = "Active")
        )
        coEvery { api.getMyClaims() } returns Response.success(rows)
        val claims = (repository().getMyClaims() as Result.Success).data
        assertEquals(listOf("c1", "c2", "c3"), claims.map { it.claimId })
        assertEquals("Pan dulce", claims[0].offerTitle)
        assertNull(claims[1].offerTitle)
        assertEquals(listOf(ClaimStatus.ACTIVE, ClaimStatus.REDEEMED, ClaimStatus.CANCELLED), claims.map { it.status })
    }

    @Test
    fun listWithoutSessionDoesNotCallServer() = runTest {
        state.value = SessionState(isSessionActive = true, isGuest = true)
        val result = repository().getMyClaims() as Result.Failure
        assertTrue(result.error is AppError.Unauthorized)
        coVerify(exactly = 0) { api.getMyClaims() }
    }

    @Test
    fun unpublishedListRouteMapsToNotFound() = runTest {
        coEvery { api.getMyClaims() } returns Response.error(404, "".toResponseBody())
        assertTrue((repository().getMyClaims() as Result.Failure).error is AppError.NotFound)
    }

    @Test
    fun cancelRequiresMatchingCancelledClaim() = runTest {
        coEvery { api.cancelClaim("c1") } returns Response.success(active.copy(status = "Cancelled"))
        val cancelled = (repository().cancelClaim("c1") as Result.Success).data
        assertEquals(ClaimStatus.CANCELLED, cancelled.status)
        coEvery { api.cancelClaim("c1") } returns Response.success(active)
        assertTrue((repository().cancelClaim("c1") as Result.Failure).error is AppError.Unprocessable)
        coEvery { api.cancelClaim("c1") } returns Response.success(active.copy(claimId = "other", status = "Cancelled"))
        assertTrue(repository().cancelClaim("c1") is Result.Failure)
        coEvery { api.cancelClaim("c1") } returns Response.error(409, "".toResponseBody())
        assertTrue((repository().cancelClaim("c1") as Result.Failure).error is AppError.Unprocessable)
        coEvery { api.cancelClaim("c1") } throws IOException("offline")
        assertTrue((repository().cancelClaim("c1") as Result.Failure).error is AppError.Network)
    }

    @Test
    fun unknownClaimIdIsRecoveredFromListAndRemembered() = runTest {
        val rows = listOf(active.copy(offerId = "other"), active.copy(status = "Redeemed", claimId = "old"), active)
        coEvery { api.getMyClaims() } returns Response.success(rows)
        val restored = (repository().restoreClaim("offer") as Result.Success).data
        assertEquals("c1", restored.claimId)
        assertEquals("signed-1", restored.qrPayload)
        verify { index.rememberClaimId("owner@example.invalid", "offer", "c1") }
        coVerify(exactly = 0) { api.getClaim(any()) }
        coVerify(exactly = 0) { api.createClaim(any()) }
    }

    @Test
    fun listedClaimWithoutPayloadFetchesDetail() = runTest {
        coEvery { api.getMyClaims() } returns Response.success(listOf(active.copy(qrPayload = null)))
        coEvery { api.getClaim("c1") } returns Response.success(active.copy(qrPayload = "detail-payload"))
        assertEquals("detail-payload", repository().restoreClaim("offer").getOrNull()?.qrPayload)
    }

    @Test
    fun listWithoutActiveClaimForOfferIsNotFound() = runTest {
        coEvery { api.getMyClaims() } returns Response.success(listOf(active.copy(status = "Expired")))
        assertTrue((repository().restoreClaim("offer") as Result.Failure).error is AppError.NotFound)
        coEvery { api.getMyClaims() } returns Response.success(listOf(active.copy(expiresAtUtc = "2000-01-01T00:00:00Z")))
        assertTrue(repository().restoreClaim("offer") is Result.Failure)
        verify(exactly = 0) { index.rememberClaimId(any(), any(), any()) }
    }
}
