package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.OfferDetailsRepository

class RegisterOfferViewUseCase(private val repository: OfferDetailsRepository) {
    suspend operator fun invoke(offerId: String): Result<Unit> = if (offerId.isBlank()) {
        Result.Failure(AppError.Validation("Identificador de oferta requerido."))
    } else {
        repository.registerView(offerId)
    }
}
