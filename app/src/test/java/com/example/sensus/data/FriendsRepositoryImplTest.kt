package com.example.sensus.data

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.data.remote.api.FriendsApiService
import com.example.sensus.data.remote.dto.FriendItemDto
import com.example.sensus.data.remote.dto.FriendsOverviewDto
import com.example.sensus.data.remote.dto.PendingFriendRequestDto
import com.example.sensus.data.remote.dto.SendFriendRequestBody
import com.example.sensus.data.remote.dto.SendFriendRequestResponseDto
import com.example.sensus.data.remote.dto.SentFriendRequestDto
import com.example.sensus.data.repository.FriendsRepositoryImpl
import com.example.sensus.domain.model.AddFriendResult
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsRepositoryImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessionFlow = MutableStateFlow(
        SessionState(
            isSessionActive = true,
            isGuest = false,
            hasAcceptedTerms = true,
            activeEmail = "alberto@sensus.com",
            activeName = "Alberto Cruz"
        )
    )
    private val sessionRepo = mockk<SessionRepository>(relaxed = true) {
        every { sessionState } returns sessionFlow
    }
    private val storedPrefs = mutableMapOf<String, UserPreferenceEntity>()
    private val fakeDao = object : UserPreferenceDao {
        override fun getByKey(key: String): UserPreferenceEntity? = storedPrefs[key]
        override fun upsert(preference: UserPreferenceEntity) {
            storedPrefs[preference.prefKey] = preference
        }
        override fun deleteByKey(key: String) {
            storedPrefs.remove(key)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        storedPrefs.clear()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun inviteCodecGeneratesDeterministicCodeAndParsesWebAndCustomUrls() {
        val code = FriendInviteCodec.generateInviteCode("maria@sensus.com", "Maria Perez")
        assertTrue(code.startsWith("SNS-"))

        val url = FriendInviteCodec.buildInviteUrl(code, "Maria Perez")
        assertTrue(url.startsWith("https://rodnix.com.mx/sensus?code="))

        val parsedWeb = FriendInviteCodec.parseInvite(url)
        assertNotNull(parsedWeb)
        assertEquals(code, parsedWeb!!.inviteCode)
        assertEquals("Maria Perez", parsedWeb.displayName)

        val parsedSlashWeb = FriendInviteCodec.parseInvite("https://rodnix.com.mx/sensus/?code=$code&name=Maria")
        assertNotNull(parsedSlashWeb)
        assertEquals(code, parsedSlashWeb!!.inviteCode)
        assertEquals("Maria", parsedSlashWeb.displayName)

        val parsedApp = FriendInviteCodec.parseInvite("sensus://invitacion?code=$code&name=Maria")
        assertNotNull(parsedApp)
        assertEquals(code, parsedApp!!.inviteCode)
        assertEquals("Maria", parsedApp.displayName)
    }

    @Test
    fun syncPendingAndRespondFlowsNotifyOnceAcceptAndRejectCleanly() = runTest(dispatcher) {
        val mockApi = mockk<FriendsApiService>()
        val pendingReq = PendingFriendRequestDto(
            requestId = "req-1",
            senderInviteCode = "SNS-AAAAAA",
            senderName = "Rocio",
            addedVia = "QR",
            createdAtUtc = "2026-11-16T12:00:00Z"
        )
        coEvery { mockApi.getFriendsOverview() } returns Response.success(
            FriendsOverviewDto(
                myInviteCode = "SNS-SELF00",
                friends = emptyList(),
                pendingReceived = listOf(pendingReq),
                pendingSent = emptyList()
            )
        )
        coEvery { mockApi.sendFriendRequest(any<SendFriendRequestBody>()) } returns Response.success(
            SendFriendRequestResponseDto(
                outcome = "Sent",
                pendingRequest = SentFriendRequestDto("req-2", "SNS-BBBBBB", "Carlos", "QR", "2026-11-16T12:01:00Z")
            )
        )
        coEvery { mockApi.acceptFriendRequest("req-1") } returns Response.success(
            FriendItemDto("u-rocio", "SNS-AAAAAA", "Rocio", "QR", "2026-11-16T12:02:00Z")
        )
        coEvery { mockApi.rejectFriendRequest("req-3") } returns Response.success(Unit)

        val repo = FriendsRepositoryImpl(
            userPreferenceDao = fakeDao,
            sessionRepository = sessionRepo,
            friendsApi = mockApi,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.pendingRequests.value.size)
        assertEquals("req-1", repo.newlyArrivedRequest.value?.requestId)

        repo.markIncomingNotificationHandled()
        assertNull(repo.newlyArrivedRequest.value)

        // Segundo sync del mismo ID no vuelve a disparar notificación duplicada
        assertTrue(repo.syncFromBackend())
        assertNull(repo.newlyArrivedRequest.value)

        val sendResult = repo.addFriendFromRaw("SNS-BBBBBB", "QR")
        assertTrue(sendResult is AddFriendResult.RequestSent)
        assertEquals(1, repo.sentRequests.value.size)

        assertTrue(repo.acceptPendingRequest("req-1"))
        assertTrue(repo.pendingRequests.value.isEmpty())
        assertEquals(1, repo.friends.value.size)
        assertEquals("Rocio", repo.friends.value.first().displayName)

        assertTrue(repo.rejectPendingRequest("req-3"))
    }
}
