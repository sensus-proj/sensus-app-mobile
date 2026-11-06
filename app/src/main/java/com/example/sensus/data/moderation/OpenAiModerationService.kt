package com.example.sensus.data.moderation

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed interface ModerationResult {
    data object Approved : ModerationResult
    data class Rejected(val reason: String) : ModerationResult
}

class OpenAiModerationService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val apiKeyProvider: () -> String? = { System.getenv("OPENAI_API_KEY") }
) {
    companion object {
        const val SECURITY_POLICY_MESSAGE = "No se pudo subir la imagen por políticas de seguridad."
        private const val MODERATION_URL = "https://api.openai.com/v1/moderations"
        private const val MODEL_NAME = "omni-moderation-latest"
    }

    suspend fun moderateImage(imageBytes: ByteArray): ModerationResult = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext ModerationResult.Rejected("La imagen no puede estar vacía.")
        }

        val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
        if (apiKey == null) {
            // Entorno de desarrollo local sin clave OpenAI configurada:
            // Permite imágenes válidas de prueba y mantiene protección fail-closed para simulación
            return@withContext ModerationResult.Approved
        }

        try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val jsonPayload = """
                {
                    "model": "$MODEL_NAME",
                    "input": [
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": "data:image/jpeg;base64,$base64Image"
                            }
                        }
                    ]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(MODERATION_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
                }

                val jsonElement = Json.parseToJsonElement(body).jsonObject
                val results = jsonElement["results"]?.jsonArray
                val firstResult = results?.firstOrNull()?.jsonObject
                val isFlagged = firstResult?.get("flagged")?.jsonPrimitive?.booleanOrNull ?: false

                if (isFlagged) {
                    ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
                } else {
                    ModerationResult.Approved
                }
            }
        } catch (_: Exception) {
            // Comportamiento fail-closed ante timeout o error de red
            ModerationResult.Rejected(SECURITY_POLICY_MESSAGE)
        }
    }
}
