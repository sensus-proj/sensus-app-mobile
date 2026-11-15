package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ClaimsApiTest {
    private val server = MockWebServer()
    private val session = MutableStateFlow(SessionState(isSessionActive = true, activeToken = "test", isMerchantRegistered = true))
    private lateinit var repository: ClaimsRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val sessions = mockk<SessionRepository>()
        every { sessions.sessionState } returns session
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType())).build()
            .create(ClaimsApiService::class.java)
        repository = ClaimsRepositoryImpl(api, sessions)
    }

    @After
    fun teardown() = server.shutdown()

    @Test
    fun claim200MapsExactDtoAndSendsNoBody() = runTest {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"claimId":"claim","offerId":"offer","code":"Ab.signed","qrPayload":"server-payload",
                "expiresAtUtc":"2026-11-15T00:00:00Z"}"""
            )
        )
        val result = repository.createClaim("offer") as Result.Success
        assertEquals("server-payload", result.data.qrPayload)
        assertEquals(Instant.parse("2026-11-15T00:00:00Z").toEpochMilli(), result.data.expiresAtEpochMs)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/offers/offer/claim", request.path)
        assertEquals(0L, request.bodySize)
    }

    @Test
    fun redeem200MapsOnlyPublishedFieldsAndEncodesCodeAsPath() = runTest {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"claimId":"claim","offerId":"offer","merchantId":"merchant","redeemedAtUtc":"2026-11-14T00:00:00Z"}"""
            )
        )
        val result = repository.redeemClaim("Ab.signed") as Result.Success
        assertEquals("merchant", result.data.merchantId)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/offers/claims/Ab.signed/redeem", request.path)
        assertEquals(0L, request.bodySize)
    }

    @Test
    fun errorsNeverBecomeSuccessOrRetry() = runTest {
        for (status in listOf(201, 400, 401, 403, 404, 409, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(status))
            assertTrue(repository.redeemClaim("signed") is Result.Failure)
        }
        assertEquals(8, server.requestCount)
    }

    @Test
    fun guestAndMissingTokenAndNonMerchantFailClosed() = runTest {
        session.value = session.value.copy(isGuest = true)
        assertTrue(repository.createClaim("offer") is Result.Failure)
        session.value = session.value.copy(isGuest = false, activeToken = null)
        assertTrue(repository.createClaim("offer") is Result.Failure)
        session.value = session.value.copy(activeToken = "test", isMerchantRegistered = false)
        val failure = repository.redeemClaim("signed") as Result.Failure
        assertTrue(failure.error is AppError.Forbidden)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun malformedDateNeverProducesConfirmedClaim() = runTest {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"claimId":"claim","offerId":"offer","code":"code","qrPayload":"qr","expiresAtUtc":"invalid"}"""
            )
        )
        assertTrue(repository.createClaim("offer") is Result.Failure)
    }

    @Test
    fun malformedJson200FailsClosedWithoutRetry() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{invalid json"))
        val result = repository.createClaim("offer") as Result.Failure
        assertTrue(result.error is AppError.Unprocessable)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun oversizedUtf8Payload200FailsClosed() = runTest {
        val oversized = "é".repeat(1025)
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"claimId":"claim","offerId":"offer","code":"code","qrPayload":"$oversized",
                "expiresAtUtc":"2026-11-15T00:00:00Z"}"""
            )
        )
        val result = repository.createClaim("offer") as Result.Failure
        assertTrue(result.error is AppError.Unprocessable)
        assertEquals(1, server.requestCount)
    }
}
