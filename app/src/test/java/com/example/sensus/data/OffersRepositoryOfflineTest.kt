package com.example.sensus.data

import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.data.remote.interceptor.AuthInterceptor
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.data.repository.SessionRepositoryImpl
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffersRepositoryOfflineTest {

    @Test
    fun `getOffers returns cached offers from OfferDao when Room is populated`() {
        val dao = mockk<OfferDao>()
        val cachedEntity = OfferEntity(
            "room-1",
            "Pan Artesanal",
            "Recién horneado",
            35.0,
            70.0,
            "Panadería",
            null,
            20.05,
            -99.34,
            "m-1",
            "Panadería La Espiga",
            1000L
        )
        every { dao.getOffers() } returns listOf(cachedEntity)

        val repo = OffersRepositoryImpl(dao)
        val offers = repo.getOffers()

        assertEquals(1, offers.size)
        assertEquals("room-1", offers.first().id)
        assertEquals("Pan Artesanal", offers.first().name)
        assertEquals(50, offers.first().discountPercent)
        verify(exactly = 0) { dao.insertOffers(any()) }
    }

    @Test
    fun `getOffers returns empty list when Room is empty without seeding fake data`() {
        val dao = mockk<OfferDao>()
        every { dao.getOffers() } returns emptyList()

        val repo = OffersRepositoryImpl(dao)
        val offers = repo.getOffers()

        assertTrue(offers.isEmpty())
        verify(exactly = 0) { dao.insertOffers(any()) }
    }

    @Test
    fun `addOffer persists offer in OfferDao`() {
        val dao = mockk<OfferDao>()
        every { dao.insertOffer(any()) } just runs

        val repo = OffersRepositoryImpl(dao)
        val newOffer = ProductOffer(
            id = "new-room-offer",
            name = "Queso Oaxaca",
            storeName = "Cremería Real",
            category = "Lácteos",
            originalPrice = 120.0,
            discountPrice = 80.0,
            discountPercent = 33,
            unitsLeft = 4,
            distanceMeters = 200
        )

        repo.addOffer(newOffer)
        verify(exactly = 1) { dao.insertOffer(match { it.id == "new-room-offer" && it.price == 80.0 }) }
    }

    @Test
    fun `syncOffersFromServer applies server-wins conflict strategy on OfferDao`() {
        val dao = mockk<OfferDao>()
        every { dao.clearOffers() } just runs
        every { dao.insertOffers(any()) } just runs

        val repo = OffersRepositoryImpl(dao)
        val serverCatalog = listOf(
            ProductOffer(
                id = "srv-1",
                name = "Yogurt Natural",
                storeName = "Lácteos del Valle",
                category = "Lácteos",
                originalPrice = 50.0,
                discountPrice = 25.0,
                discountPercent = 50,
                unitsLeft = 10,
                distanceMeters = 500
            )
        )

        repo.syncOffersFromServer(serverCatalog)

        verify(exactly = 1) { dao.clearOffers() }
        verify(exactly = 1) { dao.insertOffers(match { it.size == 1 && it.first().id == "srv-1" }) }
    }

    @Test
    fun `AuthInterceptor clears session and invokes callback on 401 for protected routes`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        val prefs = mockk<SecurePreferences>(relaxed = true)
        every { prefs.getAuthToken() } returns "expired-jwt-token"

        var unauthorizedTriggered = false
        val interceptor = AuthInterceptor(prefs) {
            unauthorizedTriggered = true
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(server.url("/api/v1/users/me"))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(401, response.code)
        assertTrue(unauthorizedTriggered)
        verify(exactly = 1) { prefs.clearAll() }

        server.shutdown()
    }

    @Test
    fun `SessionRepositoryImpl clearSession purges SecurePreferences and SQLCipher DAOs (FT-08 T08_6)`() = runTest {
        val prefs = mockk<SecurePreferences>(relaxed = true)
        val userSessionDao = mockk<UserSessionDao>(relaxed = true)
        val pendingSyncDao = mockk<PendingSyncDao>(relaxed = true)
        val offerDao = mockk<OfferDao>(relaxed = true)
        val resolveSessionDao = mockk<() -> UserSessionDao>()
        every { resolveSessionDao() } returns userSessionDao

        val sessionRepo = SessionRepositoryImpl(
            securePreferences = prefs,
            userSessionDao = resolveSessionDao,
            pendingSyncDao = { pendingSyncDao },
            offerDao = { offerDao }
        )

        verify(exactly = 0) { resolveSessionDao() }
        sessionRepo.clearSession()

        verify(exactly = 1) { resolveSessionDao() }
        verify(exactly = 1) { prefs.clearAll() }
        verify(exactly = 1) { userSessionDao.clearSession() }
        verify(exactly = 1) { pendingSyncDao.clearAll() }
        verify(exactly = 1) { offerDao.clearOffers() }
    }

    @Test
    fun `getOffers excludes Paused Archived Expired and zero-stock offers from public cache`() {
        val dao = mockk<OfferDao>()
        val active = OfferEntity("o1", "Pan", "Desc", 20.0, 40.0, "Panadería", null, 20.0, -99.0, "m1", "T1", 50L).apply {
            unitsLeft = 3
            status = "Published"
        }
        val paused = OfferEntity("o2", "Leche", "Desc", 15.0, 30.0, "Lácteos", null, 20.0, -99.0, "m1", "T1", 50L).apply {
            unitsLeft = 5
            status = "Paused"
        }
        val archived = OfferEntity("o3", "Café", "Desc", 30.0, 60.0, "Bebidas", null, 20.0, -99.0, "m1", "T1", 50L).apply {
            unitsLeft = 4
            status = "Archived"
        }
        val zeroStock = OfferEntity("o4", "Jugo", "Desc", 10.0, 20.0, "Bebidas", null, 20.0, -99.0, "m1", "T1", 50L).apply {
            unitsLeft = 0
            status = "Published"
        }
        every { dao.getOffers() } returns listOf(active, paused, archived, zeroStock)

        val repo = OffersRepositoryImpl(dao)
        val offers = repo.getOffers()

        assertEquals(1, offers.size)
        assertEquals("o1", offers.single().id)
        assertEquals(OfferLifecycleStatus.ACTIVE, offers.single().status)
    }

    @Test
    fun `AuthInterceptor does not clear session on 401 for public nearby endpoints`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        val prefs = mockk<SecurePreferences>(relaxed = true)
        var unauthorizedTriggered = false
        val interceptor = AuthInterceptor(prefs) { unauthorizedTriggered = true }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client.newCall(Request.Builder().url(server.url("/api/v1/comercios/nearby?lat=20&lng=-99&radiusMeters=2000")).build()).execute()
        client.newCall(Request.Builder().url(server.url("/api/v1/merchants/nearby?lat=20&lng=-99&radiusMeters=2000")).build()).execute()

        assertEquals(false, unauthorizedTriggered)
        verify(exactly = 0) { prefs.clearAll() }
        server.shutdown()
    }
}
