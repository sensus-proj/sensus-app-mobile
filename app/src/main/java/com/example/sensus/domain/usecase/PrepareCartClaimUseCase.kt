package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.StoreCart

/** Prepara todos los productos y cantidades de una misma tienda para apartarse en un único QR. */

class PrepareCartClaimUseCase {
    operator fun invoke(cart: StoreCart): Result<List<CartItem>> {
        val stores = cart.items
            .map { it.offer.merchantId?.takeIf { id -> id.isNotBlank() } ?: it.offer.storeName }
            .distinct()
        return when {
            cart.items.isEmpty() -> Result.Failure(AppError.Validation("Agrega al menos un producto para apartar."))
            cart.items.any { it.quantity < 1 || it.offer.id.isBlank() } ->
                Result.Failure(AppError.Validation("Verifica las cantidades del carrito antes de continuar."))
            stores.size > 1 ->
                Result.Failure(AppError.Validation("Solo puedes generar un apartado con productos de la misma tienda."))
            else -> Result.Success(cart.items)
        }
    }
}
