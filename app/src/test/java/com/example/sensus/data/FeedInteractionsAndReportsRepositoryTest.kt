package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.NetworkClient
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedInteractionsAndReportsRepositoryTest {
    private val server = MockWebServer()
    private lateinit var reportsRepository: ReportsRepositoryImpl

    @Before
    fun setup() {
        server.start()
        val prefs = mockk<SecurePreferences>(relaxed = true)
        every { prefs.getAuthToken() } returns "jwt-test"
        val client = NetworkClient(prefs, server.url("/").toString())
        reportsRepository = ReportsRepositoryImpl(client.reportsApi)
    }

    @After
    fun teardown() {
        server.shutdown()
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
