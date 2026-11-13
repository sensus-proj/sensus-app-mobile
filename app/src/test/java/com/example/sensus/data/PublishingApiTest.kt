package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.entity.FeedPostEntity
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.media.compressToLimit
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.remote.api.FeedApiService
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.data.repository.MediaRepositoryImpl
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UploadedMedia
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PublishingApiTest {
    private val server = MockWebServer()
    private lateinit var client: NetworkClient
    private val dao = mockk<FeedPostDao>(relaxed = true)
    private val postJson = """{"id":"post-1","authorName":"Vecino","text":"Pan recién hecho","createdAt":"2026-11-12T12:00:00Z","merchantId":"shop-1"}"""

    @Before
    fun setup() {
        server.start()
        client = NetworkClient(mockk<SecurePreferences>(relaxed = true), server.url("/").toString())
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun mediaUpload_sendsMultipartAndRequiresExplicitApproval() = runTest {
        val repository = MediaRepositoryImpl(client.mediaUploadApi)
        server.enqueue(MockResponse().setBody("""{"mediaId":"m1","url":"https://example.com/x.jpg","approved":true}"""))
        assertTrue(repository.uploadAndModerate(byteArrayOf(1, 2), "image/jpeg") is Result.Success)
        val request = server.takeRequest()
        assertEquals("/api/v1/media/upload", request.path)
        assertTrue(request.getHeader("Content-Type")!!.contains("multipart/form-data"))
        assertTrue(request.body.readUtf8().contains("sensus_upload.jpg"))
        server.enqueue(MockResponse().setBody("""{"mediaId":"m2","url":"https://example.com/x.jpg"}"""))
        assertTrue(repository.uploadAndModerate(byteArrayOf(1), "image/jpeg") is Result.Failure)
    }

    @Test
    fun mediaUpload_rejects413And422AndOversizeBeforeNetwork() = runTest {
        val repository = MediaRepositoryImpl(client.mediaUploadApi)
        server.enqueue(MockResponse().setResponseCode(413))
        assertTrue((repository.uploadAndModerate(byteArrayOf(1), "image/jpeg") as Result.Failure).error is AppError.Validation)
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Foto rechazada"}"""))
        assertTrue((repository.uploadAndModerate(byteArrayOf(1), "image/jpeg") as Result.Failure).error is AppError.ContentRejected)
        assertTrue(repository.uploadAndModerate(ByteArray(ImageUploadPolicy.MAX_UPLOAD_BYTES + 1), "image/jpeg") is Result.Failure)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun offerCreated201_preservesStockInRoomAndSendsUtc() = runTest {
        val offerDao = mockk<OfferDao>(relaxed = true)
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"o1","title":"Pan","originalPrice":100.0,"discountPrice":60.0,"stock":17,"merchantId":"shop-1","expiresAt":"2026-12-01T20:00:00Z"}"""
            )
        )
        val input = CreateOfferInput("Pan", "Sellado", StoreCategory.BAKERY, 100.0, 60.0, 17, 2_000_000L)
        val result = OffersRepositoryImpl(offerDao, client.offersApi).createOffer(input, UploadedMedia("m1", "https://example.com/x.jpg"))
        assertTrue(result is Result.Success)
        verify { offerDao.insertOffer(match { it.unitsLeft == 17 && it.merchantId == "shop-1" }) }
        val request = server.takeRequest()
        assertEquals("/api/v1/offers", request.path)
        assertTrue(request.body.readUtf8().contains("1970-01-01T00:33:20Z"))
        val created = (result as Result.Success).data
        assertEquals(17, created.toEntity().toDomain().unitsLeft)
        assertEquals(created.expirationDate, created.toEntity().toDomain().expirationDate)
    }

    @Test
    fun rejectedOffer_doesNotInsertInRoom() = runTest {
        val offerDao = mockk<OfferDao>(relaxed = true)
        server.enqueue(MockResponse().setResponseCode(422))
        val input = CreateOfferInput("Pan", "", StoreCategory.BAKERY, 100.0, 60.0, 17, Long.MAX_VALUE)
        assertTrue(OffersRepositoryImpl(offerDao, client.offersApi).createOffer(input, null) is Result.Failure)
        verify(exactly = 0) { offerDao.insertOffer(any()) }
    }

    @Test
    fun nearbyFeed_sendsProximityAndPaginationAndCachesResults() = runTest {
        server.enqueue(MockResponse().setBody("""{"posts":[$postJson],"page":2,"hasMore":false}"""))
        val result = FeedRepositoryImpl(client.feedApi, dao).nearby(FeedQuery(20.0, -99.0, page = 2))
        assertEquals("post-1", (result as Result.Success).data.posts.single().id)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("lat=20.0&lng=-99.0&radiusMeters=2000&page=2&pageSize=20"))
        verify { dao.replacePage(any(), 2, match { it.single().id == "post-1" }) }
    }

    @Test
    fun postCreation201_andModeration422_neverCachesRejectedContent() = runTest {
        val repository = FeedRepositoryImpl(client.feedApi, dao)
        server.enqueue(MockResponse().setResponseCode(201).setBody(postJson))
        assertTrue(repository.createPost(CreatePostInput("Pan", 20.0, -99.0, "shop-1"), null) is Result.Success)
        assertEquals("/api/v1/feed/posts", server.takeRequest().path)
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Contenido rechazado"}"""))
        assertTrue((repository.createPost(CreatePostInput("Pan", 20.0, -99.0), null) as Result.Failure).error is AppError.ContentRejected)
        verify(exactly = 0) { dao.insertPosts(any()) }
    }

    @Test
    fun offlineFeed_usesOnlyCacheForTheSameQuery() = runTest {
        val api = mockk<FeedApiService>()
        coEvery { api.nearby(any(), any(), any(), any(), any()) } throws IOException("offline")
        every { dao.getPage("20.0:-99.0:2000:20", 1) } returns listOf(FeedPostEntity("post-1", "20.0:-99.0:2000:20", 1, 0, postJson))
        val result = FeedRepositoryImpl(api, dao).nearby(FeedQuery(20.0, -99.0)) as Result.Success
        assertTrue(result.data.isCached)
        assertFalse(result.data.hasMore)
        assertEquals("post-1", result.data.posts.single().id)
    }

    @Test
    fun compressor_stopsAtFirstValidSizeAndRejectsUncompressibleInput() {
        assertArrayEquals(ByteArray(2), compressToLimit(3, listOf(90, 50)) { if (it == 90) ByteArray(4) else ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90, 50)) { ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90)) { byteArrayOf() })
    }
}
