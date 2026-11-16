package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SensiChatViewModel @Inject constructor(
    private val loadAiConversationUseCase: LoadAiConversationUseCase,
    private val sendSensiMessageUseCase: SendSensiMessageUseCase,
    private val aiChatRepository: AiChatRepository,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository,
    private val mapPreferencesRepository: MapPreferencesRepository
) : MviViewModel<SensiChatViewState, SensiChatViewIntent, SensiChatViewEffect>(SensiChatViewState()) {

    private var currentScreenContext: ScreenContext = ScreenContext.HOME
    private var countdownJob: Job? = null

    init {
        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            updateState { copy(isGuest = true) }
        } else {
            dispatchIntent(SensiChatViewIntent.Init(ScreenContext.HOME))
        }
    }

    override fun processIntent(intent: SensiChatViewIntent) {
        when (intent) {
            is SensiChatViewIntent.Init -> handleInit(intent.screenContext)
            is SensiChatViewIntent.SendMessage -> handleSendMessage(intent.text)
            is SensiChatViewIntent.ClearConversation -> handleClearConversation()
            is SensiChatViewIntent.DeleteConversation -> handleDeleteConversation()
            is SensiChatViewIntent.DismissGuestPrompt -> updateState { copy(showGuestPrompt = false) }
            is SensiChatViewIntent.DismissError -> updateState { copy(errorMessage = null) }
            is SensiChatViewIntent.UpdateRetryCountdown -> updateState { copy(retryAfterSeconds = intent.remainingSeconds) }
        }
    }

    private fun handleInit(screenContext: ScreenContext) {
        currentScreenContext = screenContext
        viewModelScope.launch {
            val session = sessionRepository.sessionState.value
            if (session.isGuest || !session.isSessionActive) {
                updateState { copy(isGuest = true) }
                return@launch
            }
            updateState { copy(isGuest = false) }
            when (val convResult = loadAiConversationUseCase.createOrResume(forceNew = false, screenContext = screenContext)) {
                is Result.Success -> {
                    val convId = convResult.data.conversationId
                    updateState { copy(conversationId = convId) }
                    val messagesResult = loadAiConversationUseCase.getMessages(convId)
                    if (messagesResult is Result.Success) {
                        updateState { copy(messages = messagesResult.data, errorMessage = null) }
                    }
                }
                is Result.Failure -> {
                    if (session.isGuest) {
                        updateState { copy(isGuest = true) }
                    }
                }
            }
        }
    }

    private fun handleSendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        val session = sessionRepository.sessionState.value
        if (session.isGuest || !session.isSessionActive) {
            updateState { copy(isGuest = true, showGuestPrompt = true) }
            return
        }

        val currentState = viewState.value
        if (currentState.retryAfterSeconds != null && currentState.retryAfterSeconds > 0) {
            emitEffect(SensiChatViewEffect.ShowToast("Espera ${currentState.retryAfterSeconds} segundos antes de enviar otro mensaje."))
            return
        }

        val tempId = "user_opt_${System.currentTimeMillis()}"
        val optimisticMsg = AiChatMessage(
            id = tempId,
            conversationId = currentState.conversationId ?: "",
            role = "User",
            content = text,
            createdAtUtc = Instant.now().toString()
        )

        updateState {
            copy(
                messages = messages + optimisticMsg,
                isTyping = true,
                isSending = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            sendWithRetry(text, optimisticMsg, isRetry = false)
        }
    }

    private suspend fun sendWithRetry(
        text: String,
        optimisticMsg: AiChatMessage,
        isRetry: Boolean
    ) {
        val state = viewState.value
        val location = try {
            locationRepository.getCurrentLocation()
        } catch (e: Exception) {
            null
        }
        val lat = location?.latitude
        val lng = location?.longitude
        val radiusKm = 5.0

        val result = sendSensiMessageUseCase(
            conversationId = state.conversationId,
            message = text,
            screenContext = currentScreenContext,
            latitude = lat,
            longitude = lng,
            radiusKm = radiusKm
        )

        when (result) {
            is SensiResult.Success -> {
                val turn = result.turn
                val assistantMsg = AiChatMessage(
                    id = turn.messageId,
                    conversationId = turn.conversationId,
                    role = "Assistant",
                    content = turn.reply,
                    recommendedPackage = turn.recommendedPackage,
                    offersCited = turn.offersCited,
                    degraded = turn.degraded,
                    createdAtUtc = Instant.now().toString()
                )
                updateState {
                    copy(
                        messages = messages + assistantMsg,
                        conversationId = turn.conversationId,
                        isTyping = false,
                        isSending = false,
                        isDegraded = turn.degraded
                    )
                }
            }
            is SensiResult.ConversationGone -> {
                if (!isRetry) {
                    val newConv = loadAiConversationUseCase.createOrResume(
                        forceNew = true,
                        screenContext = currentScreenContext
                    )
                    if (newConv is Result.Success) {
                        updateState { copy(conversationId = newConv.data.conversationId) }
                        sendWithRetry(text, optimisticMsg, isRetry = true)
                        return
                    }
                }
                updateState {
                    copy(
                        isTyping = false,
                        isSending = false,
                        errorMessage = "La conversación expiró. Intenta de nuevo."
                    )
                }
                emitEffect(SensiChatViewEffect.ShowToast("La conversación expiró. Intenta de nuevo."))
            }
            is SensiResult.RateLimited -> {
                val seconds = result.retryAfterSeconds
                updateState {
                    copy(
                        isTyping = false,
                        isSending = false,
                        retryAfterSeconds = seconds,
                        errorMessage = "Límite de mensajes alcanzado. Espera $seconds segundos."
                    )
                }
                startRetryCountdown(seconds)
            }
            is SensiResult.GuestBlocked -> {
                updateState {
                    copy(
                        isTyping = false,
                        isSending = false,
                        isGuest = true,
                        showGuestPrompt = true
                    )
                }
            }
            is SensiResult.Rejected -> {
                updateState {
                    copy(
                        isTyping = false,
                        isSending = false,
                        errorMessage = result.message
                    )
                }
                emitEffect(SensiChatViewEffect.ShowToast(result.message))
            }
            is SensiResult.Unavailable -> {
                val msg = "Sensi no está disponible, intenta de nuevo."
                updateState {
                    copy(
                        isTyping = false,
                        isSending = false,
                        errorMessage = msg
                    )
                }
                emitEffect(SensiChatViewEffect.ShowToast(msg))
            }
        }
    }

    private fun handleClearConversation() {
        viewModelScope.launch {
            updateState { copy(messages = emptyList(), isTyping = false, isSending = false, isDegraded = false) }
            val conv = loadAiConversationUseCase.createOrResume(forceNew = true, screenContext = currentScreenContext)
            if (conv is Result.Success) {
                updateState { copy(conversationId = conv.data.conversationId) }
            }
        }
    }

    private fun handleDeleteConversation() {
        viewModelScope.launch {
            val convId = viewState.value.conversationId
            if (!convId.isNullOrBlank()) {
                aiChatRepository.deleteConversation(convId)
            }
            updateState { copy(messages = emptyList(), conversationId = null, isTyping = false, isSending = false) }
            handleInit(currentScreenContext)
        }
    }

    private fun startRetryCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                updateState { copy(retryAfterSeconds = remaining) }
                delay(1000L)
                remaining--
            }
            updateState { copy(retryAfterSeconds = null, errorMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
