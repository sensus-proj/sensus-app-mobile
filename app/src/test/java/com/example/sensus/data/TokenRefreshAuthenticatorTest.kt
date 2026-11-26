package com.example.sensus.data

import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.NetworkClient
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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

private val REFRESH_RESPONSE_BODY = """
    {
      "token": "fresh-jwt-15m",
      "tokenType": "Bearer",
      "expiresAtUtc": "2026-11-26T12:15:00Z",
      "refreshToken": "refresh-v2",
      "refreshExpiresAtUtc": "2026-12-26T12:00:00Z",
      "user": {
        "id": "u1",
        "nombreCompleto": "Ana",
        "correo": "ana@sensus.app",
        "roles": ["Consumidor"],
        "estatus": true,
        "fechaRegistro": "2026-11-26T12:00:00Z"
      }
    }
""".trimIndent()

class TokenRefreshAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var securePreferences: SecurePreferences

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        securePreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authenticator_refreshesOnceOn401AndRetriesRequestWithNewJwt() = runTest {
        var currentJwt = "expired-jwt"
        var currentRefresh = "refresh-v1"
        every { securePreferences.getAuthToken() } answers { currentJwt }
        every { securePreferences.refreshToken } answers { currentRefresh }
        every { securePreferences.saveAuthToken(any()) } answers {
            currentJwt = firstArg()
        }
        every { securePreferences.refreshToken = any() } answers {
            currentRefresh = firstArg() ?: ""
        }

        var unauthorizedTriggered = false
        val client = NetworkClient(
            securePreferences = securePreferences,
            baseUrl = server.url("/").toString(),
            onUnauthorized = { unauthorizedTriggered = true }
        )

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody(REFRESH_RESPONSE_BODY))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"u1","nombreCompleto":"Ana","correo":"ana@sensus.app","roles":["Consumidor"],"estatus":true,"fechaRegistro":"2026-11-26T12:00:00Z"}"""
            )
        )

        val response = client.profileApi.getProfile()

        assertTrue(response.isSuccessful)
        assertFalse(unauthorizedTriggered)
        assertEquals("fresh-jwt-15m", currentJwt)
        assertEquals("refresh-v2", currentRefresh)
        verify(exactly = 0) { securePreferences.clearAll() }

        val first = server.takeRequest()
        val refreshReq = server.takeRequest()
        val retried = server.takeRequest()
        assertEquals("/api/v1/users/profile", first.path)
        assertEquals("Bearer expired-jwt", first.getHeader("Authorization"))
        assertEquals("/api/v1/auth/refresh", refreshReq.path)
        assertTrue(refreshReq.body.readUtf8().contains("refresh-v1"))
        assertEquals("/api/v1/users/profile", retried.path)
        assertEquals("Bearer fresh-jwt-15m", retried.getHeader("Authorization"))
    }

    @Test
    fun authenticator_clearsSessionAndInvokesCallback_whenRefreshRejectedWith401() = runTest {
        every { securePreferences.getAuthToken() } returns "expired-jwt"
        every { securePreferences.refreshToken } returns "revoked-refresh"
        every { securePreferences.clearAll() } just runs

        var unauthorizedTriggered = false
        val client = NetworkClient(
            securePreferences = securePreferences,
            baseUrl = server.url("/").toString(),
            onUnauthorized = { unauthorizedTriggered = true }
        )

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client.profileApi.getProfile()

        assertEquals(401, response.code())
        assertTrue(unauthorizedTriggered)
        verify(atLeast = 1) { securePreferences.clearAll() }
    }
}
