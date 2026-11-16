package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.SessionRepository
import javax.inject.Inject

class LoadAiConversationUseCase @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun createOrResume(forceNew: Boolean = false, screenContext: ScreenContext? = null): Result<AiConversation> {
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            return Result.Failure(AppError.Forbidden("Los invitados no tienen acceso a Sensi."))
        }
        return aiChatRepository.createOrResume(forceNew, screenContext)
    }

    suspend fun getMessages(conversationId: String, page: Int = 1, pageSize: Int = 20): Result<List<AiChatMessage>> {
        return aiChatRepository.getConversation(conversationId, page, pageSize)
    }
}
