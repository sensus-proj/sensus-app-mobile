package com.example.sensus.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class SessionState(
    val isSessionActive: Boolean = false,
    val isGuest: Boolean = false,
    val hasAcceptedTerms: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasCompletedSplash: Boolean = false,
    val activeEmail: String? = null,
    val activeToken: String? = null
)

interface SessionRepository {
    val sessionState: StateFlow<SessionState>
    suspend fun saveGuestSession(token: String, maxRadiusMeters: Int = 2000)
    suspend fun clearSession()
    suspend fun setTermsAccepted(accepted: Boolean)
    suspend fun setLocationPermission(granted: Boolean)
    suspend fun setSplashCompleted(completed: Boolean)
}
