package com.example.sensus.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class SessionState(
    val isSessionActive: Boolean = false,
    val isGuest: Boolean = false,
    val hasAcceptedTerms: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasCompletedSplash: Boolean = false,
    val activeEmail: String? = null,
    val activeName: String? = null,
    val activePhotoUrl: String? = null,
    val activeToken: String? = null,
    val isMerchantRegistered: Boolean = false,
    val merchantStoreName: String? = null,
    val isStoreOpen: Boolean = true
)

interface SessionRepository {
    val sessionState: StateFlow<SessionState>
    suspend fun saveGuestSession(token: String, maxRadiusMeters: Int = 2000)
    suspend fun clearSession()
    suspend fun clearLocalSession() {
        clearSession()
    }
    suspend fun syncSession() {}
    suspend fun setTermsAccepted(accepted: Boolean)
    suspend fun setLocationPermission(granted: Boolean)
    suspend fun setSplashCompleted(completed: Boolean)
    suspend fun setMerchantStatus(isRegistered: Boolean, storeName: String?) {}
    suspend fun setStoreOpen(isOpen: Boolean) {}
}
