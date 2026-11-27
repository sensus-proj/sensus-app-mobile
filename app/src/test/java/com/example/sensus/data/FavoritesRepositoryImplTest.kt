package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.FavoriteDao
import com.example.sensus.data.local.db.entity.FavoriteEntity
import com.example.sensus.data.remote.api.FavoritesApiService
import com.example.sensus.data.repository.FavoritesRepositoryImpl
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private val storedRows = linkedMapOf<String, FavoriteEntity>()
    private val fakeDao = object : FavoriteDao {
        override fun getByOwner(ownerKey: String): List<FavoriteEntity> = storedRows.values.filter { it.ownerKey == ownerKey }
        override fun upsertAll(items: List<FavoriteEntity>) {
            items.forEach { storedRows[it.offerId] = it }
        }
        override fun upsert(item: FavoriteEntity) {
            storedRows[item.offerId] = item
        }
        override fun deleteById(ownerKey: String, offerId: String) {
            if (storedRows[offerId]?.ownerKey == ownerKey) storedRows.remove(offerId)
        }
        override fun clearByOwner(ownerKey: String) {
            storedRows.entries.removeIf { it.value.ownerKey == ownerKey }
        }
        override fun clearAll() = storedRows.clear()
    }

    private val sessionFlow = MutableStateFlow(
        SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com", activeToken = "jwt-a")
    )
    private val sessionRepo = mockk<SessionRepository> {
        every { sessionState } returns sessionFlow
    }

    private lateinit var api: FavoritesApiService

    private val offerA = ProductOffer(
        id = "offer-a",
        name = "Pan Integral",
        storeName = "Panadería Sol",
        category = "Panadería",
        originalPrice = 80.0,
        discountPrice = 40.0,
        discountPercent = 50,
        unitsLeft = 4,
        distanceMeters = 100,
        merchantId = "m-1"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        storedRows.clear()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FavoritesApiService::class.java)
    }

    @After
    fun teardown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    private fun createRepo() = FavoritesRepositoryImpl(
        api = api,
        favoriteDao = { fakeDao },
        sessionRepository = sessionRepo,
        ioDispatcher = dispatcher,
        scope = CoroutineScope(SupervisorJob() + dispatcher)
    )

    @Test
    fun syncFavorites200PersistsInRoomAnd404DoesNotWipeCache() = runTest(dispatcher) {
        server.enqueue(
            jsonResponse(
                """
                {
                  "items": [
                    {
                      "id": "offer-a",
                      "merchantId": "m-1",
                      "merchantName": "Panadería Sol",
                      "category": "Bakery",
                      "title": "Pan Integral",
                      "description": "Recién horneado",
                      "originalPrice": 80.0,
                      "discountPercentage": 50,
                      "discountedPrice": 40.0,
                      "stock": 4,
                      "imageUrl": null,
                      "startsAt": "2026-11-26T10:00:00Z",
                      "endsAt": "2026-11-27T20:00:00Z",
                      "latitude": 20.05,
                      "longitude": -99.34,
                      "likesCount": 5,
                      "favoritedAtUtc": "2026-11-26T12:00:00Z"
                    }
                  ],
                  "totalCount": 1,
                  "pageNumber": 1,
                  "pageSize": 20,
                  "totalPages": 1,
                  "hasPreviousPage": false,
                  "hasNextPage": false
                }
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setResponseCode(404))

        val repo = createRepo()
        advanceUntilIdle()

        val ok = repo.syncFavorites()
        assertTrue(ok is Result.Success)
        assertTrue(repo.isFavorite("offer-a"))
        assertEquals(1, storedRows.size)

        val notFound = repo.syncFavorites()
        assertTrue(notFound is Result.Failure && notFound.error is AppError.NotFound)
        assertTrue(repo.isFavorite("offer-a"))
        assertEquals(1, storedRows.size)
    }

    @Test
    fun toggleFavoriteCommitsOn200AndRollsBackOnError() = runTest(dispatcher) {
        server.enqueue(jsonResponse("""{"offerId":"offer-a","favorited":true,"favoriteCount":9}"""))
        server.enqueue(MockResponse().setResponseCode(429))

        val repo = createRepo()
        advanceUntilIdle()

        val added = repo.toggleFavorite(offerA)
        assertTrue(added is Result.Success && added.data.favorited)
        assertTrue(repo.isFavorite("offer-a"))
        assertEquals(1, storedRows.size)

        val rateLimited = repo.toggleFavorite(offerA)
        assertTrue(rateLimited is Result.Failure && rateLimited.error is AppError.Generic)
        assertTrue(repo.isFavorite("offer-a"))
        assertEquals(1, storedRows.size)
    }

    @Test
    fun switchingAccountIsolatesCachedFavoritesAndMalformedJsonReturnsUnprocessable() = runTest(dispatcher) {
        server.enqueue(jsonResponse("""{"offerId":"offer-a","favorited":true,"favoriteCount":1}"""))
        server.enqueue(jsonResponse("""{invalid-json"""))

        val repo = createRepo()
        advanceUntilIdle()
        repo.toggleFavorite(offerA)
        assertTrue(repo.isFavorite("offer-a"))

        sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, activeEmail = "other@sensus.com", activeToken = "jwt-b")
        advanceUntilIdle()
        assertFalse(repo.isFavorite("offer-a"))

        val badJson = repo.syncFavorites()
        assertTrue(badJson is Result.Failure && badJson.error is AppError.Unprocessable)
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
}
