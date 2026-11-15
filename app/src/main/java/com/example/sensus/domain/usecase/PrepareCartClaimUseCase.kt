package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart

/** The published claim contract reserves one unit of one offer per confirmation. */
class PrepareCartClaimUseCase {
    operator fun invoke(cart: StoreCart): Result<ProductOffer> {
        val item = cart.items.singleOrNull()
        return if (item != null && item.quantity == 1) {
            Result.Success(item.offer)
        } else {
            Result.Failure(AppError.Validation("Selecciona una oferta y una unidad para continuar."))
        }
    }
}
