package com.example.sensus.data.moderation

import android.util.Base64
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.dto.ModerateImageRequest
import com.example.sensus.domain.model.ModerationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageModerationService(
    private val mediaApi: MediaApiService? = null
) {
    companion object {
        const val SECURITY_POLICY_MESSAGE = "No se pudo subir la imagen por políticas de seguridad."
    }

    suspend fun moderateImage(imageBytes: ByteArray): ModerationResult = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext ModerationResult.Rejected("La imagen no puede estar vacía.")
        }

        val api = mediaApi
        if (api == null) {
            // Entorno local sin cliente de red: aprobado por defecto
            return@withContext ModerationResult.Approved
        }

        try {
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val response = api.moderateImage(ModerateImageRequest(imageBase64 = base64))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.approved) {
                    ModerationResult.Approved
                } else {
                    ModerationResult.Rejected(body.message.ifBlank { SECURITY_POLICY_MESSAGE })
                }
            } else {
                ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
            }
        } catch (_: Exception) {
            // Fail-closed ante timeout o error de conexión con el backend
            ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
        }
    }
}
