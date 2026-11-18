package com.example.sensus.data

import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.local.db.entity.AiMessageEntity
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.remote.dto.AiTurnResponseDto
import com.example.sensus.data.remote.dto.toDomain
import com.example.sensus.data.repository.AiChatRepositoryImpl
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** FT-24: las tarjetas de oferta que Sensi cita llegan completas del backend y sobreviven en la caché Room. */
/** Timeout holgado: bajo carga de CI 500 ms hacia fallar pruebas de la capa HTTP de forma intermitente. */
private const val HTTP_TIMEOUT_SECONDS = 10L

class AiOffersCacheTest {

    private val server = MockWebServer()
    private val sessionRepository = mockk<SessionRepository>()
    private val dao = mockk<AiMessageDao>(relaxed = true)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private lateinit var repository: AiChatRepositoryImpl

    private val turnJson = """
        {"conversationId":"c1","messageId":"m1","reply":"Encontré el Teclado Gaming.","degraded":false,
         "correlationId":"corr",
         "offersCited":[{"offerId":"off_1","title":"Teclado Gaming","merchantId":"m_1","merchantName":"Tienda 3B",
           "offerPriceMxn":150.0,"regularPriceMxn":200.0,"availableStock":1,"isOpenNow":true,"distanceKm":0.4,
           "description":"Teclado con RGB","discountPercentage":25,"imageUrl":"https://img/t.jpg",
           "category":"Tecnologia","endsAtUtc":"2026-11-30T00:00:00Z","merchantAddress":"Calle Falsa 123",
           "latitude":19.43,"longitude":-99.13}]}
    """.trimIndent()

    @Before
    fun setup() {
        server.start()
        every { sessionRepository.sessionState } returns
            MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeToken = "t"))
        val client = OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
    fun dto_withFullOfferData_mapsEveryFieldIncludingParsedExpiry() {
        val turn = json.decodeFromString<AiTurnResponseDto>(turnJson).toDomain()

        val offer = turn.offersCited.single()
        assertEquals("Teclado con RGB", offer.description)
        assertEquals(25, offer.discountPercent)
        assertEquals("https://img/t.jpg", offer.imageUrl)
        assertEquals("Tecnologia", offer.category)
        assertEquals("Calle Falsa 123", offer.merchantAddress)
        assertEquals(19.43, offer.latitude!!, 0.0)
        assertEquals(-99.13, offer.longitude!!, 0.0)
        assertNotNull(offer.endsAtMillis)
    }

    @Test
    fun dto_withBasicOfferOnly_keepsOptionalFieldsNull() {
        val basic = """{"conversationId":"c","messageId":"m","reply":"x","offersCited":[
            {"offerId":"o","title":"Pan","merchantId":"m","merchantName":"T","offerPriceMxn":1.0,
             "regularPriceMxn":2.0,"availableStock":1,"isOpenNow":true}]}"""
        val offer = json.decodeFromString<AiTurnResponseDto>(basic).toDomain().offersCited.single()

        assertNull(offer.description)
        assertNull(offer.imageUrl)
        assertNull(offer.latitude)
        assertNull(offer.endsAtMillis)
    }

    @Test
    fun sendMessage_cachesCitedOffersAndTheyAreRestoredFromRoom() = runTest {
        val saved = mutableListOf<AiMessageEntity>()
        every { dao.insertMessage(capture(saved)) } returns Unit
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(turnJson)
        )

        val result = repository.sendMessage("c1", "ofertas", null, null)

        assertTrue(result is SensiResult.Success)
        val assistant = saved.single { it.role == "Assistant" }
        assertNotNull(assistant.offersJson)

        every { dao.observeMessagesByConversationId("c1") } returns flowOf(listOf(assistant))
        val restored = repository.observeCachedMessages("c1").first().single()
        assertEquals("Teclado Gaming", restored.offersCited.single().title)
        assertEquals("Calle Falsa 123", restored.offersCited.single().merchantAddress)
    }

    @Test
    fun getConversation_networkFails_restoresCitedOffersFromRoom() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val entitySlot = slot<String>()
        val cached = AiMessageEntity(
            "m1",
            "c1",
            "Assistant",
            "Hola",
            null,
            """[{"offerId":"o","title":"Pan","merchantId":"m","merchantName":"T","offerPriceMxn":1.0,
                "regularPriceMxn":2.0,"availableStock":1,"isOpenNow":true,"imageUrl":"https://i/p.jpg"}]""",
            false,
            "2026-11-15T09:00:00Z"
        )
        every { dao.getMessagesByConversationId(capture(entitySlot)) } returns listOf(cached)

        val result = repository.getConversation("c1")

        assertTrue(result is Result.Success)
        assertEquals("https://i/p.jpg", (result as Result.Success).data.single().offersCited.single().imageUrl)
    }

    @Test
    fun cachedMessageWithCorruptOffersJson_degradesToNoCards() = runTest {
        val broken = AiMessageEntity("m1", "c1", "Assistant", "Hola", null, "{no-es-json", false, "2026-11-15T09:00:00Z")
        every { dao.observeMessagesByConversationId("c1") } returns flowOf(listOf(broken))

        val restored = repository.observeCachedMessages("c1").first().single()

        assertTrue(restored.offersCited.isEmpty())
    }
}
