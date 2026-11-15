package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.repository.OfferDetailsRepository

class GetOfferDetailUseCase(private val repository: OfferDetailsRepository) {
    suspend operator fun invoke(offerId: String): Result<OfferDetails> = if (offerId.isBlank()) {
        Result.Failure(AppError.Validation("Identificador de oferta requerido."))
    } else {
        repository.getDetail(offerId)
    }
}
