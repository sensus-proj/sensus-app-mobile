package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.ReportsApiService
import com.example.sensus.data.remote.dto.ContentReportResponseDto
import com.example.sensus.data.remote.dto.CreateContentReportRequest
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.repository.ReportsRepository

private const val HTTP_CREATED = 201
private const val HTTP_CONFLICT = 409

class ReportsRepositoryImpl(
    private val api: ReportsApiService
) : ReportsRepository {
    override suspend fun submitReport(input: ContentReportInput): Result<SubmittedReport> = safeNetworkCall("No se pudo enviar el reporte.") {
        val request = CreateContentReportRequest(
            targetType = input.targetType.apiCode,
            targetId = input.targetId,
            reason = input.reason.apiCode,
            details = input.details
        )
        val response = api.submitReport(request)
        val body = response.body()
        if ((response.code() == HTTP_CREATED || response.isSuccessful) && body != null) {
            Result.Success(body.toSubmittedReport())
        } else {
            val message = parseBackendErrorMessage(response.errorBody()?.string())
            val error = if (response.code() == HTTP_CONFLICT) {
                AppError.Conflict(message ?: "Ya reportaste este contenido recientemente.")
            } else {
                mapCommonHttpStatus(
                    response.code(),
                    message,
                    "Inicia sesión para reportar contenido.",
                    "El contenido reportado ya no está disponible."
                )
            }
            Result.Failure(error)
        }
    }
}

private fun ContentReportResponseDto.toSubmittedReport(): SubmittedReport = SubmittedReport(
    id = id,
    targetType = targetType,
    targetId = targetId,
    reason = reason,
    targetMarkedForReview = targetMarkedForReview,
    createdAtUtc = createdAtUtc,
    message = message.ifBlank { "Reporte enviado." }
)
