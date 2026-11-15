package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository

class CreateClaimUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(offerId: String): Result<OfferClaim> {
        if (offerId.isBlank()) return Result.Failure(AppError.Validation())
        return repository.createClaim(offerId.trim())
    }
}
