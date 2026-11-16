package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.ScreenContext
import com.example.sensus.domain.model.SensiResult
import com.example.sensus.domain.repository.AiChatRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.LoadAiConversationUseCase
import com.example.sensus.domain.usecase.SendSensiMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiChatUseCasesTest {

    private val aiChatRepository = mockk<AiChatRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val sessionState = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false, activeToken = "jwt"))

    private lateinit var sendSensiMessageUseCase: SendSensiMessageUseCase
    private lateinit var loadAiConversationUseCase: LoadAiConversationUseCase

    @Before
    fun setup() {
        coEvery { sessionRepository.sessionState } returns sessionState
        sendSensiMessageUseCase = SendSensiMessageUseCase(aiChatRepository, sessionRepository)
        loadAiConversationUseCase = LoadAiConversationUseCase(aiChatRepository, sessionRepository)
    }

    @Test
    fun sendMessage_emptyOrBlank_rejectedWithEmptyMessageCode() = runTest {
        val emptyResult = sendSensiMessageUseCase("conv_1", "")
        assertTrue(emptyResult is SensiResult.Rejected)
        assertEquals("Ai.EmptyMessage", (emptyResult as SensiResult.Rejected).code)

        val blankResult = sendSensiMessageUseCase("conv_1", "   \n\t  ")
        assertTrue(blankResult is SensiResult.Rejected)
        assertEquals("Ai.EmptyMessage", (blankResult as SensiResult.Rejected).code)

        coVerify(exactly = 0) { aiChatRepository.sendMessage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun sendMessage_exceeds1500Chars_rejectedWithMessageTooLongCode() = runTest {
        val longMessage = "a".repeat(1501)
        val result = sendSensiMessageUseCase("conv_1", longMessage)
        assertTrue(result is SensiResult.Rejected)
        assertEquals("Ai.MessageTooLong", (result as SensiResult.Rejected).code)

        coVerify(exactly = 0) { aiChatRepository.sendMessage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun sendMessage_guestSession_returnsGuestBlockedWithoutCallingRepo() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)
        val result = sendSensiMessageUseCase("conv_1", "Hola Sensi")
        assertTrue(result is SensiResult.GuestBlocked)

        coVerify(exactly = 0) { aiChatRepository.sendMessage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun sendMessage_inactiveSession_returnsGuestBlockedWithoutCallingRepo() = runTest {
        sessionState.value = SessionState(isSessionActive = false, isGuest = false)
        val result = sendSensiMessageUseCase("conv_1", "Hola Sensi")
        assertTrue(result is SensiResult.GuestBlocked)

        coVerify(exactly = 0) { aiChatRepository.sendMessage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun sendMessage_validInput_delegatesToRepositoryWithTrimmedText() = runTest {
        val expected = SensiResult.RateLimited(60)
        coEvery {
            aiChatRepository.sendMessage(
                conversationId = "conv_1",
                message = "Ofertas de café",
                screenContext = ScreenContext.MAP,
                latitude = 20.0,
                longitude = -99.0,
                radiusKm = 3.0
            )
        } returns expected

        val actual = sendSensiMessageUseCase(
            conversationId = "conv_1",
            message = "   Ofertas de café   ",
            screenContext = ScreenContext.MAP,
            latitude = 20.0,
            longitude = -99.0,
            radiusKm = 3.0
        )

        assertEquals(expected, actual)
        coVerify(exactly = 1) {
            aiChatRepository.sendMessage("conv_1", "Ofertas de café", ScreenContext.MAP, 20.0, -99.0, 3.0)
        }
    }

    @Test
    fun loadConversation_createOrResume_guest_returnsForbidden() = runTest {
        sessionState.value = SessionState(isSessionActive = true, isGuest = true)
        val result = loadAiConversationUseCase.createOrResume(forceNew = false)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Forbidden)
        coVerify(exactly = 0) { aiChatRepository.createOrResume(any(), any()) }
    }

    @Test
    fun loadConversation_createOrResume_authenticated_delegatesToRepo() = runTest {
        val conversation = AiConversation("c_1", "Active", "Fast", 0)
        coEvery { aiChatRepository.createOrResume(true, ScreenContext.HOME) } returns Result.Success(conversation)

        val result = loadAiConversationUseCase.createOrResume(forceNew = true, screenContext = ScreenContext.HOME)
        assertTrue(result is Result.Success)
        assertEquals(conversation, (result as Result.Success).data)
    }

    @Test
    fun loadConversation_getMessages_delegatesToRepo() = runTest {
        val messages = listOf(
            AiChatMessage(id = "m_1", role = "User", content = "Hola")
        )
        coEvery { aiChatRepository.getConversation("c_1", 1, 20) } returns Result.Success(messages)

        val result = loadAiConversationUseCase.getMessages("c_1", 1, 20)
        assertTrue(result is Result.Success)
        assertEquals(messages, (result as Result.Success).data)
    }
}
