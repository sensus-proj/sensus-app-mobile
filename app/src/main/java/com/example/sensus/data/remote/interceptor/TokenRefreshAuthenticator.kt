package com.example.sensus.data.remote.interceptor

import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.dto.AuthResponse
import com.example.sensus.data.remote.dto.RefreshTokenRequest
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

private const val REFRESH_TIMEOUT_SECONDS = 15L
private const val MAX_AUTH_ATTEMPTS = 2
private const val REFRESH_PATH = "/api/v1/auth/refresh"
private val SKIP_REFRESH_SUFFIXES = listOf("/auth/login", "/auth/register", "/auth/refresh", "/auth/guest")

/**
 * Authenticator sincronizado de OkHttp (G-06) para rotar automáticamente un JWT de corta vigencia
 * usando el `refreshToken` guardado en [SecurePreferences] ante un HTTP 401.
 *
 * - Si otro hilo ya rotó el JWT, reintenta con el vigente sin volver a llamar a `/auth/refresh`.
 * - Si el servidor rechaza el refresh token, limpia la sesión y notifica `onUnauthorizedCallback`.
 * - Un fallo de red no cierra la sesión: devuelve `null` y el 401 llega al llamador.
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
        if (shouldSkipRefresh(path) || responseCount(response) >= MAX_AUTH_ATTEMPTS) {
            return null
        }
        return synchronized(lock) { resolveRetry(response.request) }
    }

    private fun resolveRetry(failedRequest: Request): Request? {
        val latestToken = securePreferences.getAuthToken()
        val alreadyRotated = !latestToken.isNullOrBlank() &&
            "Bearer $latestToken" != failedRequest.header("Authorization")
        val token = if (alreadyRotated) latestToken else refreshAccessToken(failedRequest.url)
        return token?.let { withBearer(failedRequest, it) }
    }

    private fun refreshAccessToken(baseUrl: HttpUrl): String? {
        val refreshToken = securePreferences.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        val result = runCatching { executeRefresh(baseUrl, refreshToken) }
        val parsed = result.getOrNull()
        if (result.isSuccess && parsed == null) {
            clearSessionOnFailure()
        }
        parsed?.let {
            securePreferences.saveAuthToken(it.token)
            securePreferences.refreshToken = it.refreshToken
        }
        return parsed?.token
    }

    /** Devuelve la sesión renovada, o `null` si el servidor rechazó el refresh token. Lanza ante fallo de red. */
    private fun executeRefresh(baseUrl: HttpUrl, refreshToken: String): AuthResponse? {
        val refreshUrl = baseUrl.newBuilder().encodedPath(REFRESH_PATH).query(null).build()
        val payload = json.encodeToString(RefreshTokenRequest(refreshToken))
        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .header("Accept", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
            if (refreshResponse.isSuccessful) parseAuthResponse(refreshResponse.body?.string()) else null
        }
    }

    private fun parseAuthResponse(body: String?): AuthResponse? = body?.takeIf { it.isNotBlank() }
        ?.let { json.decodeFromString<AuthResponse>(it) }
        ?.takeIf { it.token.isNotBlank() }

    private fun withBearer(request: Request, token: String): Request = request.newBuilder()
        .header("Authorization", "Bearer $token")
        .build()

    private fun clearSessionOnFailure() {
        securePreferences.clearAll()
        onUnauthorizedCallback?.invoke()
    }

    private fun shouldSkipRefresh(path: String): Boolean = SKIP_REFRESH_SUFFIXES.any { path.endsWith(it) }

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
