package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SensiChatViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : MviViewModel<SensiChatViewState, SensiChatViewIntent, SensiChatViewEffect>(SensiChatViewState()) {

    init {
        updateState { copy(messages = storeRepository.getSampleChatMessages()) }
    }

    override fun processIntent(intent: SensiChatViewIntent) {
        when (intent) {
            is SensiChatViewIntent.SendMessage -> {
                val newMsg = ChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isFromUser = true,
                    text = intent.text
                )
                updateState { copy(messages = messages + newMsg) }
            }
        }
    }
}
