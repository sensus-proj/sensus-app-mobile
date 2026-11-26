package com.example.sensus.data.remote.interceptor

import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.dto.AuthResponse
import com.example.sensus.data.remote.dto.RefreshTokenRequest
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

private const val REFRESH_TIMEOUT_SECONDS = 15L

/**
 * Authenticator sincronizado de OkHttp (G-06) para rotar automáticamente un JWT de corta vigencia
 * usando el `refreshToken` guardado en [SecurePreferences] ante un HTTP 401.
 */
class TokenRefreshAuthenticator(
    private val securePreferences: SecurePreferences,
    private val onUnauthorizedCallback: (() -> Unit)? = null
) : Authenticator {

    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (shouldSkipRefresh(path) || responseCount(response) >= 2) {
            return null
        }

        val failedAuthHeader = response.request.header("Authorization")

        synchronized(lock) {
            val latestToken = securePreferences.getAuthToken()
            if (!latestToken.isNullOrBlank() && "Bearer $latestToken" != failedAuthHeader) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = securePreferences.getRefreshToken()?.takeIf { it.isNotBlank() }
                ?: return null

            val refreshUrl = response.request.url.newBuilder()
                .encodedPath("/api/v1/auth/refresh")
                .query(null)
                .build()

            val payload = json.encodeToString(RefreshTokenRequest(refreshToken))
            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .header("Accept", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            return try {
                refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                    if (!refreshResponse.isSuccessful) {
                        clearSessionOnFailure()
                        return null
                    }

                    val body = refreshResponse.body?.string()?.takeIf { it.isNotBlank() } ?: run {
                        clearSessionOnFailure()
                        return null
                    }

                    val parsed = json.decodeFromString<AuthResponse>(body)
                    if (parsed.token.isBlank()) {
                        clearSessionOnFailure()
                        return null
                    }

                    securePreferences.saveAuthToken(parsed.token)
                    securePreferences.saveRefreshToken(parsed.refreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${parsed.token}")
                        .build()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun clearSessionOnFailure() {
        securePreferences.clearAll()
        onUnauthorizedCallback?.invoke()
    }

    private fun shouldSkipRefresh(path: String): Boolean =
        path.endsWith("/auth/login") ||
            path.endsWith("/auth/register") ||
            path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/guest")

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
