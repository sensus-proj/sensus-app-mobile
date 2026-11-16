package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiLocation
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.SessionRepository
import javax.inject.Inject

class SendSensiMessageUseCase @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(conversationId: String?, message: String, screenContext: ScreenContext? = null, location: SensiLocation? = null): SensiResult {
        val trimmed = message.trim()
        val rejection = validate(trimmed)
        val session = sessionRepository.sessionState.value
        return when {
            rejection != null -> rejection
            session.isGuest || !session.isSessionActive -> SensiResult.GuestBlocked
            else -> aiChatRepository.sendMessage(conversationId, trimmed, screenContext, location)
        }
    }

    private fun validate(text: String): SensiResult.Rejected? = when {
        text.isEmpty() -> SensiResult.Rejected("Ai.EmptyMessage", "El mensaje no puede estar vacío.")
        text.length > MAX_MESSAGE_LENGTH ->
            SensiResult.Rejected("Ai.MessageTooLong", "El mensaje no puede superar los 1,500 caracteres.")
        else -> null
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 1500
    }
}
