package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.mapper.RemoteOfferMapper.toDomain as remoteToDomain
import com.example.sensus.data.mapper.RemoteOfferMapper.toRequest
import com.example.sensus.data.media.compressToLimit
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.data.remote.dto.UploadMediaResponse
import com.example.sensus.data.repository.MediaRepositoryImpl
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.data.repository.RetrofitMerchantRepository
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.NearbyOffersQuery
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.ImageModerationRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class PublishingApiTest {
    private val server = MockWebServer()
    private lateinit var client: NetworkClient

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
            "\"category\":\"Grocery\",\"title\":\"Leche 1L\",\"description\":\"Leche entera\",\"originalPrice\":30.0," +
            "\"discountPercentage\":50,\"discountedPrice\":15.0,\"stock\":8,\"startsAt\":\"2026-11-14T00:00:00Z\"," +
            "\"endsAt\":\"2026-11-15T00:00:00Z\",\"distanceMeters\":120.0,\"latitude\":20.0536,\"longitude\":-99.3436}]," +
            "\"totalCount\":1,\"page\":1,\"pageSize\":20}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(nearbyJson))
        val nearby = repo.fetchNearbyOffers(20.0536, -99.3436, 2.0)
        assertTrue(nearby is Result.Success)
        val nearbyOffer = (nearby as Result.Success).data.single()
        assertEquals("Leche 1L", nearbyOffer.name)
        assertEquals("Abarrotes", nearbyOffer.category)
        assertEquals(15.0, nearbyOffer.discountPrice, 0.01)
        assertEquals(8, nearbyOffer.unitsLeft)

        assertEquals("1", server.takeRequest().requestUrl?.queryParameter("page"))
        val secondPageJson = nearbyJson.replace("\"page\":1", "\"page\":2").replace("\"totalCount\":1", "\"totalCount\":41")
        server.enqueue(MockResponse().setResponseCode(200).setBody(secondPageJson))
        val query = NearbyOffersQuery(UserLocation(20.0536, -99.3436), radiusKm = 2.0, page = 2)
        assertEquals(3, repo.fetchNearbyOffersPage(query).getOrNull()?.nextPage)
        assertEquals("2", server.takeRequest().requestUrl?.queryParameter("page"))

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
    fun merchantInventoryPreservesReportedViewsAndOmitsUnknownMetrics() = runTest {
        val rows = listOf(",\"viewsCount\":12", ",\"viewCount\":0", "", ",\"viewCount\":null", ",\"viewCount\":-1")
            .mapIndexed { index, metric -> """{"id":"o$index","title":"Pan","originalPrice":10$metric}""" }
        server.enqueue(MockResponse().setBody("""{"items":${rows.joinToString(prefix = "[", postfix = "]")},"totalCount":5,"pageNumber":1,"pageSize":100}"""))
        val repository = OffersRepositoryImpl(mockk<OfferDao>(), client.offersApi)

        val inventory = repository.fetchMerchantOffers().getOrNull().orEmpty()
        assertEquals(listOf(12, 0, null, null, null), inventory.map { it.viewCount })
    }

    @Test
    fun merchantInventoryReadsOwnOffersAcrossPagesAndDoesNotUseSharedCacheOnFailure() = runTest {
        val dao = mockk<OfferDao>()
        val repo = OffersRepositoryImpl(dao, client.offersApi)
        val pageOneJson = """
            {"items":[{"id":"o1","title":"Pan","originalPrice":100.0,"discountedPrice":50.0,"stock":2,"status":"Paused"}],
            "totalCount":2,"pageNumber":1,"pageSize":100}
        """.trimIndent()
        val pageTwoJson = """
            {"items":[{"id":"o2","title":"Café","originalPrice":80.0,"discountedPrice":60.0,"stock":4,"status":"Paused"}],
            "totalCount":2,"pageNumber":2,"pageSize":100}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(pageOneJson))
        server.enqueue(MockResponse().setResponseCode(200).setBody(pageTwoJson))

        val inventory = repo.fetchMerchantOffers(status = "Paused").getOrNull().orEmpty()
        assertEquals(listOf("o1", "o2"), inventory.map { it.id })
        assertTrue(inventory.all { it.status == OfferLifecycleStatus.PAUSED })
        val request = server.takeRequest()
        assertEquals("/api/v1/merchants/my-offers", request.requestUrl?.encodedPath)
        assertEquals("Paused", request.requestUrl?.queryParameter("status"))
        assertEquals("1", request.requestUrl?.queryParameter("page"))
        val secondRequest = server.takeRequest()
        assertEquals("2", secondRequest.requestUrl?.queryParameter("page"))

        server.enqueue(MockResponse().setResponseCode(403))
        val failure = repo.fetchMerchantOffers() as Result.Failure
        assertTrue(failure.error is AppError.Forbidden)
        verify { dao wasNot Called }
    }

    @Test
    fun merchantInventoryAcceptsLegacyArrayContract() = runTest {
        val repo = OffersRepositoryImpl(mockk<OfferDao>(), client.offersApi)
        val arrayJson = """
            [
              {"id":"o1","title":"Pan","originalPrice":50.0,"discountedPrice":25.0,"stock":4,"status":"Published"},
              {"id":"o2","title":"Café","originalPrice":40.0,"discountedPrice":20.0,"stock":0,"status":"Paused"}
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(arrayJson))

        val inventory = repo.fetchMerchantOffers().getOrNull().orEmpty()
        assertEquals(listOf("o1", "o2"), inventory.map { it.id })
        assertEquals(listOf(4, 0), inventory.map { it.unitsLeft })
        assertEquals(listOf(OfferLifecycleStatus.ACTIVE, OfferLifecycleStatus.PAUSED), inventory.map { it.status })
    }

    @Test
    fun merchantPausesOfferAndCachesServerResponseOffCallerThread() = runTest {
        val dao = mockk<OfferDao>(relaxed = true)
        val caller = Thread.currentThread()
        val writer = AtomicReference<Thread>()
        every { dao.insertOffer(any()) } answers { writer.set(Thread.currentThread()) }
        val pausedOfferJson = """{"id":"o1","title":"Pan","originalPrice":100.0,"discountedPrice":50.0,"stock":2,"status":"Paused"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(pausedOfferJson))

        val result = OffersRepositoryImpl(dao, client.offersApi).changeOfferStatus("o1", OfferLifecycleStatus.PAUSED)

        assertEquals(OfferLifecycleStatus.PAUSED, result.getOrNull()?.status)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/offers/o1/status", request.path)
        assertTrue(request.body.readUtf8().contains("\"newStatus\":\"Paused\""))
        verify(exactly = 1) { dao.insertOffer(match { it.id == "o1" && it.unitsLeft == 2 }) }
        assertNotSame(caller, writer.get())
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
    fun offerCreated201_persistsStockOffCallerThreadAndSendsUtcAndCategoryCode() = runTest {
        val offerDao = mockk<OfferDao>(relaxed = true)
        val caller = Thread.currentThread()
        val writer = AtomicReference<Thread>()
        every { offerDao.insertOffer(any()) } answers { writer.set(Thread.currentThread()) }
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
        assertNotSame(caller, writer.get())
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
    fun mediaUpload_doesNotBypassModerationViaSignedCloudinaryWhenBackendReturns500() = runTest {
        val repository = MediaRepositoryImpl(client.mediaUploadApi, null)

        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"Error interno"}"""))

        val result = repository.uploadAndModerate(byteArrayOf(1, 2, 3), "image/jpeg")
        assertTrue(result is Result.Failure)
        assertEquals(1, server.requestCount)
        assertEquals("/api/v1/media/upload", server.takeRequest().path)
    }

    @Test
    fun mediaUpload_doesNotFallbackOn422Rejection() = runTest {
        val moderator = mockk<ImageModerationRepository>()
        coEvery { moderator.moderateImage(any(), any()) } returns ModerationResult.Approved
        val repository = MediaRepositoryImpl(client.mediaUploadApi, moderator)

        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"detail":"Imagen bloqueada"}"""))
        val rejected = repository.uploadAndModerate(byteArrayOf(1, 2, 3), "image/jpeg")
        assertTrue(rejected is Result.Failure)
        assertTrue((rejected as Result.Failure).error is AppError.ContentRejected)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun mediaUpload_distinguishes400ValidationAnd202Quarantine_andForwardsTextContext() = runTest {
        val repository = MediaRepositoryImpl(client.mediaUploadApi)

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"Límite diario de 20 cargas"}"""))
        val badRequest = repository.uploadAndModerate(byteArrayOf(1, 2), "image/jpeg", "Pan recién horneado")
        assertTrue((badRequest as Result.Failure).error is AppError.Validation)
        val recorded = server.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("Pan recién horneado"))

        server.enqueue(
            MockResponse().setResponseCode(202).setBody(
                """{"mediaId":"m_q","status":"Quarantined","moderationDecision":"Quarantine","message":"En revisión"}"""
            )
        )
        val quarantined = repository.uploadAndModerate(byteArrayOf(1, 2), "image/jpeg")
        assertTrue((quarantined as Result.Failure).error is AppError.Unprocessable)
    }

    @Test
    fun compressor_stopsAtFirstValidSizeAndRejectsUncompressibleInput() {
        assertArrayEquals(ByteArray(2), compressToLimit(3, listOf(90, 50)) { if (it == 90) ByteArray(4) else ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90, 50)) { ByteArray(2) })
        assertNull(compressToLimit(1, listOf(90)) { byteArrayOf() })
        assertEquals(1, ImageUploadPolicy.calculateInSampleSize(1200, 900, 1600))
        assertEquals(2, ImageUploadPolicy.calculateInSampleSize(4000, 3000, 1600))
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
