package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.MediaRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_PAYLOAD_TOO_LARGE = 413
private const val HTTP_UNPROCESSABLE = 422
private const val UPLOAD_FILE_NAME = "sensus_upload.jpg"

/**
 * Carga multipart a `POST /api/v1/media/upload` (FT-15, RF-15-03/04).
 * Fail-closed: cualquier respuesta que no sea un 2xx con `approved = true` y `mediaId` valido se rechaza.
 */
class MediaRepositoryImpl(
    private val mediaApi: MediaApiService
) : MediaRepository {

    override suspend fun uploadAndModerate(bytes: ByteArray, mimeType: String): Result<UploadedMedia> {
        if (bytes.isEmpty()) return Result.Failure(AppError.Validation("La imagen no puede estar vacía."))
        if (bytes.size > ImageUploadPolicy.MAX_UPLOAD_BYTES) return Result.Failure(AppError.Validation(ImageUploadPolicy.TOO_LARGE_MESSAGE))

        return safeNetworkCall("Error al subir la imagen.") {
            val part = MultipartBody.Part.createFormData("file", UPLOAD_FILE_NAME, bytes.toRequestBody(mimeType.toMediaType()))
            val response = mediaApi.uploadMedia(part)
            if (response.isSuccessful) mapApprovedUpload(response.body()) else mapUploadError(response)
        }
    }

    private fun mapApprovedUpload(body: com.example.sensus.data.remote.dto.UploadMediaResponse?): Result<UploadedMedia> = when {
        body == null -> Result.Failure(AppError.ContentRejected(ImageModerationService.SECURITY_POLICY_MESSAGE))
        !body.approved -> Result.Failure(AppError.ContentRejected(body.message ?: ImageModerationService.SECURITY_POLICY_MESSAGE))
        body.mediaId.isBlank() || body.url.isBlank() -> Result.Failure(AppError.ContentRejected(ImageModerationService.SECURITY_POLICY_MESSAGE))
        else -> Result.Success(UploadedMedia(body.mediaId, body.url))
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
