package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.repository.ClaimsRepository

class RedeemClaimUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(code: String): Result<ClaimRedemption> {
        if (code.isBlank()) return Result.Failure(AppError.Validation())
        return repository.redeemClaim(code.trim())
    }
}
