package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.entity.FeedPostEntity
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.mapper.RemoteOfferMapper.toDomain as remoteToDomain
import com.example.sensus.data.mapper.RemoteOfferMapper.toRequest
import com.example.sensus.data.media.compressToLimit
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.remote.api.FeedApiService
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.data.remote.dto.UploadMediaResponse
import com.example.sensus.data.repository.CloudinarySignedUploader
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.data.repository.MediaRepositoryImpl
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.data.repository.RetrofitMerchantRepository
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.ImageModerationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import okhttp3.logging.HttpLoggingInterceptor
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
import retrofit2.Response

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
    fun mediaUpload_runsProfileModerationCheck_andSupportsBackendSecureUrl() = runTest {
        val moderator = mockk<com.example.sensus.domain.repository.ImageModerationRepository>()
        coEvery { moderator.moderateImage(any(), any()) } returns
            com.example.sensus.domain.model.ModerationResult.Rejected("Imagen inapropiada detectada")
        val repository = MediaRepositoryImpl(client.mediaUploadApi, moderator)

        val rejected = repository.uploadAndModerate(byteArrayOf(1, 2), "image/jpeg")
        assertTrue(rejected is Result.Failure)
        assertTrue((rejected as Result.Failure).error is AppError.ContentRejected)
        assertEquals(0, server.requestCount)

        coEvery { moderator.moderateImage(any(), any()) } returns com.example.sensus.domain.model.ModerationResult.Approved
        val uploadJson = "{\"mediaId\":\"m_oscar\",\"publicId\":\"pub_1\"," +
            "\"secureUrl\":\"https://res.cloudinary.com/sensus/img.jpg\",\"status\":\"Approved\"," +
            "\"moderationDecision\":\"Approved\",\"moderationMessage\":\"OK\"}"
        server.enqueue(MockResponse().setResponseCode(201).setBody(uploadJson))
        val approved = repository.uploadAndModerate(byteArrayOf(1, 2), "image/jpeg")
        assertTrue(approved is Result.Success)
        assertEquals("https://res.cloudinary.com/sensus/img.jpg", (approved as Result.Success).data.url)
    }

    @Test
    fun offersRepository_fetchesNearbyAndMerchantOffers_andUpdatesStock() = runTest {
        val offerDao = mockk<OfferDao>(relaxed = true)
        val repo = OffersRepositoryImpl(offerDao, client.offersApi)

        val nearbyJson = "{\"items\":[{\"id\":\"o_near\",\"merchantId\":\"m_1\",\"merchantName\":\"Tienda UTTT\"," +
            "\"category\":\"Abarrotes\",\"title\":\"Leche 1L\",\"description\":\"Leche entera\",\"originalPrice\":30.0," +
            "\"discountPercentage\":50,\"discountedPrice\":15.0,\"stock\":8,\"startsAt\":\"2026-11-14T00:00:00Z\"," +
            "\"endsAt\":\"2026-11-15T00:00:00Z\",\"distanceMeters\":120.0,\"latitude\":20.0536,\"longitude\":-99.3436}]," +
            "\"totalCount\":1,\"page\":1,\"pageSize\":20}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(nearbyJson))
        val nearby = repo.fetchNearbyOffers(20.0536, -99.3436, 2.0)
        assertTrue(nearby is Result.Success)
        val nearbyOffer = (nearby as Result.Success).data.single()
        assertEquals("Leche 1L", nearbyOffer.name)
        assertEquals(15.0, nearbyOffer.discountPrice, 0.01)
        assertEquals(8, nearbyOffer.unitsLeft)

        val updatedJson = "{\"id\":\"o_near\",\"merchantId\":\"m_1\",\"title\":\"Leche 1L\"," +
            "\"description\":\"Leche entera\",\"originalPrice\":30.0,\"discountPercentage\":50," +
            "\"discountedPrice\":15.0,\"stock\":5,\"startsAt\":\"2026-11-14T00:00:00Z\"," +
            "\"endsAt\":\"2026-11-15T00:00:00Z\",\"status\":\"Published\"}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(updatedJson))
        val updated = repo.updateOfferStock(nearbyOffer, 5)
        assertTrue(updated is Result.Success)
        assertEquals(5, (updated as Result.Success).data.unitsLeft)
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
        val rawBody = request.body.readUtf8()
        assertTrue(rawBody.contains("1970-01-01T00:33:20Z"))
        assertTrue(rawBody.contains("\"category\":\"Bakery\""))
        assertFalse(rawBody.contains("\"category\":\"Panadería\""))
        val created = (result as Result.Success).data
        assertEquals("Panadería", created.category)
        assertEquals(17, created.toEntity().toDomain().unitsLeft)
        assertEquals(created.expirationDate, created.toEntity().toDomain().expirationDate)
    }

    @Test
    fun offerCategory_sendsApiCodeAndMapsCodeBackToDisplayNameOnRead() = runTest {
        StoreCategory.entries.filter { it != StoreCategory.ALL }.forEach { category ->
            val req = CreateOfferInput("Item", "Desc", category, 50.0, 25.0, 3, 2_000_000L).toRequest(null)
            assertEquals(category.apiCode, req.category)
            val dto = OfferDto(
                id = "o_cat",
                title = "Item",
                category = category.apiCode,
                originalPrice = 50.0,
                discountedPrice = 25.0,
                stock = 3
            )
            assertEquals(category.displayName, dto.remoteToDomain().category)
        }
        val legacyDto = OfferDto(id = "o_leg", title = "Legacy", category = "Panadería", originalPrice = 50.0, discountedPrice = 25.0)
        assertEquals("Panadería", legacyDto.remoteToDomain().category)

        val offerDao = mockk<OfferDao>(relaxed = true)
        val repo = OffersRepositoryImpl(offerDao, client.offersApi)
        val nearbyJson = "{\"items\":[{\"id\":\"o_clean\",\"title\":\"Jabon\",\"category\":\"CleaningHome\"," +
            "\"originalPrice\":40.0,\"discountedPrice\":20.0,\"stock\":4}],\"totalCount\":1,\"page\":1,\"pageSize\":20}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(nearbyJson))
        val nearby = repo.fetchNearbyOffers(20.0, -99.0, 2.0) as Result.Success
        assertEquals("Limpieza y Hogar", nearby.data.single().category)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createOfferRequest_rejectsStoreCategoryAll() {
        CreateOfferInput("Pan", "Desc", StoreCategory.ALL, 100.0, 60.0, 2, 2_000_000L).toRequest(null)
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
    fun publishingClients_applyRateMatrixTimeoutsWithoutBodyLogging() {
        listOf(client.offersHttpClient to 20_000, client.mediaUploadHttpClient to 45_000).forEach { (http, millis) ->
            assertEquals(millis, http.connectTimeoutMillis)
            assertEquals(millis, http.readTimeoutMillis)
            assertEquals(millis, http.writeTimeoutMillis)
            assertTrue(http.interceptors.none { it is HttpLoggingInterceptor })
        }
    }

    @Test
    fun mediaUpload_retriesOnceOnTransportTimeoutThenSucceeds() = runTest {
        val api = mockk<MediaApiService>()
        coEvery { api.uploadMedia(any()) } throws SocketTimeoutException("timeout") andThen
            Response.success(UploadMediaResponse("m1", "https://example.com/x.jpg", approved = true))
        val result = MediaRepositoryImpl(api).uploadAndModerate(byteArrayOf(1), "image/jpeg")
        assertEquals(UploadedMedia("m1", "https://example.com/x.jpg"), (result as Result.Success).data)
        coVerify(exactly = 2) { api.uploadMedia(any()) }
    }

    @Test
    fun mediaUpload_failsClosedAfterSingleRetry() = runTest {
        val api = mockk<MediaApiService>()
        coEvery { api.uploadMedia(any()) } throws SocketTimeoutException("timeout")
        val result = MediaRepositoryImpl(api).uploadAndModerate(byteArrayOf(1), "image/jpeg")
        assertTrue((result as Result.Failure).error is AppError.Network)
        coVerify(exactly = 2) { api.uploadMedia(any()) }
    }

    @Test
    fun mediaUpload_neverRetriesHttpResponses() = runTest {
        val repository = MediaRepositoryImpl(client.mediaUploadApi)
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.uploadAndModerate(byteArrayOf(1), "image/jpeg") is Result.Failure)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun offerCreate_isNotRetriedOnTimeoutAndNothingIsCached() = runTest {
        val api = mockk<OffersApiService>()
        val offerDao = mockk<OfferDao>(relaxed = true)
        coEvery { api.createOffer(any()) } throws SocketTimeoutException("timeout")
        val input = CreateOfferInput("Pan", "", StoreCategory.BAKERY, 100.0, 60.0, 5, Long.MAX_VALUE)
        val result = OffersRepositoryImpl(offerDao, api).createOffer(input, null)
        assertTrue((result as Result.Failure).error is AppError.Network)
        coVerify(exactly = 1) { api.createOffer(any()) }
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
    fun mediaUpload_fallsBackToSignedCloudinaryWhenBackendReturns500_afterModerationApproved() = runTest {
        val moderator = mockk<ImageModerationRepository>()
        coEvery { moderator.moderateImage(any(), any()) } returns ModerationResult.Approved
        val uploader = CloudinarySignedUploader(baseUrl = server.url("/").toString())
        val repository = MediaRepositoryImpl(client.mediaUploadApi, moderator, client.profileApi, uploader)

        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"Error interno"}"""))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"cloudName":"demo-cloud","apiKey":"k1","timestamp":1700000000,"folder":"sensus/media","signature":"sig1"}"""
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"public_id":"pub_fb","secure_url":"https://res.cloudinary.com/demo-cloud/image/upload/fb.jpg"}"""
            )
        )

        val result = repository.uploadAndModerate(byteArrayOf(1, 2, 3), "image/jpeg")
        assertTrue(result is Result.Success)
        val media = (result as Result.Success).data
        assertEquals("pub_fb", media.mediaId)
        assertEquals("https://res.cloudinary.com/demo-cloud/image/upload/fb.jpg", media.url)
        assertEquals("/api/v1/media/upload", server.takeRequest().path)
        assertEquals("/api/v1/users/photo-signature", server.takeRequest().path)
        assertEquals("/v1_1/demo-cloud/image/upload", server.takeRequest().path)
    }

    @Test
    fun mediaUpload_doesNotFallbackOn422Rejection() = runTest {
        val moderator = mockk<ImageModerationRepository>()
        coEvery { moderator.moderateImage(any(), any()) } returns ModerationResult.Approved
        val uploader = CloudinarySignedUploader(baseUrl = server.url("/").toString())
        val repository = MediaRepositoryImpl(client.mediaUploadApi, moderator, client.profileApi, uploader)

        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"detail":"Imagen bloqueada"}"""))
        val rejected = repository.uploadAndModerate(byteArrayOf(1, 2, 3), "image/jpeg")
        assertTrue(rejected is Result.Failure)
        assertTrue((rejected as Result.Failure).error is AppError.ContentRejected)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun compressor_stopsAtFirstValidSizeAndRejectsUncompressibleInput() {
        assertArrayEquals(ByteArray(2), compressToLimit(3, listOf(90, 50)) { if (it == 90) ByteArray(4) else ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90, 50)) { ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90)) { byteArrayOf() })
    }

    @Test
    fun offersRepository_updatesOfferWithVersionAndChangesLifecycleStatus() = runTest {
        val offerDao = mockk<OfferDao>(relaxed = true)
        val repo = OffersRepositoryImpl(offerDao, client.offersApi)

        val updateJson = "{\"id\":\"o_1\",\"merchantId\":\"m_1\",\"title\":\"Pan Integral\"," +
            "\"description\":\"Nuevo lote\",\"originalPrice\":100.0,\"discountPercentage\":30," +
            "\"discountedPrice\":70.0,\"stock\":12,\"startsAt\":\"2026-11-14T00:00:00Z\"," +
            "\"endsAt\":\"2026-11-15T00:00:00Z\",\"status\":\"Published\",\"version\":4}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(updateJson))

        val updateRes = repo.updateOffer(
            UpdateOfferInput(
                offerId = "o_1",
                title = "Pan Integral",
                description = "Nuevo lote",
                originalPrice = 100.0,
                discountPrice = 70.0,
                stock = 12,
                expiresAtMillis = 2_000_000L,
                expectedVersion = 3
            )
        )
        assertTrue(updateRes is Result.Success)
        val updated = (updateRes as Result.Success).data
        assertEquals(4, updated.version)
        assertEquals(OfferLifecycleStatus.ACTIVE, updated.status)
        val putReq = server.takeRequest()
        assertEquals("/api/v1/offers/o_1", putReq.path)
        assertTrue(putReq.body.readUtf8().contains("\"expectedVersion\":3"))

        val pausedJson = "{\"id\":\"o_1\",\"merchantId\":\"m_1\",\"title\":\"Pan Integral\"," +
            "\"description\":\"Nuevo lote\",\"originalPrice\":100.0,\"discountPercentage\":30," +
            "\"discountedPrice\":70.0,\"stock\":12,\"startsAt\":\"2026-11-14T00:00:00Z\"," +
            "\"endsAt\":\"2026-11-15T00:00:00Z\",\"status\":\"Paused\",\"version\":5}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(pausedJson))
        val statusRes = repo.changeOfferStatus("o_1", OfferLifecycleStatus.PAUSED)
        assertTrue(statusRes is Result.Success)
        assertEquals(OfferLifecycleStatus.PAUSED, (statusRes as Result.Success).data.status)
        val statusReq = server.takeRequest()
        assertEquals("/api/v1/offers/o_1/status", statusReq.path)
        assertTrue(statusReq.body.readUtf8().contains("\"Paused\""))
    }

    @Test
    fun merchantRepository_fetchesNearbyComercios_andGracefullyFallsBackWhen404() = runTest {
        val repo = RetrofitMerchantRepository(client.merchantApi)
        val nearbyBody = "[{\"id\":\"m_9\",\"usuarioId\":\"u_9\",\"nombreComercial\":\"Tiendita Real\"," +
            "\"direccion\":\"Av. Central 10\",\"codigoPostal\":\"42800\",\"latitud\":20.053,\"longitud\":-99.343,\"horarios\":[]}]"
        server.enqueue(MockResponse().setResponseCode(200).setBody(nearbyBody))

        val ok = repo.getNearbyMerchants(20.053, -99.343, 2000)
        assertTrue(ok is Result.Success)
        assertEquals("Tiendita Real", (ok as Result.Success).data.single().nombreComercial)

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        val fallback = repo.getNearbyMerchants(20.053, -99.343, 2000)
        assertTrue(fallback is Result.Success)
        assertTrue((fallback as Result.Success).data.isEmpty())
    }
}
