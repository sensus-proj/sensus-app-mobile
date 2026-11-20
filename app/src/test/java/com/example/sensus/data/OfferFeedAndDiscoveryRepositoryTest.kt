package com.example.sensus.data

import android.database.sqlite.SQLiteException
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedOfferDao
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.SearchHistoryDao
import com.example.sensus.data.local.db.entity.FeedOfferEntity
import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.DiscoveryRepositoryImpl
import com.example.sensus.data.repository.InterestsRepositoryImpl
import com.example.sensus.data.repository.OfferFeedRepositoryImpl
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.model.OfferSearchRequest
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

class OfferFeedAndDiscoveryRepositoryTest {
    private val server = MockWebServer()
    private val feedOfferDao = mockk<FeedOfferDao>(relaxed = true)
    private val offerDao = mockk<OfferDao>(relaxed = true)
    private val searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true)

    private lateinit var feedRepo: OfferFeedRepositoryImpl
    private lateinit var interestsRepo: InterestsRepositoryImpl
    private lateinit var discoveryRepo: DiscoveryRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val prefs = mockk<SecurePreferences>(relaxed = true)
        every { prefs.getAuthToken() } returns "jwt-test"
        val client = NetworkClient(prefs, server.url("/").toString())
        feedRepo = OfferFeedRepositoryImpl(client.offerFeedApi, feedOfferDao)
        interestsRepo = InterestsRepositoryImpl(client.interestsApi)
        discoveryRepo = DiscoveryRepositoryImpl(client.discoveryApi, offerDao, searchHistoryDao)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun offerFeed_successCachesPage1_andMapsSpanishCategoryAnd403() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "offerId": "o1",
                      "title": "Conchas frescas",
                      "category": "Bakery",
                      "originalPrice": 50.0,
                      "finalPrice": 30.0,
                      "stock": 3,
                      "distanceMeters": 180,
                      "expiresAtUtc": "2026-11-21T10:00:00Z",
                      "merchantId": "m1",
                      "merchantName": "La Espiga",
                      "slot": "exploration"
                    }
                  ],
                  "page": 1,
                  "pageSize": 20,
                  "totalCount": 1,
                  "totalPages": 1,
                  "snapshotId": "11111111-1111-1111-1111-111111111111",
                  "rankingVersion": "feed-v1"
                }
                """.trimIndent()
            )
        )

        val ok = (feedRepo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 1, 20, null)) as Result.Success).data
        assertEquals("Panadería", ok.items.first().categoryDisplayName)
        verify(exactly = 1) { feedOfferDao.replaceFirstPage(any()) }

        server.enqueue(MockResponse().setResponseCode(403))
        val forbidden = feedRepo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 1, 20, null))
        assertTrue(forbidden is Result.Failure && forbidden.error is AppError.Forbidden)
    }

    @Test
    fun offerFeed_networkErrorFallsBackToEncryptedDao_andToleratesCorruptDao() = runTest {
        val jsonItem = """{"offerId":"c1","title":"Cached","category":"Dairy","originalPrice":40.0,"finalPrice":25.0}"""
        every { feedOfferDao.getAllOrdered() } returns listOf(FeedOfferEntity("c1", 0, jsonItem, "feed-v1", 1000L))
        server.shutdown()

        val cached = (feedRepo.getFeed(OfferFeedRequest(19.43, -99.13, 10, 1, 20, null)) as Result.Success).data
        assertTrue(cached.isCached)
        assertEquals("Lácteos", cached.items.first().categoryDisplayName)

        every { feedOfferDao.getAllOrdered() } throws SQLiteException("db error")
        val noCrash = feedRepo.getCachedFirstPage()
        assertEquals(null, noCrash)
    }

    @Test
    fun interestsRepository_readsAndUpdatesCategories() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"categories":["Bakery","Pets"],"updatedAtUtc":"2026-11-20T00:00:00Z"}"""))
        val got = (interestsRepo.getMyInterests() as Result.Success).data
        assertEquals(listOf("Bakery", "Pets"), got)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"categories":["Dairy"]}"""))
        val updated = (interestsRepo.updateMyInterests(listOf("Dairy")) as Result.Success).data
        assertEquals(listOf("Dairy"), updated)

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"Máximo 5"}"""))
        val bad = interestsRepo.updateMyInterests(listOf("Invalid"))
        assertTrue(bad is Result.Failure)
    }

    @Test
    fun discoveryRepository_searchesAndGetsAlternativesAndFallsBackOffline() = runTest {
        val searchBody = """
            {
              "items": [{
                "offerId":"s1","title":"Bolillo","description":"Pan","category":"Bakery",
                "originalPrice":20.0,"finalPrice":12.0,"stock":4,"distanceMeters":100,
                "expiresAtUtc":"2026-11-21","merchantName":"Horno","score":0.95
              }],
              "totalCount":1,"pageNumber":1,"pageSize":20,"totalPages":1
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(searchBody))
        val searchOk = (discoveryRepo.searchOffers(OfferSearchRequest("bol", 19.43, -99.13, 10.0, 1, 20)) as Result.Success).data
        assertEquals("Panadería", searchOk.items.first().categoryDisplayName)

        val altBody = """
            [{
              "offerId":"a1","title":"Concha","category":"Bakery","originalPrice":25.0,
              "finalPrice":15.0,"stock":2,"distanceMeters":150,"expiresAtUtc":"2026-11-21","merchantName":"Horno"
            }]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(altBody))
        val altsOk = (discoveryRepo.getAlternatives("s1", 5) as Result.Success).data
        assertEquals("a1", altsOk.first().offerId)

        discoveryRepo.saveSearchQuery("panadería")
        verify(exactly = 1) { searchHistoryDao.upsert(any()) }
        verify(exactly = 1) { searchHistoryDao.trimToFive() }

        every {
            offerDao.getOffers()
        } returns listOf(
            OfferEntity("l1", "Pan dulce", "Desc", 15.0, 30.0, "Bakery", null, 19.4, -99.1, "m1", "Espiga", 1000L).apply {
                unitsLeft = 3
            }
        )
        server.shutdown()
        val offlineSearch = (discoveryRepo.searchOffers(OfferSearchRequest("pan", 19.43, -99.13, 10.0, 1, 20)) as Result.Success).data
        assertTrue(offlineSearch.isFromLocalFallback)
        assertEquals("l1", offlineSearch.items.first().offerId)
    }
}
