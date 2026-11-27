package com.example.sensus.data

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.EncryptedClaimIndex
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class MyClaimsRepositoryTest {
    private val server = MockWebServer()
    private val sessions = mockk<SessionRepository>()
    private val state = MutableStateFlow(
        SessionState(isSessionActive = true, activeToken = "jwt-a", activeEmail = "owner@example.invalid")
    )
    private lateinit var api: ClaimsApiService

    @Before
    fun setup() {
        server.start()
        every { sessions.sessionState } returns state
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ClaimsApiService::class.java)
    }

    @After
    fun teardown() = server.shutdown()

    private fun fakeEncryptedIndex(): EncryptedClaimIndex {
        val data = mutableMapOf<String, Any?>()
        val prefs = mockk<EncryptedSharedPreferences>()
        every { prefs.all } answers { data.toMutableMap() }
        every { prefs.getString(any(), any()) } answers {
            data[firstArg()] as? String ?: secondArg()
        }
        every { prefs.edit() } answers {
            val pending = mutableMapOf<String, Any?>()
            var clearFirst = false
            val editor = mockk<SharedPreferences.Editor>()
            every { editor.putString(any(), any()) } answers {
                pending[firstArg()] = secondArg()
                editor
            }
            every { editor.remove(any()) } answers {
                pending[firstArg()] = null
                editor
            }
            every { editor.clear() } answers {
                clearFirst = true
                editor
            }
            every { editor.apply() } answers {
                if (clearFirst) data.clear()
                pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
            }
            every { editor.commit() } answers {
                if (clearFirst) data.clear()
                pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
                true
            }
            editor
        }
        return EncryptedClaimIndex { prefs }
    }

    @Test
    fun threeSavedIdsBuildListIncludingExpiredAndRedeemed() = runTest {
        val index = fakeEncryptedIndex()
        index.rememberClaimId("owner@example.invalid", "o1", "c1")
        index.rememberClaimId("owner@example.invalid", "o2", "c2")
        index.rememberClaimId("owner@example.invalid", "o3", "c3")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/offers/claims/c1" -> jsonResponse(
                    """{"claimId":"c1","offerId":"o1","status":"Active","expiresAtUtc":"2099-01-01T00:00:00Z","qrPayload":"qr-1","offerTitle":"Pan"}"""
                )
                "/api/v1/offers/claims/c2" -> jsonResponse(
                    """{"claimId":"c2","offerId":"o2","status":"Redeemed","expiresAtUtc":"2026-11-10T00:00:00Z","offerTitle":"Leche"}"""
                )
                "/api/v1/offers/claims/c3" -> jsonResponse(
                    """{"claimId":"c3","offerId":"o3","status":"Expired","expiresAtUtc":"2026-11-09T00:00:00Z","offerTitle":"Cafe"}"""
                )
                else -> MockResponse().setResponseCode(404)
            }
        }

        val repo = ClaimsRepositoryImpl(api, sessions, index, UnconfinedTestDispatcher(testScheduler))
        val claims = (repo.getMyClaims() as Result.Success).data

        assertEquals(3, server.requestCount)
        assertEquals(setOf("c1", "c2", "c3"), claims.map { it.claimId }.toSet())
        assertEquals(ClaimStatus.ACTIVE, claims.first { it.claimId == "c1" }.status)
        assertEquals(ClaimStatus.REDEEMED, claims.first { it.claimId == "c2" }.status)
        assertEquals(ClaimStatus.EXPIRED, claims.first { it.claimId == "c3" }.status)
    }

    @Test
    fun notFoundPrunesStaleIdFromIndex() = runTest {
        val index = fakeEncryptedIndex()
        index.rememberClaimId("owner@example.invalid", "o1", "c1")
        index.rememberClaimId("owner@example.invalid", "o2", "c2")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/offers/claims/c1" -> jsonResponse(
                    """{"claimId":"c1","offerId":"o1","status":"Active","expiresAtUtc":"2099-01-01T00:00:00Z","qrPayload":"qr-1"}"""
                )
                else -> MockResponse().setResponseCode(404)
            }
        }

        val repo = ClaimsRepositoryImpl(api, sessions, index, UnconfinedTestDispatcher(testScheduler))
        val claims = (repo.getMyClaims() as Result.Success).data

        assertEquals(listOf("c1"), claims.map { it.claimId })
        assertEquals(listOf("c1"), index.getClaimIds("owner@example.invalid"))
        assertNull(index.findClaimId("owner@example.invalid", "o2"))
    }

    @Test
    fun unauthorizedResponseFailsClosed() = runTest {
        val index = fakeEncryptedIndex()
        index.rememberClaimId("owner@example.invalid", "o1", "c1")
        index.rememberClaimId("owner@example.invalid", "o2", "c2")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/offers/claims/c1" -> jsonResponse(
                    """{"claimId":"c1","offerId":"o1","status":"Active","expiresAtUtc":"2099-01-01T00:00:00Z","qrPayload":"qr-1"}"""
                )
                else -> MockResponse().setResponseCode(401)
            }
        }

        val repo = ClaimsRepositoryImpl(api, sessions, index, UnconfinedTestDispatcher(testScheduler))
        val result = repo.getMyClaims() as Result.Failure

        assertTrue(result.error is AppError.Unauthorized)
    }

    @Test
    fun guestSessionFailsClosedWithoutServerCalls() = runTest {
        val index = fakeEncryptedIndex()
        index.rememberClaimId("owner@example.invalid", "o1", "c1")
        state.value = SessionState(isSessionActive = true, isGuest = true)

        val repo = ClaimsRepositoryImpl(api, sessions, index, UnconfinedTestDispatcher(testScheduler))
        val result = repo.getMyClaims() as Result.Failure

        assertTrue(result.error is AppError.Unauthorized)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun indexIsolatesAccountsAndCapsAtFiftyMostRecent() {
        val index = fakeEncryptedIndex()
        repeat(55) { i ->
            index.rememberClaimId("owner@example.invalid", "offer-$i", "claim-$i")
        }
        index.rememberClaimId("other@example.invalid", "offer-x", "claim-other")

        val ownerIds = index.getClaimIds("owner@example.invalid")
        val otherIds = index.getClaimIds("other@example.invalid")

        assertEquals(50, ownerIds.size)
        assertEquals("claim-54", ownerIds.first())
        assertFalse(ownerIds.contains("claim-0"))
        assertFalse(ownerIds.contains("claim-other"))
        assertNull(index.findClaimId("owner@example.invalid", "offer-0"))
        assertEquals(listOf("claim-other"), otherIds)
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
}
