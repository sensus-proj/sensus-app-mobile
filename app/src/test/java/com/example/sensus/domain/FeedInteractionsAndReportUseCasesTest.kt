package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.ReportPolicy
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ReportTargetType
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.repository.ReportsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedInteractionsAndReportUseCasesTest {
    private val reportsRepo = mockk<ReportsRepository>()
    private val sessionRepo = mockk<SessionRepository>()
    private val activeSession = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
    private val guestSession = MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))

    @Before
    fun setup() {
        every { sessionRepo.sessionState } returns activeSession
    }

    @Test
    fun submitReport_blocksGuestLengthAndProhibitedCsamBeforeNetwork() = runTest {
        val submit = SubmitContentReportUseCase(reportsRepo, sessionRepo)

        every { sessionRepo.sessionState } returns guestSession
        val guestRes = submit(ContentReportInput(ReportTargetType.OFFER, "o1", ReportReason.FRAUD))
        assertTrue((guestRes as Result.Failure).error is AppError.Unauthorized)

        every { sessionRepo.sessionState } returns activeSession
        val legalRes = submit(
            ContentReportInput(ReportTargetType.POST, "p1", ReportReason.OTHER, "Reporto CSAM explotación infantil")
        )
        assertTrue((legalRes as Result.Failure).error is AppError.ContentRejected)
        assertTrue(legalRes.error.message.contains("autoridades"))

        val longRes = submit(
            ContentReportInput(ReportTargetType.OFFER, "o1", ReportReason.SPAM, "a".repeat(ReportPolicy.MAX_DETAILS_LENGTH + 1))
        )
        assertTrue((longRes as Result.Failure).error is AppError.Validation)

        val expected = SubmittedReport("r1", "offer", "o1", "fraude", false, "2026-11-21T12:00:00Z", "Recibido")
        coEvery { reportsRepo.submitReport(any()) } returns Result.Success(expected)

        val ok = submit(ContentReportInput(ReportTargetType.OFFER, "o1", ReportReason.FRAUD, "Precio alterado")) as Result.Success
        assertEquals("r1", ok.data.id)
        coVerify(exactly = 1) { reportsRepo.submitReport(match { it.targetId == "o1" && it.details == "Precio alterado" }) }
    }
}
