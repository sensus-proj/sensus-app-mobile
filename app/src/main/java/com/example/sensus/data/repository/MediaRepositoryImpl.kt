package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.api.ProfileApiService
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.ImageModerationRepository
import com.example.sensus.domain.repository.MediaRepository
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404
private const val HTTP_PAYLOAD_TOO_LARGE = 413
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_SERVER_ERROR = 500
private const val MEDIA_UPLOAD_MAX_RETRIES = 1
private const val UPLOAD_FILE_NAME = "sensus_upload.jpg"
private const val OFFER_MODERATION_CONTEXT = "Fotografía de producto de oferta comercial"

/**
 * Carga multipart a `POST /api/v1/media/upload` (FT-15, Spec 022).
 * Fail-closed: el servidor modera los bytes antes de almacenarlos en Cloudinary; cualquier rechazo,
 * cuarentena o error de red falla cerrado sin subir directamente a Cloudinary.
 */
class MediaRepositoryImpl(
    private val mediaApi: MediaApiService,
    private val moderationService: ImageModerationRepository? = null,
    @Suppress("UNUSED_PARAMETER") profileApi: ProfileApiService? = null,
    @Suppress("UNUSED_PARAMETER") cloudinaryUploader: CloudinarySignedUploader = CloudinarySignedUploader()
) : MediaRepository {

    override suspend fun uploadAndModerate(bytes: ByteArray, mimeType: String): Result<UploadedMedia> {
        val preValidationError = validateBytesAndModerate(bytes)
        if (preValidationError != null) {
            return Result.Failure(preValidationError)
        }

        return safeNetworkCall("Error al subir la imagen.") {
            val part = MultipartBody.Part.createFormData("file", UPLOAD_FILE_NAME, bytes.toRequestBody(mimeType.toMediaType()))
            val response = withTransportRetry(MEDIA_UPLOAD_MAX_RETRIES) { mediaApi.uploadMedia(part) }
            when {
                response.isSuccessful -> mapApprovedUpload(response.body())
                else -> mapUploadError(response)
            }
        }
    }

    /** Retry only transport failures; HTTP responses retain the upstream handling. */
    private suspend fun <T> withTransportRetry(retriesLeft: Int, call: suspend () -> T): T = try {
        call()
    } catch (e: IOException) {
        if (retriesLeft <= 0) throw e
        withTransportRetry(retriesLeft - 1, call)
    }

    private suspend fun validateBytesAndModerate(bytes: ByteArray): AppError? = when {
        bytes.isEmpty() -> AppError.Validation("La imagen no puede estar vacía.")
        bytes.size > ImageUploadPolicy.MAX_UPLOAD_BYTES -> AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE)
        moderationService == null -> null
        else -> when (val modResult = moderationService.moderateImage(bytes, OFFER_MODERATION_CONTEXT)) {
            is ModerationResult.Rejected ->
                AppError.ContentRejected(modResult.reason.ifBlank { ImageModerationService.SECURITY_POLICY_MESSAGE })
            is ModerationResult.Quarantine ->
                AppError.ContentRejected("La imagen está en revisión preventiva: ${modResult.reason}")
            is ModerationResult.Approved -> null
        }
    }

    private fun mapApprovedUpload(body: com.example.sensus.data.remote.dto.UploadMediaResponse?): Result<UploadedMedia> {
        val resolvedUrl = body?.resolvedUrl.orEmpty()
        return when {
            body == null -> Result.Failure(AppError.ContentRejected(ImageModerationService.SECURITY_POLICY_MESSAGE))
            !body.isApproved -> Result.Failure(AppError.ContentRejected(body.resolvedMessage ?: ImageModerationService.SECURITY_POLICY_MESSAGE))
            body.mediaId.isBlank() || resolvedUrl.isBlank() -> Result.Failure(AppError.ContentRejected(ImageModerationService.SECURITY_POLICY_MESSAGE))
            else -> Result.Success(UploadedMedia(body.mediaId, resolvedUrl))
        }
    }

    private fun <T> mapUploadError(response: Response<T>): Result<Nothing> {
        val message = parseBackendErrorMessage(response.errorBody()?.string())
        val error = when (response.code()) {
            HTTP_PAYLOAD_TOO_LARGE -> AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE)
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE -> AppError.ContentRejected(message ?: ImageModerationService.SECURITY_POLICY_MESSAGE)
            else -> mapCommonHttpStatus(response.code(), message, default401 = "Sesión requerida.", default404 = "Servicio de imágenes no disponible.")
        }
        return Result.Failure(error)
    }
}
