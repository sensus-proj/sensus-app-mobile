package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.AiTurn
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import com.example.sensus.presentation.buyer.SensiChatViewEffect
import com.example.sensus.presentation.buyer.SensiChatViewIntent
import com.example.sensus.presentation.buyer.SensiChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensiChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val loadAiConversationUseCase = mockk<LoadAiConversationUseCase>(relaxed = true)
    private val sendSensiMessageUseCase = mockk<SendSensiMessageUseCase>(relaxed = true)
    private val aiChatRepository = mockk<AiChatRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val locationRepository = mockk<LocationRepository>(relaxed = true)

    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeToken = "token"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { sessionRepository.sessionState } returns sessionState
        coEvery { locationRepository.getCurrentLocation() } returns UserLocation(20.05, -99.34)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SensiChatViewModel {
        return SensiChatViewModel(
            loadAiConversationUseCase = loadAiConversationUseCase,
            sendSensiMessageUseCase = sendSensiMessageUseCase,
            aiChatRepository = aiChatRepository,
            sessionRepository = sessionRepository,
            locationRepository = locationRepository
        )
    }

    @Test
    fun init_authenticatedUser_loadsConversationAndMessages() = runTest {
        val conv = AiConversation("c_123", "Active", "Fast", 1)
        val initialMessages = listOf(
            AiChatMessage(id = "m1", role = "User", content = "Hola")
        )
        coEvery { loadAiConversationUseCase.createOrResume(forceNew = false, screenContext = ScreenContext.HOME) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages("c_123") } returns Result.Success(initialMessages)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("c_123", vm.viewState.value.conversationId)
        assertEquals(1, vm.viewState.value.messages.size)
        assertEquals("Hola", vm.viewState.value.messages.first().content)
        assertFalse(vm.viewState.value.isGuest)
    }

    @Test
    fun init_guestUser_marksIsGuestAndDoesNotCallNetwork() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.isGuest)
        coVerify(exactly = 0) { loadAiConversationUseCase.createOrResume(any(), any()) }
    }

    @Test
    fun sendMessage_optimisticAddAnd200Success_appendsAssistantReply() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())

        val turn = AiTurn(
            conversationId = "c_1",
            messageId = "msg_reply_1",
            reply = "Aquí tienes tu recomendación",
            recommendedPackage = null,
            offersCited = emptyList(),
            degraded = false,
            degradationReason = null,
            correlationId = "corr_1"
        )
        coEvery {
            sendSensiMessageUseCase(
                conversationId = "c_1",
                message = "¿Qué hay para cenar?",
                screenContext = any(),
                location = any()
            )
        } returns SensiResult.Success(turn)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("¿Qué hay para cenar?"))

        // Optimistic state was added immediately
        assertEquals(1, vm.viewState.value.messages.size)
        assertEquals("¿Qué hay para cenar?", vm.viewState.value.messages[0].content)
        assertTrue(vm.viewState.value.isSending)
        assertTrue(vm.viewState.value.isTyping)

        testDispatcher.scheduler.advanceUntilIdle()

        // After success
        assertEquals(2, vm.viewState.value.messages.size)
        assertEquals("msg_reply_1", vm.viewState.value.messages[1].id)
        assertEquals("Aquí tienes tu recomendación", vm.viewState.value.messages[1].content)
        assertFalse(vm.viewState.value.isSending)
        assertFalse(vm.viewState.value.isTyping)
        assertFalse(vm.viewState.value.isDegraded)
    }

    @Test
    fun sendMessage_degradedResponse_marksStateAsDegraded() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())

        val turn = AiTurn(
            conversationId = "c_1",
            messageId = "msg_deg_1",
            reply = "Respuesta rápida básica",
            recommendedPackage = null,
            offersCited = emptyList(),
            degraded = true,
            degradationReason = "timeout",
            correlationId = "corr_deg"
        )
        coEvery { sendSensiMessageUseCase(any(), any(), any(), any()) } returns SensiResult.Success(turn)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Hola"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.isDegraded)
        assertEquals(2, vm.viewState.value.messages.size)
        assertTrue(vm.viewState.value.messages[1].degraded)
    }

    @Test
    fun sendMessage_rejected400_setsErrorMessageAndEmitsToast() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())
        coEvery { sendSensiMessageUseCase(any(), any(), any(), any()) } returns SensiResult.Rejected(
            "Ai.MessageBlockedByModeration",
            "Contenido no permitido por moderación."
        )

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.viewEffect.test {
            vm.dispatchIntent(SensiChatViewIntent.SendMessage("Mensaje no permitido"))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is SensiChatViewEffect.ShowToast)
            assertEquals("Contenido no permitido por moderación.", (effect as SensiChatViewEffect.ShowToast).message)
            assertEquals("Contenido no permitido por moderación.", vm.viewState.value.errorMessage)
            assertFalse(vm.viewState.value.isSending)
            assertFalse(vm.viewState.value.isTyping)
        }
    }

    @Test
    fun sendMessage_rateLimited429_setsCountdownAndDisablesSending() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())
        coEvery { sendSensiMessageUseCase(any(), any(), any(), any()) } returns SensiResult.RateLimited(3)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Muchos mensajes"))
        testDispatcher.scheduler.runCurrent()

        assertEquals(3, vm.viewState.value.retryAfterSeconds)
        assertTrue(vm.viewState.value.errorMessage!!.contains("Límite de mensajes alcanzado"))

        // Advance 1 second
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, vm.viewState.value.retryAfterSeconds)

        // Advance remaining 2 seconds
        testDispatcher.scheduler.advanceTimeBy(2000)
        testDispatcher.scheduler.runCurrent()
        assertNull(vm.viewState.value.retryAfterSeconds)
    }

    @Test
    fun sendMessage_guestBlocked403_opensGuestPromptDialog() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())
        coEvery { sendSensiMessageUseCase(any(), any(), any(), any()) } returns SensiResult.GuestBlocked

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Hola"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.showGuestPrompt)
        assertTrue(vm.viewState.value.isGuest)
    }

    @Test
    fun sendMessage_guestSessionDirectCheck_blocksBeforeSending() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Hola"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.showGuestPrompt)
        coVerify(exactly = 0) { sendSensiMessageUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun sendMessage_conversationGone404_recreatesAndRetriesOnce() = runTest {
        val initialConv = AiConversation("c_old", "Active", "Fast", 0)
        val renewedConv = AiConversation("c_new", "Active", "Fast", 0)

        coEvery { loadAiConversationUseCase.createOrResume(forceNew = false, screenContext = any()) } returns Result.Success(initialConv)
        coEvery { loadAiConversationUseCase.createOrResume(forceNew = true, screenContext = any()) } returns Result.Success(renewedConv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())

        // First call with old conv returns ConversationGone; second call with renewed conv returns Success
        coEvery { sendSensiMessageUseCase(conversationId = "c_old", any(), any(), any()) } returns SensiResult.ConversationGone
        val turn = AiTurn("c_new", "msg_1", "Respuesta reintentada", null, emptyList(), false, null, "corr")
        coEvery { sendSensiMessageUseCase(conversationId = "c_new", any(), any(), any()) } returns SensiResult.Success(turn)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.SendMessage("Reintento"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("c_new", vm.viewState.value.conversationId)
        assertEquals(2, vm.viewState.value.messages.size)
        assertEquals("Respuesta reintentada", vm.viewState.value.messages[1].content)
    }

    @Test
    fun sendMessage_unavailable_showsToast() = runTest {
        val conv = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(any(), any()) } returns Result.Success(conv)
        coEvery { loadAiConversationUseCase.getMessages(any()) } returns Result.Success(emptyList())
        coEvery { sendSensiMessageUseCase(any(), any(), any(), any()) } returns SensiResult.Unavailable

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.viewEffect.test {
            vm.dispatchIntent(SensiChatViewIntent.SendMessage("Hola"))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is SensiChatViewEffect.ShowToast)
            assertEquals("Sensi no está disponible, intenta de nuevo.", (effect as SensiChatViewEffect.ShowToast).message)
            assertEquals("Sensi no está disponible, intenta de nuevo.", vm.viewState.value.errorMessage)
        }
    }

    @Test
    fun clearConversation_resetsMessagesAndCreatesNew() = runTest {
        val conv1 = AiConversation("c_1", "Active", "Fast", 0)
        val conv2 = AiConversation("c_2", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(forceNew = false, screenContext = any()) } returns Result.Success(conv1)
        coEvery { loadAiConversationUseCase.createOrResume(forceNew = true, screenContext = any()) } returns Result.Success(conv2)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.ClearConversation)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("c_2", vm.viewState.value.conversationId)
        assertTrue(vm.viewState.value.messages.isEmpty())
    }

    @Test
    fun deleteConversation_callsRepoAndReinitializes() = runTest {
        val conv1 = AiConversation("c_1", "Active", "Fast", 0)
        val conv2 = AiConversation("c_2", "Active", "Fast", 0)
        coEvery { loadAiConversationUseCase.createOrResume(forceNew = false, screenContext = any()) } returnsMany listOf(
            Result.Success(conv1),
            Result.Success(conv2)
        )
        coEvery { aiChatRepository.deleteConversation("c_1") } returns Result.Success(Unit)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(SensiChatViewIntent.DeleteConversation)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { aiChatRepository.deleteConversation("c_1") }
    }

    @Test
    fun dismissDialogsAndErrors_updatesStateProperly() = runTest {
        val vm = createViewModel()
        vm.dispatchIntent(SensiChatViewIntent.DismissGuestPrompt)
        assertFalse(vm.viewState.value.showGuestPrompt)

        vm.dispatchIntent(SensiChatViewIntent.DismissError)
        assertNull(vm.viewState.value.errorMessage)
    }
}
