package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository

/**
 * Cambia el estado del ciclo de vida de una oferta (`Paused`, `Published`, `Archived`) validando transiciones (FT-16, Spec 036).
 */
class ChangeOfferStatusUseCase(
    private val offersRepository: OffersRepository
) {
    suspend operator fun invoke(offer: ProductOffer, targetStatus: OfferLifecycleStatus): Result<ProductOffer> {
        val validationError = validateTransition(offer.status, targetStatus)
        return if (validationError != null) {
            Result.Failure(AppError.Validation(validationError))
        } else {
            offersRepository.changeOfferStatus(offer.id, targetStatus)
        }
    }

    private fun validateTransition(current: OfferLifecycleStatus, target: OfferLifecycleStatus): String? = when {
        current == OfferLifecycleStatus.ARCHIVED ->
            "Una oferta dada de baja definitivamente no puede cambiar de estado."
        target != OfferLifecycleStatus.PAUSED && target != OfferLifecycleStatus.ACTIVE && target != OfferLifecycleStatus.ARCHIVED ->
            "Estado destino no soportado para gestión manual."
        else -> null
    }
}
