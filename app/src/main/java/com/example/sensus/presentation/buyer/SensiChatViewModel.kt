package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiTurn
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiLocation
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_RADIUS_KM = 5.0
private const val COUNTDOWN_TICK_MS = 1000L
private const val UNAVAILABLE_MESSAGE = "Sensi no está disponible, intenta de nuevo."
private const val CONVERSATION_EXPIRED_MESSAGE = "La conversación expiró. Intenta de nuevo."

@HiltViewModel
class SensiChatViewModel @Inject constructor(
    private val loadAiConversationUseCase: LoadAiConversationUseCase,
    private val sendSensiMessageUseCase: SendSensiMessageUseCase,
    private val aiChatRepository: AiChatRepository,
    private val sessionRepository: SessionRepository,
    private val locationRepository: LocationRepository
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
            sendWithRetry(text, isRetry = false)
        }
    }

    private suspend fun sendWithRetry(text: String, isRetry: Boolean) {
        val result = sendSensiMessageUseCase(
            conversationId = viewState.value.conversationId,
            message = text,
            screenContext = currentScreenContext,
            location = currentLocation()
        )
        when (result) {
            is SensiResult.Success -> showAssistantReply(result.turn)
            is SensiResult.ConversationGone -> handleConversationGone(text, isRetry)
            is SensiResult.RateLimited -> showRateLimited(result.retryAfterSeconds)
            is SensiResult.GuestBlocked -> updateState { stopSending().copy(isGuest = true, showGuestPrompt = true) }
            is SensiResult.Rejected -> showSendError(result.message)
            is SensiResult.Unavailable -> showSendError(UNAVAILABLE_MESSAGE)
        }
    }

    private suspend fun currentLocation(): SensiLocation? {
        val location = try {
            locationRepository.getCurrentLocation()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return location?.let { SensiLocation(it.latitude, it.longitude, SEARCH_RADIUS_KM) }
    }

    private fun showAssistantReply(turn: AiTurn) {
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
            stopSending().copy(
                messages = messages + assistantMsg,
                conversationId = turn.conversationId,
                isDegraded = turn.degraded
            )
        }
    }

    private suspend fun handleConversationGone(text: String, isRetry: Boolean) {
        if (!isRetry) {
            val newConv = loadAiConversationUseCase.createOrResume(forceNew = true, screenContext = currentScreenContext)
            if (newConv is Result.Success) {
                updateState { copy(conversationId = newConv.data.conversationId) }
                sendWithRetry(text, isRetry = true)
                return
            }
        }
        showSendError(CONVERSATION_EXPIRED_MESSAGE)
    }

    private fun showRateLimited(seconds: Int) {
        updateState {
            stopSending().copy(
                retryAfterSeconds = seconds,
                errorMessage = "Límite de mensajes alcanzado. Espera $seconds segundos."
            )
        }
        startRetryCountdown(seconds)
    }

    private fun showSendError(message: String) {
        updateState { stopSending().copy(errorMessage = message) }
        emitEffect(SensiChatViewEffect.ShowToast(message))
    }

    private fun SensiChatViewState.stopSending() = copy(isTyping = false, isSending = false)

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
                delay(COUNTDOWN_TICK_MS)
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
