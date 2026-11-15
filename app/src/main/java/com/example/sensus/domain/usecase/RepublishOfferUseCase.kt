package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.repository.OffersRepository

class RepublishOfferUseCase(
    private val repository: OffersRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(offer: ProductOffer, input: RepublishOfferInput): Result<ProductOffer> {
        if (!supports(offer.status) || offer.id != input.offerId || !input.isValid(nowMillis())) {
            return Result.Failure(AppError.Validation())
        }
        return repository.republishOffer(input)
    }

    companion object {
        fun supports(status: OfferLifecycleStatus): Boolean = status == OfferLifecycleStatus.EXPIRED || status == OfferLifecycleStatus.ARCHIVED
    }
}
