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
        if (imageBytes == null || imageBytes.isEmpty()) {
            return Result.Failure(AppError.Validation("Debes tomar o seleccionar una foto del producto."))
        }
        val validationMessage = OfferPublishingPolicy.validate(normalized, clock())?.message
            ?: "Selecciona una categoría para la oferta.".takeIf { normalized.category.apiCode == null }
        if (validationMessage != null) return Result.Failure(AppError.Validation(validationMessage))

        val textContext = listOf(normalized.title, normalized.description)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        return when (val upload = compressAndUpload(mediaRepository, imageCompressor, imageBytes, textContext)) {
            is Result.Failure -> upload
            is Result.Success -> createAfterUpload(normalized, upload.data)
        }
    }

    private suspend fun createAfterUpload(input: CreateOfferInput, media: UploadedMedia): Result<ProductOffer> {
        val error = OfferPublishingPolicy.validate(input, clock())
        return if (error != null) Result.Failure(AppError.Validation(error.message)) else offersRepository.createOffer(input, media)
    }
}

/** Compresion + subida moderada compartida por ofertas (FT-15) y publicaciones del feed (FT-18). */
internal suspend fun uploadMediaIfPresent(
    mediaRepository: MediaRepository,
    imageCompressor: ImageCompressor,
    imageBytes: ByteArray?,
    textContext: String? = null
): Result<UploadedMedia?> {
    return when {
        imageBytes == null -> Result.Success(null)
        imageBytes.isEmpty() -> Result.Failure(AppError.Validation("La imagen no puede estar vacía."))
        else -> compressAndUpload(mediaRepository, imageCompressor, imageBytes, textContext)
    }
}

private suspend fun compressAndUpload(
    repository: MediaRepository,
    compressor: ImageCompressor,
    bytes: ByteArray,
    textContext: String? = null
): Result<UploadedMedia> {
    val compressed = compressor.compressForUpload(bytes)
        ?: return Result.Failure(AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE))
    return repository.uploadAndModerate(compressed, ImageUploadPolicy.JPEG_MIME_TYPE, textContext)
}
