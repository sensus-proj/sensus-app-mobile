package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.repository.ClaimsRepository
import javax.inject.Inject

class GetMyClaimsUseCase @Inject constructor(private val repository: ClaimsRepository) {
    suspend operator fun invoke(): Result<List<ClaimSummary>> = repository.getMyClaims()
}
