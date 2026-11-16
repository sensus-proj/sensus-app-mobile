package com.example.sensus.data

import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.repository.AiChatRepositoryImpl
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Regresión de docs/errores-conocidos.md (E-01 y E-02): si la base Room local falla (clave de SQLCipher
 * perdida, archivo corrupto o acceso en hilo incorrecto), el chat de Sensi NO debe cerrar la app. La caché
 * es accesoria: un fallo de Room se trata como caché vacía y el turno sigue.
 */
class AiChatLocalCacheFailureTest {

    private val server = MockWebServer()
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeToken = "token"))
    private val sessionRepository = mockk<SessionRepository>()
    private val dao = mockk<AiMessageDao>()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private lateinit var repository: AiChatRepositoryImpl

    private val brokenDatabase = IllegalStateException("file is not a database")

    @Before
    fun setup() {
        server.start()
        every { sessionRepository.sessionState } returns sessionState
        val client = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiChatApiService::class.java)
        repository = AiChatRepositoryImpl(api = api, dao = dao, sessionRepository = sessionRepository, json = json)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun sendMessage_whenRoomWriteFails_stillReturnsSuccess() = runTest {
        every { dao.insertMessage(any()) } throws brokenDatabase
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"conversationId":"c1","messageId":"m1","reply":"Hola","offersCited":[],
                     "toolsInvoked":[],"degraded":false,"correlationId":"corr"}
                    """.trimIndent()
                )
        )

        val result = repository.sendMessage("c1", "hola", null, null, null, null)

        assertTrue(result is SensiResult.Success)
        assertEquals("Hola", (result as SensiResult.Success).turn.reply)
    }

    @Test
    fun getConversation_whenNetworkAndRoomBothFail_returnsFailureInsteadOfThrowing() = runTest {
        every { dao.getMessagesByConversationId(any()) } throws brokenDatabase
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.getConversation("c1")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun getConversation_whenRoomWriteFailsAfterNetworkSuccess_stillReturnsMessages() = runTest {
        every { dao.insertMessages(any()) } throws brokenDatabase
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {"conversation":{"conversationId":"c1","status":"Active","routingTier":"Fast","messageCount":1,
                     "createdAtUtc":"2026-11-15T09:00:00Z"},
                     "messages":[{"messageId":"m1","role":"Assistant","content":"Hola","degraded":false,
                     "createdAtUtc":"2026-11-15T09:00:00Z"}],
                     "pagination":{"page":1,"pageSize":20,"totalCount":1,"totalPages":1}}
                    """.trimIndent()
                )
        )

        val result = repository.getConversation("c1")

        assertTrue(result is Result.Success)
        assertEquals("Hola", (result as Result.Success).data.single().content)
    }

    @Test
    fun deleteConversation_whenRoomFails_doesNotThrow() = runTest {
        every { dao.deleteMessagesByConversationId(any()) } throws brokenDatabase
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.deleteConversation("c1")

        assertTrue(result is Result.Success)
    }

    @Test
    fun observeCachedMessages_whenRoomFlowFails_emitsEmptyList() = runTest {
        every { dao.observeMessagesByConversationId(any()) } returns flow { throw brokenDatabase }

        val messages = repository.observeCachedMessages("c1").first()

        assertTrue(messages.isEmpty())
    }
}
