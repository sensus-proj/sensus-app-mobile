package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.repository.ClaimsRepository

class GetMyClaimsUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(): Result<List<ClaimSummary>> = when (val result = repository.getMyClaims()) {
        is Result.Success -> Result.Success(result.data.sortedWith(compareBy<ClaimSummary> { it.status.ordinal }.thenByDescending { it.expiresAtEpochMs }))
        is Result.Failure -> result
    }
}

class CancelClaimUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(claimId: String): Result<ClaimSummary> {
        if (claimId.isBlank()) return Result.Failure(AppError.Validation())
        return repository.cancelClaim(claimId.trim())
    }
}
