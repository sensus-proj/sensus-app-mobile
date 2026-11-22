package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.FeedPostDao
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.data.repository.ReportsRepositoryImpl
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.ReportTargetType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedInteractionsAndReportsRepositoryTest {
    private val server = MockWebServer()
    private val dao = mockk<FeedPostDao>(relaxed = true)
    private lateinit var feedRepository: FeedRepositoryImpl
    private lateinit var reportsRepository: ReportsRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val prefs = mockk<SecurePreferences>(relaxed = true)
        every { prefs.getAuthToken() } returns "jwt-test"
        val client = NetworkClient(prefs, server.url("/").toString())
        feedRepository = FeedRepositoryImpl(client.feedApi, dao)
        reportsRepository = ReportsRepositoryImpl(client.reportsApi)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun feedInteractions_toggleLikeGetAddCommentsAndDeletePost() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"postId":"p1","likesCount":5,"isLikedByMe":true}""")
        )
        val like = feedRepository.toggleLike("p1") as Result.Success
        assertEquals(5, like.data.likesCount)
        assertTrue(like.data.isLikedByMe)

        val commentsBody = """
            {"items":[{"id":"c1","postId":"p1","authorUserId":"u1","authorName":"Ana",
            "content":"Genial","createdAtUtc":"2026-11-21T10:00:00Z"}],
            "totalCount":1,"pageNumber":1,"pageSize":20,"hasNextPage":false}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(commentsBody))
        val comments = feedRepository.getComments("p1", 1, 20) as Result.Success
        assertEquals(1, comments.data.items.size)
        assertEquals("Genial", comments.data.items.first().text)

        val createdBody = """
            {"id":"c2","postId":"p1","authorUserId":"u1","authorName":"Ana",
            "content":"Gracias","createdAtUtc":"2026-11-21T10:05:00Z"}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(201).setBody(createdBody))
        val created = feedRepository.addComment("p1", "Gracias") as Result.Success
        assertEquals("c2", created.data.id)

        server.enqueue(MockResponse().setResponseCode(204))
        val deleted = feedRepository.deletePost("p1")
        assertTrue(deleted is Result.Success)
    }

    @Test
    fun reportsRepository_maps201CreatedAnd409DuplicateConflict() = runTest {
        val reportBody = """
            {"id":"r1","targetType":"offer","targetId":"o1","reason":"fraude",
            "targetMarkedForReview":true,"createdAtUtc":"2026-11-21T12:00:00Z",
            "message":"Reporte recibido"}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(201).setBody(reportBody))
        val created = reportsRepository.submitReport(
            ContentReportInput(ReportTargetType.OFFER, "o1", ReportReason.FRAUD, "Precio falso")
        ) as Result.Success
        assertTrue(created.data.targetMarkedForReview)

        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"detail":"Ya reportaste este contenido en las últimas 24 horas."}""")
        )
        val duplicate = reportsRepository.submitReport(
            ContentReportInput(ReportTargetType.OFFER, "o1", ReportReason.FRAUD, "Precio falso")
        ) as Result.Failure
        assertTrue(duplicate.error is AppError.Conflict)
    }
}
