package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository

/**
 * Publica una oferta del comerciante (FT-15, SE-019..SE-021).
 * Orden estricto: validacion pura -> compresion <= 4.5 MB -> subida moderada fail-closed -> creacion.
 * Si la imagen es rechazada o la subida falla, nunca se invoca `POST /api/v1/offers`.
 */
class CreateOfferUseCase(
    private val offersRepository: OffersRepository,
    private val mediaRepository: MediaRepository,
    private val imageCompressor: ImageCompressor,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(input: CreateOfferInput, imageBytes: ByteArray? = null): Result<ProductOffer> {
        val normalized = input.copy(title = input.title.trim(), description = input.description.trim())
        OfferPublishingPolicy.validate(normalized, clock())?.let { return Result.Failure(AppError.Validation(it.message)) }

        val media = when (val upload = uploadMediaIfPresent(mediaRepository, imageCompressor, imageBytes)) {
            is Result.Failure -> return upload
            is Result.Success -> upload.data
        }
        return offersRepository.createOffer(normalized, media)
    }
}

/** Compresion + subida moderada compartida por ofertas (FT-15) y publicaciones del feed (FT-18). */
internal suspend fun uploadMediaIfPresent(mediaRepository: MediaRepository, imageCompressor: ImageCompressor, imageBytes: ByteArray?): Result<UploadedMedia?> {
    return when {
        imageBytes == null -> Result.Success(null)
        imageBytes.isEmpty() -> Result.Failure(AppError.Validation("La imagen no puede estar vacía."))
        else -> compressAndUpload(mediaRepository, imageCompressor, imageBytes)
    }
}

private suspend fun compressAndUpload(repository: MediaRepository, compressor: ImageCompressor, bytes: ByteArray): Result<UploadedMedia> {
    val compressed = compressor.compressForUpload(bytes)
        ?: return Result.Failure(AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE))
    return repository.uploadAndModerate(compressed, ImageUploadPolicy.JPEG_MIME_TYPE)
}
