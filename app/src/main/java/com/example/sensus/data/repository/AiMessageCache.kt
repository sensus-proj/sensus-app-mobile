package com.example.sensus.data.repository

import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.local.db.entity.AiMessageEntity
import com.example.sensus.data.remote.dto.AiTurnResponseDto
import com.example.sensus.data.remote.dto.CitedOfferDto
import com.example.sensus.data.remote.dto.ConversationMessageDto
import com.example.sensus.data.remote.dto.RecommendedPackageDto
import com.example.sensus.data.remote.dto.toDomain
import com.example.sensus.domain.model.AiChatMessage
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val LOCAL_ID_SUFFIX_LENGTH = 6

/**
 * Caché local (Room) de los mensajes de Sensi. Es accesoria: leerla o escribirla nunca debe tumbar la app ni el
 * turno de chat. Cualquier fallo de base local (clave perdida, archivo corrupto o hilo incorrecto) se trata como
 * caché vacía, por eso aquí sí se captura cualquier excepción. Ver docs/errores-conocidos.md (E-01 y E-02).
 */
internal class AiMessageCache(
    private val dao: AiMessageDao,
    private val json: Json
) {
    suspend fun messages(conversationId: String): List<AiChatMessage> = withContext(Dispatchers.IO) {
        try {
            dao.getMessagesByConversationId(conversationId).map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun observe(conversationId: String): Flow<List<AiChatMessage>> = dao.observeMessagesByConversationId(conversationId)
        .map { entities -> entities.map { it.toDomainModel() } }
        .catch { emit(emptyList()) }

    suspend fun saveTurn(userMessage: String, turn: AiTurnResponseDto) {
        val userEntity = AiMessageEntity(
            "user_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(LOCAL_ID_SUFFIX_LENGTH)}",
            turn.conversationId,
            "User",
            userMessage,
            null,
            false,
            Instant.now().toString()
        )
        val assistantEntity = AiMessageEntity(
            turn.messageId,
            turn.conversationId,
            "Assistant",
            turn.reply,
            turn.recommendedPackage?.let { json.encodeToString(it) },
            turn.offersCited?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
            turn.degraded,
            Instant.now().toString()
        )
        write { dao.insertMessage(userEntity) }
        write { dao.insertMessage(assistantEntity) }
    }

    suspend fun saveMessages(conversationId: String, messages: List<ConversationMessageDto>) {
        write { dao.insertMessages(messages.map { it.toEntity(conversationId) }) }
    }

    suspend fun delete(conversationId: String) {
        write { dao.deleteMessagesByConversationId(conversationId) }
    }

    private suspend fun write(block: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Caché inaccesible: se ignora, el mensaje ya se entregó al usuario.
            }
        }
    }

    private fun ConversationMessageDto.toEntity(conversationId: String) = AiMessageEntity(
        messageId,
        conversationId,
        role,
        content,
        recommendedPackage?.let { json.encodeToString(it) },
        offersCited?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
        degraded,
        createdAtUtc
    )

    private fun AiMessageEntity.toDomainModel(): AiChatMessage = AiChatMessage(
        id = messageId,
        conversationId = conversationId,
        role = role,
        content = content,
        recommendedPackage = packageJson?.let { decodeOrNull<RecommendedPackageDto>(it)?.toDomain() },
        offersCited = offersJson?.let { decodeOrNull<List<CitedOfferDto>>(it)?.map { dto -> dto.toDomain() } } ?: emptyList(),
        degraded = degraded,
        createdAtUtc = createdAtUtc
    )

    private inline fun <reified T> decodeOrNull(raw: String): T? = try {
        json.decodeFromString<T>(raw)
    } catch (_: IllegalArgumentException) {
        null
    }
}
