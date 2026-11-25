package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.NotificationsRepositoryImpl
import com.example.sensus.data.repository.RetrofitAuthRepository
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.NotificationsRepository
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
import org.junit.Before
import org.junit.Test

/** FT-39: contrato HTTP de ubicacion, registro de dispositivo y logout por `deviceId`. */
class NotificationsPushApiTest {
    private val server = MockWebServer()
    private lateinit var client: NetworkClient
    private lateinit var repository: NotificationsRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val preferences = mockk<SecurePreferences>(relaxed = true)
        every { preferences.getAuthToken() } returns "test-session"
        client = NetworkClient(preferences, server.url("/").toString())
        repository = NotificationsRepositoryImpl(client.notificationsApi)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun shareLocationPutsCamelCaseCoordinatesAndAccepts204() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(Result.Success(Unit), repository.shareLocation(UserLocation(19.4326, -99.1332)))
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/users/notification-location", request.path)
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("latitude", "longitude"), body.keys)
        assertEquals("19.4326", body.getValue("latitude").jsonPrimitive.content)
        assertEquals("-99.1332", body.getValue("longitude").jsonPrimitive.content)
    }

    @Test
    fun deleteLocationUsesDeleteAndAccepts204() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(Result.Success(Unit), repository.deleteLocation())
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/users/notification-location", request.path)
    }

    @Test
    fun guestForbiddenIsReportedAsForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        assertEquals(true, (repository.shareLocation(UserLocation(1.0, 1.0)) as Result.Failure).error is AppError.Forbidden)
    }

    @Test
    fun deviceRegistration503IsMarkedAsRetryable() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val result = repository.registerDevice(NotificationsDevice("install-uuid", "token-fixture"))
        assertEquals(Result.Failure(AppError.Generic(NotificationsRepository.PUSH_UNAVAILABLE)), result)
    }

    @Test
    fun logoutSendsDeviceIdAsQueryParameter() = runTest {
        val preferences = mockk<SecurePreferences>(relaxed = true)
        every { preferences.getAuthToken() } returns "test-session"
        val auth = RetrofitAuthRepository(client.authApi, preferences)
        server.enqueue(MockResponse().setResponseCode(204))
        auth.logout("8b0f6f2e-0000-4000-8000-000000000001")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/logout?deviceId=8b0f6f2e-0000-4000-8000-000000000001", request.path)
    }

    @Test
    fun logoutWithoutDeviceIdOmitsTheQuery() = runTest {
        val auth = RetrofitAuthRepository(client.authApi, mockk(relaxed = true))
        server.enqueue(MockResponse().setResponseCode(204))
        auth.logout(null)
        assertEquals("/api/v1/auth/logout", server.takeRequest().path)
    }
}
