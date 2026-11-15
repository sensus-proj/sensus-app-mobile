package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
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
}
