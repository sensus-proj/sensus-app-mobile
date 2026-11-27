package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository

class CreateClaimUseCase(private val repository: ClaimsRepository) {
    suspend operator fun invoke(offerId: String): Result<OfferClaim> {
        if (offerId.isBlank()) return Result.Failure(AppError.Validation())
        return repository.createClaim(offerId.trim())
    }

    suspend operator fun invoke(items: List<CartItem>): Result<OfferClaim> {
        if (items.isEmpty() || items.any { it.offer.id.isBlank() || it.quantity < 1 }) {
            return Result.Failure(AppError.Validation("Selecciona al menos un producto y cantidad válida."))
        }
        val single = items.singleOrNull()
        return if (single != null && single.quantity == 1) {
            repository.createClaim(single.offer.id.trim())
        } else {
            repository.createCartClaim(items)
        }
    }
}
