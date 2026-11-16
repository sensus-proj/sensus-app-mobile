package com.example.sensus.data.remote.api

import com.example.sensus.data.remote.dto.AiTurnResponseDto
import com.example.sensus.data.remote.dto.ConversationDetailDto
import com.example.sensus.data.remote.dto.ConversationSummaryDto
import com.example.sensus.data.remote.dto.CreateConversationRequest
import com.example.sensus.data.remote.dto.SendAiMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AiChatApiService {
    @POST("api/v1/ai/conversations")
    suspend fun createOrResume(@Body body: CreateConversationRequest): Response<ConversationSummaryDto>

    @POST("api/v1/ai/messages")
    suspend fun sendMessage(@Body body: SendAiMessageRequest): Response<AiTurnResponseDto>

    @GET("api/v1/ai/conversations/{id}")
    suspend fun getConversation(
        @Path("id") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ConversationDetailDto>

    @DELETE("api/v1/ai/conversations/{id}")
    suspend fun deleteConversation(@Path("id") conversationId: String): Response<Unit>
}
