package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.UploadedMedia
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
        assertEquals("2.0", request.requestUrl!!.queryParameter("radiusKm"))
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

    @Test
    fun createPost_sendsMediaAssetIdAndNeverImageUrl() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"p1","authorName":"Ana","text":"Post con foto","createdAt":"2026-11-26T00:00:00Z"}"""
            )
        )
        val result = repository.createPost(
            CreatePostInput("Post con foto", 20.0, -99.0),
            UploadedMedia("asset-123", "https://res.cloudinary.com/sensus/image/upload/v1/x.jpg")
        )
        assertTrue(result is Result.Success)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"mediaAssetId\":\"asset-123\""))
        assertTrue(!body.contains("imageUrl"))
    }

    @Test
    fun nearbyFeed_sendsRadiusKmAndParsesPagedResultWithMerchantTag_andQuarantines202() = runTest {
        val nearbyJson = """{"items":[{"id":"p1","authorDisplayName":"Sofía",""" +
            """"content":"Pan caliente","createdAtUtc":"2026-11-26T18:00:00Z",""" +
            """"merchantId":"m1","merchantName":"Panadería El Sol",""" +
            """"likesCount":4,"isLikedByMe":true,"commentsCount":2,"isMine":true}],""" +
            """"totalCount":1,"pageNumber":1,"pageSize":20,"hasNextPage":false}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(nearbyJson))

        val pageResult = repository.nearby(com.example.sensus.domain.model.FeedQuery(20.05, -99.34, radiusMeters = 5000))

        assertTrue(pageResult is Result.Success)
        val post = (pageResult as Result.Success).data.posts.single()
        assertEquals("Sofía", post.authorName)
        assertEquals("Panadería El Sol", post.merchantName)
        assertEquals(4, post.likesCount)
        assertTrue(post.isLikedByMe)
        assertEquals(2, post.commentsCount)
        assertTrue(post.isMine)
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("radiusKm=5.0"))

        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"id":"p2","status":"Quarantined"}"""))
        val quarantined = repository.createPost(CreatePostInput("Texto", 20.0, -99.0, merchantId = "m1"), null)
        assertTrue((quarantined as Result.Failure).error is AppError.Unprocessable)
        server.takeRequest()

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"liked":false,"likeCount":3}"""))
        val unliked = repository.setPostLike("p1", false)
        assertEquals(false, (unliked as Result.Success).data.isLikedByMe)
        assertEquals(3, unliked.data.likesCount)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"liked\":false"))
    }
}
