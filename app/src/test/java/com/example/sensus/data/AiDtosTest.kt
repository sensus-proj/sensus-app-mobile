package com.example.sensus.data

import com.example.sensus.data.remote.dto.AiTurnResponseDto
import com.example.sensus.data.remote.dto.ConversationDetailDto
import com.example.sensus.data.remote.dto.ConversationSummaryDto
import com.example.sensus.data.remote.dto.CreateConversationRequest
import com.example.sensus.data.remote.dto.ProblemDetailsDto
import com.example.sensus.data.remote.dto.ScreenContextDto
import com.example.sensus.data.remote.dto.SendAiMessageRequest
import com.example.sensus.data.remote.dto.toDomain
import com.example.sensus.data.remote.dto.toDto
import com.example.sensus.domain.model.ScreenContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun sendAiMessageRequest_serializesScreenContextInSnakeCase() {
        val request = SendAiMessageRequest(
            conversationId = "conv_123",
            message = "Quiero ofertas de panadería",
            screenContext = ScreenContextDto.OFFER_DETAIL,
            latitude = 20.0536,
            longitude = -99.3436,
            radiusKm = 2.5
        )
        val serialized = json.encodeToString(request)

        assertTrue(serialized.contains("\"screenContext\":\"offer_detail\""))
        assertTrue(serialized.contains("\"conversationId\":\"conv_123\""))
        assertTrue(serialized.contains("\"message\":\"Quiero ofertas de panadería\""))
        assertTrue(serialized.contains("\"radiusKm\":2.5"))
    }

    @Test
    fun screenContext_allEnumsMapToSnakeCase() {
        assertEquals(ScreenContextDto.HOME, ScreenContext.HOME.toDto())
        assertEquals(ScreenContextDto.MAP, ScreenContext.MAP.toDto())
        assertEquals(ScreenContextDto.OFFER_DETAIL, ScreenContext.OFFER_DETAIL.toDto())
        assertEquals(ScreenContextDto.MY_CLAIMS, ScreenContext.MY_CLAIMS.toDto())
        assertEquals(ScreenContextDto.MERCHANT_INVENTORY, ScreenContext.MERCHANT_INVENTORY.toDto())
        assertEquals(ScreenContextDto.MERCHANT_PROFILE, ScreenContext.MERCHANT_PROFILE.toDto())
        assertEquals(ScreenContextDto.USER_PROFILE, ScreenContext.USER_PROFILE.toDto())
        assertEquals(ScreenContextDto.SETTINGS, ScreenContext.SETTINGS.toDto())

        assertEquals(ScreenContext.HOME, ScreenContextDto.HOME.toDomain())
        assertEquals(ScreenContext.MAP, ScreenContextDto.MAP.toDomain())
        assertEquals(ScreenContext.OFFER_DETAIL, ScreenContextDto.OFFER_DETAIL.toDomain())
        assertEquals(ScreenContext.MY_CLAIMS, ScreenContextDto.MY_CLAIMS.toDomain())
        assertEquals(ScreenContext.MERCHANT_INVENTORY, ScreenContextDto.MERCHANT_INVENTORY.toDomain())
        assertEquals(ScreenContext.MERCHANT_PROFILE, ScreenContextDto.MERCHANT_PROFILE.toDomain())
        assertEquals(ScreenContext.USER_PROFILE, ScreenContextDto.USER_PROFILE.toDomain())
        assertEquals(ScreenContext.SETTINGS, ScreenContextDto.SETTINGS.toDomain())
    }

    @Test
    fun createConversationRequest_serializesProperly() {
        val request = CreateConversationRequest(
            forceNew = true,
            screenContext = ScreenContextDto.MAP
        )
        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("\"forceNew\":true"))
        assertTrue(serialized.contains("\"screenContext\":\"map\""))
    }

    @Test
    fun aiTurnResponseDto_deserializesWithRecommendedPackage() {
        val payload = """
        {
            "conversationId": "conv_99",
            "messageId": "msg_88",
            "reply": "Aquí tienes tu paquete sugerido:",
            "recommendedPackage": {
                "budgetMxn": 100.0,
                "totalPriceMxn": 85.0,
                "savingsMxn": 25.0,
                "items": [
                    {
                        "offerId": "off_1",
                        "title": "Arroz 1kg",
                        "merchantId": "merch_1",
                        "merchantName": "Don Luis",
                        "offerPriceMxn": 25.0,
                        "regularPriceMxn": 35.0,
                        "availableStock": 5,
                        "isOpenNow": true,
                        "distanceKm": 0.5
                    }
                ]
            },
            "offersCited": [],
            "degraded": false,
            "degradationReason": null,
            "correlationId": "corr_123"
        }
        """.trimIndent()

        val dto = json.decodeFromString<AiTurnResponseDto>(payload)
        assertEquals("conv_99", dto.conversationId)
        assertEquals("msg_88", dto.messageId)
        assertEquals("Aquí tienes tu paquete sugerido:", dto.reply)
        assertFalse(dto.degraded)
        assertNotNull(dto.recommendedPackage)
        assertEquals(100.0, dto.recommendedPackage!!.budgetMxn, 0.001)
        assertEquals(85.0, dto.recommendedPackage!!.totalPriceMxn, 0.001)
        assertEquals(25.0, dto.recommendedPackage!!.savingsMxn, 0.001)
        assertEquals(1, dto.recommendedPackage!!.items.size)

        val item = dto.recommendedPackage!!.items[0]
        assertEquals("off_1", item.offerId)
        assertEquals("Arroz 1kg", item.title)
        assertEquals("Don Luis", item.merchantName)
        assertEquals(25.0, item.offerPriceMxn, 0.001)
        assertEquals(35.0, item.regularPriceMxn, 0.001)
        assertEquals(5, item.availableStock)
        assertTrue(item.isOpenNow)
        assertEquals(0.5, item.distanceKm!!, 0.001)

        val domain = dto.toDomain()
        assertEquals(dto.conversationId, domain.conversationId)
        assertEquals(dto.messageId, domain.messageId)
        assertEquals(dto.reply, domain.reply)
        assertEquals(1, domain.recommendedPackage?.items?.size)
    }

    @Test
    fun aiTurnResponseDto_deserializesWithoutRecommendedPackage() {
        val payload = """
        {
            "conversationId": "conv_99",
            "messageId": "msg_89",
            "reply": "Hola, ¿en qué te puedo ayudar hoy?",
            "recommendedPackage": null,
            "offersCited": [],
            "degraded": true,
            "degradationReason": "budget_exceeded",
            "correlationId": "corr_456"
        }
        """.trimIndent()

        val dto = json.decodeFromString<AiTurnResponseDto>(payload)
        assertEquals("conv_99", dto.conversationId)
        assertEquals("msg_89", dto.messageId)
        assertNull(dto.recommendedPackage)
        assertTrue(dto.degraded)
        assertEquals("budget_exceeded", dto.degradationReason)
        assertEquals("corr_456", dto.correlationId)

        val domain = dto.toDomain()
        assertNull(domain.recommendedPackage)
        assertTrue(domain.degraded)
    }

    @Test
    fun problemDetailsDto_deserializesValidationAndBusinessErrors() {
        val payload = """
        {
            "type": "https://tools.ietf.org/html/rfc9110#section-15.5.1",
            "title": "Ai.MessageBlockedByModeration",
            "status": 400,
            "detail": "El mensaje no cumple con las políticas de contenido seguro.",
            "instance": "/api/v1/ai/messages"
        }
        """.trimIndent()

        val dto = json.decodeFromString<ProblemDetailsDto>(payload)
        assertEquals("Ai.MessageBlockedByModeration", dto.title)
        assertEquals(400, dto.status)
        assertEquals("El mensaje no cumple con las políticas de contenido seguro.", dto.detail)
        assertEquals("/api/v1/ai/messages", dto.instance)
    }

    @Test
    fun conversationDetailDto_deserializesWithMessages() {
        val payload = """
        {
            "conversation": {
                "conversationId": "conv_01",
                "status": "Active",
                "routingTier": "Fast",
                "messageCount": 2,
                "lastScreenContext": "home",
                "markdownVersion": "1.0.0",
                "createdAtUtc": "2026-11-15T10:00:00Z",
                "updatedAtUtc": "2026-11-15T10:05:00Z"
            },
            "messages": [
                {
                    "messageId": "msg_2",
                    "role": "Assistant",
                    "content": "Respuesta reciente",
                    "recommendedPackage": null,
                    "degraded": false,
                    "createdAtUtc": "2026-11-15T10:05:00Z"
                },
                {
                    "messageId": "msg_1",
                    "role": "User",
                    "content": "Mensaje antiguo",
                    "recommendedPackage": null,
                    "degraded": false,
                    "createdAtUtc": "2026-11-15T10:00:00Z"
                }
            ],
            "pagination": {
                "page": 1,
                "pageSize": 20,
                "totalCount": 2,
                "totalPages": 1
            }
        }
        """.trimIndent()

        val dto = json.decodeFromString<ConversationDetailDto>(payload)
        assertEquals("conv_01", dto.conversation.conversationId)
        assertEquals(2, dto.messages.size)
        assertEquals("msg_2", dto.messages[0].messageId)
        assertEquals("Assistant", dto.messages[0].role)
        assertEquals("msg_1", dto.messages[1].messageId)
        assertEquals("User", dto.messages[1].role)

        val domainConv = dto.conversation.toDomain()
        assertEquals("conv_01", domainConv.conversationId)
        assertEquals(2, domainConv.messageCount)
    }
}
