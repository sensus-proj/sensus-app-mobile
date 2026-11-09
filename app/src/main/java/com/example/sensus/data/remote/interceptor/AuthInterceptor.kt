package com.example.sensus.data.remote.interceptor

import com.example.sensus.data.local.SecurePreferences
import okhttp3.Interceptor
import okhttp3.Response

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
        val isPublicAuthRoute = path.endsWith("/auth/login") ||
            path.endsWith("/auth/register") ||
            path.endsWith("/auth/guest")

        if (response.code == 401 && !isPublicAuthRoute) {
            securePreferences.clearAll()
            onUnauthorizedCallback?.invoke()
        }

        return response
    }
}
