package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiLocation
import com.example.sensus.domain.model.SensiResult
import kotlinx.coroutines.flow.Flow

interface AiChatRepository {
    suspend fun createOrResume(forceNew: Boolean = false, screenContext: ScreenContext? = null): Result<AiConversation>

    suspend fun sendMessage(conversationId: String?, message: String, screenContext: ScreenContext? = null, location: SensiLocation? = null): SensiResult

    suspend fun getConversation(conversationId: String, page: Int = 1, pageSize: Int = 20): Result<List<AiChatMessage>>

    suspend fun deleteConversation(conversationId: String): Result<Unit>

    fun observeCachedMessages(conversationId: String): Flow<List<AiChatMessage>>
}
