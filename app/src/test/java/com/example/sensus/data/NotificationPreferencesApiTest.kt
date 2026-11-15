package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.NotificationsRepositoryImpl
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPreferencesApiTest {
    private val server = MockWebServer()
    private lateinit var repository: NotificationsRepositoryImpl
    private val response = """{"userId":"user-1","pushEnabled":true,"offerAlerts":false,"socialAlerts":true,"radiusKm":9,
        "createdAtUtc":"2026-11-14T00:00:00Z","updatedAtUtc":null}
    """.trimIndent()

    @Before
    fun setup() {
        server.start()
        val preferences = mockk<SecurePreferences>(relaxed = true)
        every { preferences.getAuthToken() } returns "test-session"
        repository = NotificationsRepositoryImpl(NetworkClient(preferences, server.url("/").toString()).notificationsApi)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun getAndPutUsePublishedPathsAndServerValues() = runTest {
        server.enqueue(MockResponse().setBody(response).setHeader("Content-Type", "application/json"))
        assertEquals(Result.Success(NotificationsPreferences(true, false, true, 9)), repository.getSettings())
        val get = server.takeRequest()
        assertEquals("GET", get.method)
        assertEquals("/api/v1/users/settings", get.path)
        assertEquals("Bearer test-session", get.getHeader("Authorization"))
        server.enqueue(MockResponse().setBody(response).setHeader("Content-Type", "application/json"))
        assertEquals(Result.Success(NotificationsPreferences(true, false, true, 9)), repository.updateSettings(NotificationsPreferences(false, true, false, 1)))
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/users/settings", put.path)
        val body = Json.parseToJsonElement(put.body.readUtf8()).jsonObject
        assertEquals(setOf("pushEnabled", "offerAlerts", "socialAlerts", "radiusKm"), body.keys)
        assertEquals("false", body.getValue("pushEnabled").jsonPrimitive.content)
        assertEquals("true", body.getValue("offerAlerts").jsonPrimitive.content)
        assertEquals("false", body.getValue("socialAlerts").jsonPrimitive.content)
        assertEquals("1", body.getValue("radiusKm").jsonPrimitive.content)
    }

    @Test
    fun devicePutUsesSdkIdentityAndExactPlatform() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"deviceId":"installation-id","platform":"Android","token":"sdk-token-from-provider",
            "isActive":true,"registeredAtUtc":"2026-11-14T00:00:00Z","message":"Registered"}"""
            ).setHeader("Content-Type", "application/json")
        )
        assertEquals(Result.Success(Unit), repository.registerDevice(NotificationsDevice("installation-id", "sdk-token-from-provider")))
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/users/device-token", request.path)
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("deviceId", "platform", "token"), body.keys)
        assertEquals("Android", body.getValue("platform").jsonPrimitive.content)
        assertEquals("installation-id", body.getValue("deviceId").jsonPrimitive.content)
        assertEquals("sdk-token-from-provider", body.getValue("token").jsonPrimitive.content)
    }

    @Test
    fun errorsNeverReturnDefaultSettingsOrSuccess() = runTest {
        for (status in listOf(400, 401, 403, 404, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(status).setBody("sensitive-server-detail"))
            val result = repository.getSettings()
            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            if (status == 401) assertTrue(error is AppError.Unauthorized)
            if (status == 403) assertTrue(error is AppError.Forbidden)
            assertTrue(!error.message.contains("sensitive-server-detail"))
        }
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.updateSettings(NotificationsPreferences(true, true, true, 1)) is Result.Failure)
        server.enqueue(MockResponse().setBody("not-json"))
        assertTrue(repository.getSettings() is Result.Failure)
    }

    @Test
    fun inactiveRegistrationAndUnauthorizedPutCannotReportSuccess() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"deviceId":"installation-id","platform":"Android","token":"sdk-token-from-provider",
            "isActive":false,"registeredAtUtc":"2026-11-14T00:00:00Z","message":"Inactive"}"""
            )
        )
        assertTrue(repository.registerDevice(NotificationsDevice("installation-id", "sdk-token-from-provider")) is Result.Failure)
        server.enqueue(MockResponse().setResponseCode(401))
        assertTrue(repository.updateSettings(NotificationsPreferences(true, true, true, 5)) is Result.Failure)
        server.enqueue(MockResponse().setResponseCode(403))
        assertTrue(repository.registerDevice(NotificationsDevice("installation-id", "sdk-token-from-provider")) is Result.Failure)
        assertEquals(3, server.requestCount)
    }
}
