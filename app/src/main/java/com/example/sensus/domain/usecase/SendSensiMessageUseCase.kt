package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.SessionRepository
import javax.inject.Inject

class SendSensiMessageUseCase @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        conversationId: String?,
        message: String,
        screenContext: ScreenContext? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Double? = null
    ): SensiResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return SensiResult.Rejected("Ai.EmptyMessage", "El mensaje no puede estar vacío.")
        }
        if (trimmed.length > 1500) {
            return SensiResult.Rejected("Ai.MessageTooLong", "El mensaje no puede superar los 1,500 caracteres.")
        }
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            return SensiResult.GuestBlocked
        }
        return aiChatRepository.sendMessage(
            conversationId = conversationId,
            message = trimmed,
            screenContext = screenContext,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )
    }
}
