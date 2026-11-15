package com.example.sensus.data

import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.domain.model.RepublishOfferInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RepublishRepositoryTest {
    private val input = RepublishOfferInput("offer-1", 2000000000000L, 2000003600000L, 8)

    @Test
    fun sendsExactContractAndCachesServerResponse() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val client = NetworkClient(mockk<SecurePreferences>(relaxed = true), server.url("/").toString())
            val dao = mockk<OfferDao>(relaxed = true)
            server.enqueue(
                MockResponse().setBody(
                    """{"id":"offer-1","title":"Pan","originalPrice":100.0,"discountedPrice":60.0,"stock":6,"status":"Published","version":9}"""
                )
            )
            val result = OffersRepositoryImpl(dao, client.offersApi).republishOffer(input) as Result.Success
            assertEquals(6, result.data.unitsLeft)
            assertEquals(9, result.data.version)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/offers/offer-1/republish", request.path)
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(setOf("newStartsAt", "newEndsAt", "newStock"), body.keys)
            assertEquals("2033-05-18T03:33:20Z", body.getValue("newStartsAt").jsonPrimitive.content)
            assertEquals("2033-05-18T04:33:20Z", body.getValue("newEndsAt").jsonPrimitive.content)
            assertEquals("8", body.getValue("newStock").jsonPrimitive.content)
            verify(exactly = 1) { dao.insertOffer(match { it.id == "offer-1" && it.unitsLeft == 6 }) }
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun failureAndUnexpectedSuccessDoNotCacheOrRetry() = runTest {
        val api = mockk<OffersApiService>()
        val dao = mockk<OfferDao>(relaxed = true)
        val dto = OfferDto("offer-1", "Pan", originalPrice = 100.0)
        coEvery { api.republishOffer(any(), any()) } returns Response.success(201, dto)
        val repository = OffersRepositoryImpl(dao, api)
        assertTrue(repository.republishOffer(input) is Result.Failure)
        coEvery { api.republishOffer(any(), any()) } returns Response.success(null)
        assertTrue(repository.republishOffer(input) is Result.Failure)
        for (code in listOf(400, 401, 403, 404, 409, 500)) {
            coEvery { api.republishOffer(any(), any()) } returns Response.error(
                code, "{\"detail\":\"Rejected\"}".toResponseBody("application/problem+json".toMediaType())
            )
            assertTrue(repository.republishOffer(input) is Result.Failure)
        }
        coEvery { api.republishOffer(any(), any()) } throws IOException("offline")
        assertTrue(repository.republishOffer(input) is Result.Failure)
        coVerify(exactly = 9) { api.republishOffer(any(), any()) }
        verify(exactly = 0) { dao.insertOffer(any()) }
        assertTrue(OffersRepositoryImpl(dao).republishOffer(input) is Result.Failure)
    }
}
