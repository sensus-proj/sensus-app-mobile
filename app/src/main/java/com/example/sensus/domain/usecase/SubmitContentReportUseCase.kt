package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ContentReportInput
import com.example.sensus.domain.model.ReportPolicy
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.repository.ReportsRepository
import com.example.sensus.domain.repository.SessionRepository

class SubmitContentReportUseCase(
    private val repository: ReportsRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(input: ContentReportInput): Result<SubmittedReport> {
        val cleanTargetId = input.targetId.trim()
        val cleanDetails = input.details?.trim()?.ifEmpty { null }
        val error = validate(cleanTargetId, cleanDetails, input.details)
        return if (error != null) {
            Result.Failure(error)
        } else {
            repository.submitReport(input.copy(targetId = cleanTargetId, details = cleanDetails))
        }
    }

    private fun validate(targetId: String, cleanDetails: String?, rawDetails: String?): AppError? {
        val current = session.sessionState.value
        return when {
            current.isGuest || !current.isSessionActive -> AppError.Unauthorized("Inicia sesión para reportar contenido.")
            ReportPolicy.isProhibitedLegalContent(rawDetails) -> AppError.ContentRejected(ReportPolicy.LEGAL_EXCLUSION_MESSAGE)
            targetId.isEmpty() -> AppError.Validation("El contenido a reportar no es válido.")
            cleanDetails != null && cleanDetails.length > ReportPolicy.MAX_DETAILS_LENGTH ->
                AppError.Validation("Los detalles adicionales no deben exceder ${ReportPolicy.MAX_DETAILS_LENGTH} caracteres.")
            else -> null
        }
    }
}
