package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class SensiChatViewState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false
) : ViewState

sealed interface SensiChatViewIntent : ViewIntent {
    data class SendMessage(val text: String) : SensiChatViewIntent
}

sealed interface SensiChatViewEffect : ViewEffect
