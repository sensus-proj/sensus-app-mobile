package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.local.db.entity.AiMessageEntity
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.repository.AiChatRepositoryImpl
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiLocation
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Timeout holgado: bajo carga de CI 500 ms hacia fallar pruebas de la capa HTTP de forma intermitente. */
private const val HTTP_TIMEOUT_SECONDS = 10L

class AiChatApiTest {

    private val server = MockWebServer()
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeToken = "token"))
    private val sessionRepository = mockk<SessionRepository>()
    private val dao = mockk<AiMessageDao>(relaxed = true)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private lateinit var api: AiChatApiService
    private lateinit var repository: AiChatRepositoryImpl

    private val normalTurnJson = """
        {
            "conversationId": "conv_100",
            "messageId": "msg_200",
            "reply": "Encontré 2 ofertas ideales para ti:",
            "recommendedPackage": {
                "budgetMxn": 50.0,
                "totalPriceMxn": 45.0,
                "savingsMxn": 15.0,
                "items": [
                    {
                        "offerId": "off_1",
                        "title": "Pan de dulce",
                        "merchantId": "merch_1",
                        "merchantName": "Panadería El Sol",
                        "offerPriceMxn": 20.0,
                        "regularPriceMxn": 30.0,
                        "availableStock": 10,
                        "isOpenNow": true,
                        "distanceKm": 0.3
                    }
                ]
            },
            "offersCited": [],
            "degraded": false,
            "degradationReason": null,
            "correlationId": "corr_abc"
        }
    """.trimIndent()

    @Before
    fun setup() {
        server.start()
        every { sessionRepository.sessionState } returns sessionState

        val client = OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiChatApiService::class.java)

        repository = AiChatRepositoryImpl(
            api = api,
            dao = dao,
            sessionRepository = sessionRepository,
            json = json
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun sendMessage_200Normal_cachesUserAndAssistantAndReturnsSuccess() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(normalTurnJson)
        )

        val result = repository.sendMessage(
            conversationId = "conv_100",
            message = "¿Qué hay con $50?",
            screenContext = ScreenContext.HOME,
            location = SensiLocation(20.05, -99.34, 2.0)
        )

        assertTrue(result is SensiResult.Success)
        val success = result as SensiResult.Success
        assertEquals("conv_100", success.turn.conversationId)
        assertEquals("msg_200", success.turn.messageId)
        assertFalse(success.turn.degraded)
        assertNotNull(success.turn.recommendedPackage)
        assertEquals(1, success.turn.recommendedPackage!!.items.size)

        val insertedEntities = mutableListOf<AiMessageEntity>()
        coVerify(exactly = 2) { dao.insertMessage(capture(insertedEntities)) }
        assertEquals("User", insertedEntities[0].role)
        assertEquals("¿Qué hay con $50?", insertedEntities[0].content)
        assertEquals("Assistant", insertedEntities[1].role)
        assertEquals("msg_200", insertedEntities[1].messageId)

        val recordedRequest = server.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/api/v1/ai/messages", recordedRequest.path)
        val requestBody = recordedRequest.body.readUtf8()
        assertTrue(requestBody.contains("\"conversationId\":\"conv_100\""))
        assertTrue(requestBody.contains("\"screenContext\":\"home\""))
    }

    @Test
    fun sendMessage_200Degraded_returnsSuccessWithDegradedTrue() = runTest {
        val responseBody = """
        {
            "conversationId": "conv_100",
            "messageId": "msg_201",
            "reply": "Respuesta en modo básico por alta demanda.",
            "recommendedPackage": null,
            "offersCited": [],
            "degraded": true,
            "degradationReason": "budget_exceeded",
            "correlationId": "corr_def"
        }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody)
        )

        val result = repository.sendMessage("conv_100", "Hola")
        assertTrue(result is SensiResult.Success)
        val turn = (result as SensiResult.Success).turn
        assertTrue(turn.degraded)
        assertEquals("budget_exceeded", turn.degradationReason)
    }

    @Test
    fun sendMessage_400ModerationBlocked_returnsRejected() = runTest {
        val errorJson = """
        {
            "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
            "title": "Ai.MessageBlockedByModeration",
            "status": 400,
            "detail": "El mensaje infringe las normas de seguridad comunitaria.",
            "instance": "/api/v1/ai/messages"
        }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/problem+json")
                .setBody(errorJson)
        )

        val result = repository.sendMessage("conv_100", "contenido inapropiado")
        assertTrue(result is SensiResult.Rejected)
        val rejected = result as SensiResult.Rejected
        assertEquals("Ai.MessageBlockedByModeration", rejected.code)
        assertEquals("El mensaje infringe las normas de seguridad comunitaria.", rejected.message)
    }

    @Test
    fun sendMessage_403GuestForbidden_returnsGuestBlocked() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = repository.sendMessage("conv_100", "Hola")
        assertTrue(result is SensiResult.GuestBlocked)
    }

    @Test
    fun sendMessage_404ConversationNotFound_returnsConversationGone() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.sendMessage("conv_obsoleta", "Hola")
        assertTrue(result is SensiResult.ConversationGone)
    }

    @Test
    fun sendMessage_429RateLimit_extractsRetryAfterHeader() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "35")
                .setBody("""{"detail":"Límite de mensajes por hora alcanzado"}""")
        )

        val result = repository.sendMessage("conv_100", "Hola")
        assertTrue(result is SensiResult.RateLimited)
        assertEquals(35, (result as SensiResult.RateLimited).retryAfterSeconds)
    }

    @Test
    fun sendMessage_500ServerError_returnsUnavailable() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = repository.sendMessage("conv_100", "Hola")
        assertTrue(result is SensiResult.Unavailable)
    }

    @Test
    fun sendMessage_timeoutOrSocketDrop_returnsUnavailable() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val result = repository.sendMessage("conv_100", "Hola")
        assertTrue(result is SensiResult.Unavailable)
    }

    @Test
    fun createOrResume_200Success_returnsConversation() = runTest {
        val responseBody = """
        {
            "conversationId": "conv_new_1",
            "status": "Active",
            "routingTier": "Fast",
            "messageCount": 0,
            "lastScreenContext": "map",
            "markdownVersion": "1.0.0",
            "createdAtUtc": "2026-11-15T12:00:00Z"
        }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody)
        )

        val result = repository.createOrResume(forceNew = true, screenContext = ScreenContext.MAP)
        assertTrue(result is Result.Success)
        val conv = (result as Result.Success).data
        assertEquals("conv_new_1", conv.conversationId)
        assertEquals("Fast", conv.routingTier)
    }

    @Test
    fun createOrResume_guestSession_failsForbiddenWithoutNetwork() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)
        val result = repository.createOrResume(forceNew = false)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Forbidden)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun getConversation_200Success_reversesOrderAndCachesInRoom() = runTest {
        val responseBody = """
        {
            "conversation": {
                "conversationId": "conv_abc",
                "status": "Active",
                "routingTier": "Fast",
                "messageCount": 2,
                "createdAtUtc": "2026-11-15T10:00:00Z"
            },
            "messages": [
                {
                    "messageId": "msg_recent",
                    "role": "Assistant",
                    "content": "Respuesta reciente",
                    "createdAtUtc": "2026-11-15T10:02:00Z"
                },
                {
                    "messageId": "msg_older",
                    "role": "User",
                    "content": "Pregunta antigua",
                    "createdAtUtc": "2026-11-15T10:00:00Z"
                }
            ]
        }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody)
        )

        val result = repository.getConversation("conv_abc", 1, 20)
        assertTrue(result is Result.Success)
        val messages = (result as Result.Success).data
        assertEquals(2, messages.size)
        // Verified reversal: oldest first
        assertEquals("msg_older", messages[0].id)
        assertEquals("msg_recent", messages[1].id)

        coVerify(exactly = 1) { dao.insertMessages(any()) }
    }

    @Test
    fun getConversation_networkFails_returnsCachedMessagesFromRoom() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val cachedEntities = listOf(
            AiMessageEntity(
                "msg_local_1",
                "conv_abc",
                "User",
                "Mensaje en caché",
                null,
                false,
                "2026-11-15T09:00:00Z"
            )
        )
        coEvery { dao.getMessagesByConversationId("conv_abc") } returns cachedEntities

        val result = repository.getConversation("conv_abc")
        assertTrue(result is Result.Success)
        val messages = (result as Result.Success).data
        assertEquals(1, messages.size)
        assertEquals("Mensaje en caché", messages[0].content)
    }

    @Test
    fun deleteConversation_deletesFromRoomAndServer() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.deleteConversation("conv_to_delete")
        assertTrue(result is Result.Success)

        coVerify(exactly = 1) { dao.deleteMessagesByConversationId("conv_to_delete") }
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/api/v1/ai/conversations/conv_to_delete", req.path)
    }

    @Test
    fun observeCachedMessages_emitsMappedDomainMessages() = runTest {
        val entities = listOf(
            AiMessageEntity("m1", "c1", "User", "Hola", null, false, "2026-11-15T08:00:00Z")
        )
        every { dao.observeMessagesByConversationId("c1") } returns flowOf(entities)

        val emitted = repository.observeCachedMessages("c1").first()
        assertEquals(1, emitted.size)
        assertEquals("Hola", emitted[0].content)
        assertTrue(emitted[0].isFromUser)
    }

    @Test
    fun aiChatHttpClient_stripsHttpLoggingInterceptor_neverLogsBody() {
        val securePreferences = mockk<com.example.sensus.data.local.SecurePreferences>(relaxed = true)
        val networkClient = com.example.sensus.data.remote.NetworkClient(
            securePreferences = securePreferences,
            baseUrl = server.url("/").toString()
        )
        val aiClient = networkClient.aiChatHttpClient

        // Verify logging interceptor is absent
        assertFalse(aiClient.interceptors.contains(networkClient.loggingInterceptor))
        // Verify timeout is 30s
        assertEquals(30, aiClient.readTimeoutMillis / 1000)
    }
}
