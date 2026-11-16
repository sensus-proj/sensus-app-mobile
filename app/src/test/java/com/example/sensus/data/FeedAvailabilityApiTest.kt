package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedAvailabilityApiTest {
    private val server = MockWebServer()
    private val dao = mockk<FeedPostDao>(relaxed = true)
    private lateinit var repository: FeedRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val preferences = mockk<SecurePreferences>(relaxed = true)
        every { preferences.getAuthToken() } returns "test-session"
        val client = NetworkClient(preferences, server.url("/").toString())
        repository = FeedRepositoryImpl(client.feedApi, dao)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun nearby404_preservesAuthenticationAndDoesNotRepresentAnEmptyOrCachedFeed() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.nearby(FeedQuery(20.0, -99.0))
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NotFound)
        val request = server.takeRequest()
        assertEquals("/api/v1/feed/nearby", request.requestUrl!!.encodedPath)
        assertEquals("2000", request.requestUrl!!.queryParameter("radiusMeters"))
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
        assertEquals(1, server.requestCount)
        verify(exactly = 0) { dao.replacePage(any(), any(), any()) }
        verify(exactly = 0) { dao.getPage(any(), any()) }
    }

    @Test
    fun post404_preservesAuthenticationAndNeverReportsSuccessOrRetries() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.createPost(CreatePostInput("Hola comunidad", 20.0, -99.0), null)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NotFound)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/feed/posts", request.path)
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("Hola comunidad"))
        assertEquals(1, server.requestCount)
        verify(exactly = 0) { dao.replacePage(any(), any(), any()) }
    }
}
