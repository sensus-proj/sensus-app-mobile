package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class SensiChatViewState(
    val messages: List<AiChatMessage> = emptyList(),
    val conversationId: String? = null,
    val isTyping: Boolean = false,
    val isSending: Boolean = false,
    val isGuest: Boolean = false,
    val showGuestPrompt: Boolean = false,
    val isDegraded: Boolean = false,
    val retryAfterSeconds: Int? = null,
    val errorMessage: String? = null
) : ViewState

sealed interface SensiChatViewIntent : ViewIntent {
    data class Init(val screenContext: ScreenContext = ScreenContext.HOME) : SensiChatViewIntent
    data class SendMessage(val text: String) : SensiChatViewIntent
    data object ClearConversation : SensiChatViewIntent
    data object DeleteConversation : SensiChatViewIntent
    data object DismissGuestPrompt : SensiChatViewIntent
    data object DismissError : SensiChatViewIntent
    data class UpdateRetryCountdown(val remainingSeconds: Int?) : SensiChatViewIntent
}

sealed interface SensiChatViewEffect : ViewEffect {
    data class ShowToast(val message: String) : SensiChatViewEffect
}
