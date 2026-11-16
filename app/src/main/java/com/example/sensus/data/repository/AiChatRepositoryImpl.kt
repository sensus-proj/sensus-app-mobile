package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.local.db.entity.AiMessageEntity
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.remote.dto.CitedOfferDto
import com.example.sensus.data.remote.dto.CreateConversationRequest
import com.example.sensus.data.remote.dto.ProblemDetailsDto
import com.example.sensus.data.remote.dto.RecommendedPackageDto
import com.example.sensus.data.remote.dto.SendAiMessageRequest
import com.example.sensus.data.remote.dto.toDomain
import com.example.sensus.data.remote.dto.toDto
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.SessionRepository
import java.io.IOException
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

private const val HTTP_OK = 200
private const val HTTP_NO_CONTENT = 204
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429

class AiChatRepositoryImpl(
    private val api: AiChatApiService,
    private val dao: AiMessageDao,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }
) : AiChatRepository {

    override suspend fun createOrResume(
        forceNew: Boolean,
        screenContext: ScreenContext?
    ): Result<AiConversation> = try {
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            Result.Failure(AppError.Forbidden("Los invitados no tienen acceso a Sensi."))
        } else {
            val response = api.createOrResume(
                CreateConversationRequest(
                    forceNew = forceNew,
                    screenContext = screenContext?.toDto()
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.toDomain())
            } else {
                when (response.code()) {
                    HTTP_UNAUTHORIZED -> Result.Failure(AppError.Unauthorized())
                    HTTP_FORBIDDEN -> Result.Failure(AppError.Forbidden())
                    HTTP_NOT_FOUND -> Result.Failure(AppError.NotFound())
                    HTTP_BAD_REQUEST -> {
                        val problem = parseProblem(response.errorBody()?.string())
                        Result.Failure(AppError.Validation(problem?.detail ?: "Solicitud inválida."))
                    }
                    else -> Result.Failure(AppError.Network())
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.Failure(AppError.Network("Error de conexión al iniciar conversación."))
    } catch (e: Exception) {
        Result.Failure(AppError.Generic(e.message ?: "Error inesperado."))
    }

    override suspend fun sendMessage(
        conversationId: String?,
        message: String,
        screenContext: ScreenContext?,
        latitude: Double?,
        longitude: Double?,
        radiusKm: Double?
    ): SensiResult = try {
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            SensiResult.GuestBlocked
        } else {
            val request = SendAiMessageRequest(
                conversationId = conversationId,
                message = message,
                screenContext = screenContext?.toDto(),
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm
            )
            val response = api.sendMessage(request)
            when {
                response.isSuccessful && response.body() != null -> {
                    val turn = response.body()!!
                    val activeConvId = turn.conversationId

                    // Cache user message in Room
                    val userEntity = AiMessageEntity(
                        "user_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                        activeConvId,
                        "User",
                        message,
                        null,
                        false,
                        Instant.now().toString()
                    )
                    cacheWrite { dao.insertMessage(userEntity) }

                    // Cache assistant message in Room
                    val assistantEntity = AiMessageEntity(
                        turn.messageId,
                        activeConvId,
                        "Assistant",
                        turn.reply,
                        turn.recommendedPackage?.let { json.encodeToString(it) },
                        turn.offersCited?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                        turn.degraded,
                        Instant.now().toString()
                    )
                    cacheWrite { dao.insertMessage(assistantEntity) }

                    SensiResult.Success(turn.toDomain())
                }
                response.code() == HTTP_TOO_MANY_REQUESTS -> {
                    val retryAfter = response.headers()["Retry-After"]?.toIntOrNull() ?: 60
                    SensiResult.RateLimited(retryAfter)
                }
                response.code() == HTTP_FORBIDDEN -> SensiResult.GuestBlocked
                response.code() == HTTP_NOT_FOUND -> SensiResult.ConversationGone
                response.code() == HTTP_BAD_REQUEST -> {
                    val problem = parseProblem(response.errorBody()?.string())
                    SensiResult.Rejected(
                        code = problem?.title ?: "Error de validación",
                        message = problem?.detail ?: "No pudimos procesar tu mensaje."
                    )
                }
                else -> SensiResult.Unavailable
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        SensiResult.Unavailable
    } catch (e: Exception) {
        SensiResult.Unavailable
    }

    override suspend fun getConversation(
        conversationId: String,
        page: Int,
        pageSize: Int
    ): Result<List<AiChatMessage>> = try {
        val response = api.getConversation(conversationId, page, pageSize)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            // Invert messages order (backend returns newest first; UI wants chronological)
            val chronologicalMessages = body.messages.reversed()
            val entities = chronologicalMessages.map { msg ->
                AiMessageEntity(
                    msg.messageId,
                    conversationId,
                    msg.role,
                    msg.content,
                    msg.recommendedPackage?.let { json.encodeToString(it) },
                    msg.offersCited?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                    msg.degraded,
                    msg.createdAtUtc
                )
            }
            cacheWrite { dao.insertMessages(entities) }
            Result.Success(chronologicalMessages.map { it.toDomain(conversationId) })
        } else {
            // Read from local cache if network fails
            val cached = getCachedMessages(conversationId)
            if (cached.isNotEmpty()) {
                Result.Success(cached)
            } else {
                Result.Failure(AppError.Network("No fue posible cargar el historial de conversación."))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        val cached = getCachedMessages(conversationId)
        if (cached.isNotEmpty()) {
            Result.Success(cached)
        } else {
            Result.Failure(AppError.Network("Error de conexión al consultar conversación."))
        }
    } catch (e: Exception) {
        val cached = getCachedMessages(conversationId)
        if (cached.isNotEmpty()) {
            Result.Success(cached)
        } else {
            Result.Failure(AppError.Generic(e.message ?: "Error inesperado."))
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = try {
        val response = api.deleteConversation(conversationId)
        cacheWrite { dao.deleteMessagesByConversationId(conversationId) }
        if (response.isSuccessful || response.code() == HTTP_NOT_FOUND || response.code() == HTTP_NO_CONTENT) {
            Result.Success(Unit)
        } else {
            Result.Failure(AppError.Network())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        cacheWrite { dao.deleteMessagesByConversationId(conversationId) }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.Generic(e.message ?: "Error al eliminar conversación."))
    }

    override fun observeCachedMessages(conversationId: String): Flow<List<AiChatMessage>> {
        return dao.observeMessagesByConversationId(conversationId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { emit(emptyList()) }
    }

    /**
     * La caché Room es accesoria: leerla o escribirla nunca debe tumbar la app ni el turno de chat. Cualquier
     * fallo de base local (clave perdida, archivo corrupto) se trata como caché vacía. Ver
     * docs/errores-conocidos.md (E-01 y E-02).
     */
    private suspend fun getCachedMessages(conversationId: String): List<AiChatMessage> = withContext(Dispatchers.IO) {
        try {
            dao.getMessagesByConversationId(conversationId).map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun cacheWrite(block: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Caché inaccesible: se ignora, el mensaje ya se entregó al usuario.
            }
        }
    }

    private fun AiMessageEntity.toDomainModel(): AiChatMessage {
        val pkg = packageJson?.let {
            try {
                json.decodeFromString<RecommendedPackageDto>(it).toDomain()
            } catch (e: Exception) {
                null
            }
        }
        val offers = offersJson?.let {
            try {
                json.decodeFromString<List<CitedOfferDto>>(it).map { dto -> dto.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        return AiChatMessage(
            id = messageId,
            conversationId = conversationId,
            role = role,
            content = content,
            recommendedPackage = pkg,
            offersCited = offers,
            degraded = degraded,
            createdAtUtc = createdAtUtc
        )
    }

    private fun parseProblem(errorJson: String?): ProblemDetailsDto? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ProblemDetailsDto>(errorJson)
        } catch (e: Exception) {
            null
        }
    }
}
