package com.example.sensus.data

import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.data.remote.interceptor.AuthInterceptor
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.domain.model.ProductOffer
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
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
    fun `getOffers seeds OfferDao when Room is empty`() {
        val dao = mockk<OfferDao>()
        every { dao.getOffers() } returns emptyList()
        every { dao.insertOffers(any()) } just runs

        val repo = OffersRepositoryImpl(dao)
        val offers = repo.getOffers()

        assertTrue(offers.isNotEmpty())
        verify(exactly = 1) { dao.insertOffers(any()) }
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
}
