package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.FeedComment
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.PagedFeedComments
import com.example.sensus.domain.model.ReportPolicy
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ReportTargetType
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.model.TogglePostLikeResult
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.ReportsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.AddFeedCommentUseCase
import com.example.sensus.domain.usecase.DeleteFeedPostUseCase
import com.example.sensus.domain.usecase.GetFeedCommentsUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.TogglePostLikeUseCase
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
    private val feedRepo = mockk<FeedRepository>()
    private val reportsRepo = mockk<ReportsRepository>()
    private val sessionRepo = mockk<SessionRepository>()
    private val activeSession = MutableStateFlow(SessionState(isSessionActive = true, isGuest = false))
    private val guestSession = MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))

    @Before
    fun setup() {
        every { sessionRepo.sessionState } returns activeSession
    }

    @Test
    fun toggleLikeAndDeletePost_blockGuestAndInvokeRepoForRegistered() = runTest {
        val toggle = TogglePostLikeUseCase(feedRepo, sessionRepo)
        val deletePost = DeleteFeedPostUseCase(feedRepo, sessionRepo)

        every { sessionRepo.sessionState } returns guestSession
        assertTrue((toggle("post-1") as Result.Failure).error is AppError.Unauthorized)
        assertTrue((deletePost("post-1") as Result.Failure).error is AppError.Unauthorized)

        every { sessionRepo.sessionState } returns activeSession
        coEvery { feedRepo.toggleLike("post-1") } returns Result.Success(TogglePostLikeResult("post-1", 4, true))
        coEvery { feedRepo.deletePost("post-1") } returns Result.Success(Unit)

        assertEquals(4, (toggle("post-1") as Result.Success).data.likesCount)
        assertTrue(deletePost("post-1") is Result.Success)
    }

    @Test
    fun addAndGetComments_enforceLimitsAndProfanityFilter() = runTest {
        val add = AddFeedCommentUseCase(feedRepo, sessionRepo)
        val get = GetFeedCommentsUseCase(feedRepo)

        assertTrue((add("post-1", "   ") as Result.Failure).error is AppError.Validation)
        assertTrue(
            (add("post-1", "x".repeat(FeedPolicy.MAX_COMMENT_LENGTH + 1)) as Result.Failure).error is AppError.Validation
        )
        assertTrue((add("post-1", "Esto es una mierda") as Result.Failure).error is AppError.ContentRejected)

        val comment = FeedComment("c1", "post-1", "u1", "Sofía", "Buen precio", "2026-11-21T10:00:00Z")
        coEvery { feedRepo.addComment("post-1", "Buen precio") } returns Result.Success(comment)
        coEvery { feedRepo.getComments("post-1", 1, FeedPolicy.MAX_PAGE_SIZE) } returns Result.Success(
            PagedFeedComments(listOf(comment), 1, 1, FeedPolicy.MAX_PAGE_SIZE, false)
        )

        val added = add("post-1", "  Buen precio  ") as Result.Success
        assertEquals("c1", added.data.id)
        val loaded = get("post-1", 0, 500) as Result.Success
        assertEquals(1, loaded.data.items.size)
        coVerify(exactly = 0) { feedRepo.addComment("post-1", match { it.contains("mierda") }) }
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
