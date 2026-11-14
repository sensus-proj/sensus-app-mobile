package com.example.sensus.data.remote.interceptor

import com.example.sensus.data.local.SecurePreferences
import okhttp3.Interceptor
import okhttp3.Response

private const val HTTP_UNAUTHORIZED = 401

class AuthInterceptor(
    private val securePreferences: SecurePreferences,
    private val onUnauthorizedCallback: (() -> Unit)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = securePreferences.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())
        val path = originalRequest.url.encodedPath

        if (response.code == HTTP_UNAUTHORIZED && !isNonSessionClearingRoute(path)) {
            securePreferences.clearAll()
            onUnauthorizedCallback?.invoke()
        }

        return response
    }

    private fun isNonSessionClearingRoute(path: String): Boolean = nonClearingSuffixes.any { path.endsWith(it) } || path.contains("/feed")

    companion object {
        private val nonClearingSuffixes = listOf(
            "/auth/login",
            "/auth/register",
            "/auth/guest",
            "/offers/nearby",
            "/comercios/nearby",
            "/merchants/nearby",
            "/merchants/my-offers"
        )
    }
}
