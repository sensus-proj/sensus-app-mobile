package com.example.sensus.domain.usecase

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository

/**
 * Obtiene el inventario propio del comerciante desde `GET /api/v1/merchants/my-offers` (FT-16, Spec 037).
 */
class GetMerchantInventoryUseCase(
    private val offersRepository: OffersRepository
) {
    suspend operator fun invoke(statusFilter: OfferLifecycleStatus? = null): Result<List<ProductOffer>> =
        offersRepository.fetchMerchantOffers(status = statusFilter?.backendValue)
}
