package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository

class RestoreClaimUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(offerId: String): Result<OfferClaim> = repository.restoreClaim(offerId)
}
