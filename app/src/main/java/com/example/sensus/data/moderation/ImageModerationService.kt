package com.example.sensus.data.moderation

import android.util.Base64
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.dto.ModerateImageRequest
import com.example.sensus.data.remote.dto.ModerateImageResponse
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.repository.ImageModerationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ImageModerationService(
    private val mediaApi: MediaApiService? = null
) : ImageModerationRepository {
    companion object {
        const val SECURITY_POLICY_MESSAGE = "No se pudo subir la imagen por políticas de seguridad."
    }

    override suspend fun moderateImage(imageBytes: ByteArray, textContext: String?): ModerationResult = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext ModerationResult.Rejected("La imagen no puede estar vacía.")
        }

        val api = mediaApi ?: return@withContext ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)

        try {
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val request = ModerateImageRequest(imageBase64 = base64, textContext = textContext)
            val response = api.moderateImage(request)
            parseModerationResponse(response)
        } catch (_: Exception) {
            ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
        }
    }

    private fun parseModerationResponse(response: Response<ModerateImageResponse>): ModerationResult {
        if (!response.isSuccessful) {
            return ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
        }
        val body = response.body() ?: return ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
        return when (body.decision.lowercase()) {
            "approved" -> ModerationResult.Approved
            "quarantine" -> ModerationResult.Quarantine(
                reason = body.message,
                scores = body.categoryScores ?: emptyMap()
            )
            else -> ModerationResult.Rejected(body.message.ifBlank { SECURITY_POLICY_MESSAGE })
        }
    }
}
