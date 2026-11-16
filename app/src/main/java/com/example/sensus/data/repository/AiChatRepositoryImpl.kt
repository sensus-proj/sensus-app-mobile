package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.AiMessageDao
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.remote.dto.CreateConversationRequest
import com.example.sensus.data.remote.dto.ProblemDetailsDto
import com.example.sensus.data.remote.dto.SendAiMessageRequest
import com.example.sensus.data.remote.dto.toDomain
import com.example.sensus.data.remote.dto.toDto
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiLocation
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.SessionRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import retrofit2.Response

private const val HTTP_NO_CONTENT = 204
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val DEFAULT_RETRY_AFTER_SECONDS = 60

class AiChatRepositoryImpl(
    private val api: AiChatApiService,
    private val dao: AiMessageDao,
    private val sessionRepository: SessionRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }
) : AiChatRepository {

    private val cache = AiMessageCache(dao, json)

    override suspend fun createOrResume(forceNew: Boolean, screenContext: ScreenContext?): Result<AiConversation> {
        if (isGuestSession()) return Result.Failure(AppError.Forbidden("Los invitados no tienen acceso a Sensi."))
        return guarded(
            onNetwork = { Result.Failure(AppError.Network("Error de conexión al iniciar conversación.")) },
            onUnexpected = { Result.Failure(AppError.Generic("Error inesperado.")) }
        ) {
            val response = api.createOrResume(CreateConversationRequest(forceNew = forceNew, screenContext = screenContext?.toDto()))
            val body = response.body()
            if (response.isSuccessful && body != null) Result.Success(body.toDomain()) else conversationFailure(response)
        }
    }

    override suspend fun sendMessage(conversationId: String?, message: String, screenContext: ScreenContext?, location: SensiLocation?): SensiResult {
        if (isGuestSession()) return SensiResult.GuestBlocked
        return guarded(
            onNetwork = { SensiResult.Unavailable },
            onUnexpected = { SensiResult.Unavailable }
        ) {
            val request = SendAiMessageRequest(
                conversationId = conversationId,
                message = message,
                screenContext = screenContext?.toDto(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                radiusKm = location?.radiusKm
            )
            val response = api.sendMessage(request)
            val turn = response.body()
            if (response.isSuccessful && turn != null) {
                cache.saveTurn(message, turn)
                SensiResult.Success(turn.toDomain())
            } else {
                sendFailure(response)
            }
        }
    }

    override suspend fun getConversation(conversationId: String, page: Int, pageSize: Int): Result<List<AiChatMessage>> = guarded(
        onNetwork = { cachedOrFailure(conversationId, AppError.Network("Error de conexión al consultar conversación.")) },
        onUnexpected = { cachedOrFailure(conversationId, AppError.Generic("Error inesperado.")) }
    ) {
        val response = api.getConversation(conversationId, page, pageSize)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            // El backend devuelve primero lo más reciente; la UI lo quiere en orden cronológico.
            val chronological = body.messages.reversed()
            cache.saveMessages(conversationId, chronological)
            Result.Success(chronological.map { it.toDomain(conversationId) })
        } else {
            cachedOrFailure(conversationId, AppError.Network("No fue posible cargar el historial de conversación."))
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = guarded(
        onNetwork = {
            cache.delete(conversationId)
            Result.Success(Unit)
        },
        onUnexpected = { Result.Failure(AppError.Generic("Error al eliminar conversación.")) }
    ) {
        val response = api.deleteConversation(conversationId)
        cache.delete(conversationId)
        val gone = response.isSuccessful || response.code() == HTTP_NOT_FOUND || response.code() == HTTP_NO_CONTENT
        if (gone) Result.Success(Unit) else Result.Failure(AppError.Network())
    }

    override fun observeCachedMessages(conversationId: String): Flow<List<AiChatMessage>> = cache.observe(conversationId)

    private fun isGuestSession(): Boolean {
        val session = sessionRepository.sessionState.value
        return session.isGuest || !session.isSessionActive
    }

    /**
     * Ejecuta [block] traduciendo los fallos esperados: red ([IOException]) y datos inesperados del servidor
     * (JSON inválido o estado ilegal). La cancelación de corrutinas siempre se propaga.
     */
    private suspend fun <T> guarded(onNetwork: suspend () -> T, onUnexpected: suspend () -> T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        onNetwork()
    } catch (_: IllegalArgumentException) {
        onUnexpected()
    } catch (_: IllegalStateException) {
        onUnexpected()
    }

    private fun conversationFailure(response: Response<*>): Result.Failure = when (response.code()) {
        HTTP_UNAUTHORIZED -> Result.Failure(AppError.Unauthorized())
        HTTP_FORBIDDEN -> Result.Failure(AppError.Forbidden())
        HTTP_NOT_FOUND -> Result.Failure(AppError.NotFound())
        HTTP_BAD_REQUEST -> Result.Failure(AppError.Validation(parseProblem(response.errorBody()?.string())?.detail ?: "Solicitud inválida."))
        else -> Result.Failure(AppError.Network())
    }

    private fun sendFailure(response: Response<*>): SensiResult = when (response.code()) {
        HTTP_TOO_MANY_REQUESTS ->
            SensiResult.RateLimited(response.headers()["Retry-After"]?.toIntOrNull() ?: DEFAULT_RETRY_AFTER_SECONDS)
        HTTP_FORBIDDEN -> SensiResult.GuestBlocked
        HTTP_NOT_FOUND -> SensiResult.ConversationGone
        HTTP_BAD_REQUEST -> {
            val problem = parseProblem(response.errorBody()?.string())
            SensiResult.Rejected(
                code = problem?.title ?: "Error de validación",
                message = problem?.detail ?: "No pudimos procesar tu mensaje."
            )
        }
        else -> SensiResult.Unavailable
    }

    private suspend fun cachedOrFailure(conversationId: String, error: AppError): Result<List<AiChatMessage>> {
        val cached = cache.messages(conversationId)
        return if (cached.isNotEmpty()) Result.Success(cached) else Result.Failure(error)
    }

    private fun parseProblem(errorJson: String?): ProblemDetailsDto? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ProblemDetailsDto>(errorJson)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
