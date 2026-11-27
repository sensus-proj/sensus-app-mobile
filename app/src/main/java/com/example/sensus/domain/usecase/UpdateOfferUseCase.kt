package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository

/**
 * Edita una oferta comercial existente validando precios/stock/vigencia y moderando foto nueva si se adjunta (FT-16, Spec 035).
 */
class UpdateOfferUseCase(
    private val offersRepository: OffersRepository,
    private val mediaRepository: MediaRepository,
    private val imageCompressor: ImageCompressor,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(input: UpdateOfferInput, newImageBytes: ByteArray? = null): Result<ProductOffer> {
        val normalized = input.copy(title = input.title.trim(), description = input.description.trim())
        OfferPublishingPolicy.validateUpdate(normalized, clock())?.let {
            return Result.Failure(AppError.Validation(it.message))
        }

        val textContext = offerModerationContext(normalized.title, normalized.description)
        val resolvedImageUrl = when (val upload = uploadMediaIfPresent(mediaRepository, imageCompressor, newImageBytes, textContext)) {
            is Result.Failure -> return upload
            is Result.Success -> upload.data?.url ?: normalized.imageUrl
        }
        return offersRepository.updateOffer(normalized.copy(imageUrl = resolvedImageUrl))
    }
}
