package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.OfferDetailsRepositoryImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfferDetailsApiTest {
    private val server = MockWebServer()
    private val dao = mockk<OfferDao>(relaxed = true)
    private lateinit var repository: OfferDetailsRepositoryImpl
    private val offerId = "00000000-0000-0000-0000-000000000001"
    private val detail = """
        {
          "id":"00000000-0000-0000-0000-000000000001", "merchantId":"merchant-1",
          "merchantName":"Tienda", "merchantAddress":"Calle Central 10", "category":"Abarrotes",
          "title":"Pan", "description":"Pan sellado", "originalPrice":100, "discountPercentage":50,
          "discountedPrice":50, "stock":2, "status":"Published", "imageUrl":null,
          "latitude":20.0, "longitude":-99.0, "startsAt":"2026-01-01T00:00:00.1234567Z",
          "endsAt":"2030-01-01T00:00:00Z", "viewCount":3, "hasActiveClaim":false
        }
    """.trimIndent()

    @Before
    fun setup() {
        server.start()
        val preferences = mockk<SecurePreferences>(relaxed = true)
        every { preferences.getAuthToken() } returns "test-session"
        repository = OfferDetailsRepositoryImpl(NetworkClient(preferences, server.url("/").toString()).offerDetailsApi, dao)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun detailUsesPublishedContractAndAuthenticatedConnection() = runTest {
        server.enqueue(MockResponse().setBody(detail))
        val result = repository.getDetail(offerId)
        assertTrue(result is Result.Success)
        val value = (result as Result.Success).data
        assertEquals("Calle Central 10", value.offer.address)
        assertEquals(2, value.offer.unitsLeft)
        assertEquals(3, value.viewCount)
        assertEquals(50.0, value.offer.discountPrice, 0.0)
        assertTrue(value.canClaim(System.currentTimeMillis()))
        assertEquals("", value.offer.couponCode)
        assertFalse(value.offer.isVerified)
        verify(exactly = 1) { dao.insertOffer(match { it.id == offerId && it.unitsLeft == 2 }) }
        val request = server.takeRequest()
        assertEquals("/api/v1/offers/$offerId", request.path)
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
    }

    @Test
    fun failedDetailIsNotPresentedAsAValidCachedOffer() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.getDetail(offerId)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NotFound)
        assertEquals(1, server.requestCount)
        verify(exactly = 0) { dao.insertOffer(any()) }
    }

    @Test
    fun malformedOrInvalidDatesFailClosed() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        assertTrue(repository.getDetail(offerId) is Result.Failure)
        server.enqueue(MockResponse().setBody(detail.replace("2030-01-01T00:00:00Z", "not-a-date")))
        val result = repository.getDetail(offerId)
        assertTrue((result as Result.Failure).error is AppError.Unprocessable)
    }

    @Test
    fun registerViewRequires204AndDoesNotRetryOnFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.registerView(offerId) is Result.Success)
        assertEquals("POST", server.takeRequest().method)
        server.enqueue(MockResponse().setResponseCode(401))
        val result = repository.registerView(offerId)
        assertTrue((result as Result.Failure).error is AppError.Unauthorized)
        assertEquals("/api/v1/offers/$offerId/view", server.takeRequest().path)
        assertEquals(2, server.requestCount)
    }
}
