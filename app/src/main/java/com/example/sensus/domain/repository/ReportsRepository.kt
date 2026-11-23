package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.SubmittedReport

interface ReportsRepository {
    suspend fun submitReport(input: ContentReportInput): Result<SubmittedReport>
}
