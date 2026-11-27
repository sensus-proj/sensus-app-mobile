package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.FriendsActivityRepositoryImpl
import com.example.sensus.domain.model.CommunityActivityType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendsActivityRepositoryImplTest {
    private val server = MockWebServer()
    private lateinit var repository: FriendsActivityRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val prefs = mockk<SecurePreferences>(relaxed = true)
        every { prefs.getAuthToken() } returns "jwt-test"
        val client = NetworkClient(prefs, server.url("/").toString())
        repository = FriendsActivityRepositoryImpl(client.friendsApi)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun getFriendsActivityReadsPagedResponseFromBackend() = runTest {
        val body = """
            {
              "items": [
                {
                  "id": "review:r1",
                  "activityType": "StoreReview",
                  "actorUserId": "u1",
                  "actorName": "Sofía",
                  "merchantId": "m1",
                  "targetTitle": "Panadería Sol",
                  "commentText": "Muy buen pan",
                  "rating": 5,
                  "createdAtUtc": "2026-11-26T12:00:00Z"
                },
                {
                  "id": "reply:rep1",
                  "activityType": "ReviewReply",
                  "actorUserId": "u2",
                  "actorName": "Mateo",
                  "merchantId": "m1",
                  "targetTitle": "Panadería Sol",
                  "commentText": "Confirmo!",
                  "createdAtUtc": "2026-11-26T12:05:00Z"
                },
                {
                  "id": "like:l1",
                  "activityType": "OfferLike",
                  "actorUserId": "u3",
                  "actorName": "Lucía",
                  "offerId": "o1",
                  "merchantId": "m1",
                  "targetTitle": "Conchas 2x1",
                  "createdAtUtc": "2026-11-26T12:10:00Z"
                }
              ],
              "totalCount": 3,
              "pageNumber": 1,
              "pageSize": 50
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repository.getFriendsActivity(limit = 50) as Result.Success
        assertEquals(3, result.data.size)
        assertEquals(CommunityActivityType.STORE_REVIEW, result.data[0].type)
        assertEquals("Muy buen pan", result.data[0].commentText)
        assertEquals(CommunityActivityType.REVIEW_REPLY, result.data[1].type)
        assertEquals(CommunityActivityType.OFFER_LIKE, result.data[2].type)

        val request = server.takeRequest()
        assertEquals("/api/v1/community/friends-activity", request.requestUrl?.encodedPath)
        assertEquals("1", request.requestUrl?.queryParameter("page"))
        assertEquals("50", request.requestUrl?.queryParameter("pageSize"))
    }

    @Test
    fun getFriendsActivityMapsHttpErrorsCorrectly() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val result = repository.getFriendsActivity() as Result.Failure
        assertTrue(result.error is AppError.Unauthorized)
    }
}
