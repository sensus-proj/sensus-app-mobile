package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.dto.PhotoSignatureResponse
import com.example.sensus.domain.model.UploadedMedia
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val CLOUDINARY_TIMEOUT_SECONDS = 30L
private const val DEFAULT_CLOUDINARY_BASE_URL = "https://api.cloudinary.com"

/**
 * Cliente compartido para subida directa firmada a Cloudinary usando parámetros emitidos por el backend.
 */
class CloudinarySignedUploader(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: String = DEFAULT_CLOUDINARY_BASE_URL
) {

    fun upload(imageBytes: ByteArray, mimeType: String, fileName: String, signature: PhotoSignatureResponse): Result<UploadedMedia> {
        val request = buildUploadRequest(imageBytes, mimeType, fileName, signature)
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Result.Failure(AppError.Generic("Error al subir imagen a Cloudinary (${response.code})."))
            } else {
                parseUploadResponse(response.body?.string().orEmpty())
            }
        }
    }

    private fun buildUploadRequest(imageBytes: ByteArray, mimeType: String, fileName: String, signature: PhotoSignatureResponse): Request {
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, imageBytes.toRequestBody(mimeType.toMediaType()))
            .addFormDataPart("api_key", signature.apiKey)
            .addFormDataPart("timestamp", signature.timestamp.toString())
            .addFormDataPart("folder", signature.folder)
            .addFormDataPart("signature", signature.signature)
            .build()

        val root = baseUrl.trimEnd('/')
        return Request.Builder()
            .url("$root/v1_1/${signature.cloudName}/image/upload")
            .post(multipartBody)
            .build()
    }

    private fun parseUploadResponse(body: String): Result<UploadedMedia> {
        val json = Json.parseToJsonElement(body).jsonObject
        val secureUrl = json["secure_url"]?.jsonPrimitive?.content.orEmpty()
        if (secureUrl.isBlank()) {
            return Result.Failure(AppError.Generic("Cloudinary no devolvió una URL segura válida."))
        }
        val publicId = json["public_id"]?.jsonPrimitive?.content.orEmpty()
        return Result.Success(
            UploadedMedia(
                mediaId = publicId.ifBlank { secureUrl },
                url = secureUrl
            )
        )
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
