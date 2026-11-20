package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ToggleOfferLikeResult
import com.example.sensus.domain.repository.OfferDetailsRepository

class ToggleOfferLikeUseCase(private val repository: OfferDetailsRepository) {
    suspend operator fun invoke(offerId: String): Result<ToggleOfferLikeResult> = repository.toggleLike(offerId)
}
