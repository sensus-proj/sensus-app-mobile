package com.example.sensus.data.repository

import com.example.sensus.data.SensusAuthManager
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionRepositoryImpl(
    private val securePreferences: SecurePreferences,
    private val userSessionDao: UserSessionDao? = null,
    private val pendingSyncDao: PendingSyncDao? = null,
    private val offerDao: OfferDao? = null
) : SessionRepository {

    private val _sessionState = MutableStateFlow(buildCurrentState())
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private fun buildCurrentState(): SessionState {
        return SessionState(
            isSessionActive = SensusAuthManager.isSessionActive,
            isGuest = SensusAuthManager.isGuestUser,
            hasAcceptedTerms = SensusAuthManager.hasAcceptedTerms,
            hasLocationPermission = SensusAuthManager.hasLocationPermission,
            hasCompletedSplash = SensusAuthManager.hasCompletedSplash,
            activeEmail = SensusAuthManager.currentUser?.email,
            activeToken = securePreferences.getAuthToken()
        )
    }

    private fun syncState() {
        _sessionState.update { buildCurrentState() }
    }

    override suspend fun saveGuestSession(token: String, maxRadiusMeters: Int) {
        securePreferences.saveGuestSession(token, maxRadiusMeters)
        syncState()
    }

    /**
     * FT-08 (T08.6): Purga completa de preferencias cifradas, tablas Room+SQLCipher y estado en memoria.
     */
    override suspend fun clearSession() {
        securePreferences.clearAll()
        try {
            userSessionDao?.clearSession()
            pendingSyncDao?.clearAll()
            offerDao?.clearOffers()
        } catch (_: Exception) {
            // Degradación segura si los DAOs no están disponibles en el hilo actual
        }
        SensusAuthManager.logout()
        syncState()
    }

    override suspend fun setTermsAccepted(accepted: Boolean) {
        SensusAuthManager.setTermsAccepted(accepted)
        syncState()
    }

    override suspend fun setLocationPermission(granted: Boolean) {
        SensusAuthManager.setLocationPermissionGranted(granted)
        syncState()
    }

    override suspend fun setSplashCompleted(completed: Boolean) {
        SensusAuthManager.hasCompletedSplash = completed
        syncState()
    }
}
