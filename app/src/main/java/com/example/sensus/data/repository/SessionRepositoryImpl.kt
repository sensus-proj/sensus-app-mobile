package com.example.sensus.data.repository

import com.example.sensus.data.SensusAuthManager
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.SessionType
import com.example.sensus.data.local.db.dao.FavoriteDao
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.data.remote.api.AuthApiService
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SessionRepositoryImpl(
    private val securePreferences: SecurePreferences,
    private val userSessionDao: () -> UserSessionDao? = { null },
    private val pendingSyncDao: () -> PendingSyncDao? = { null },
    private val offerDao: () -> OfferDao? = { null },
    private val authApiService: AuthApiService? = null,
    private val favoriteDao: () -> FavoriteDao? = { null }
) : SessionRepository {

    private val _sessionState = MutableStateFlow(buildCurrentState())
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private fun buildCurrentState(): SessionState {
        val token = securePreferences.getAuthToken()?.takeIf { it.isNotBlank() }
        val sessionType = securePreferences.getSessionType()
        val hasToken = token != null
        val isLegacyManager = !hasToken && sessionType == SessionType.UNAUTHENTICATED && SensusAuthManager.isSessionActive
        val isGuest = (hasToken && sessionType == SessionType.GUEST) || (isLegacyManager && SensusAuthManager.isGuestUser)
        val isActive = isGuest || (hasToken && sessionType == SessionType.REGISTERED) || isLegacyManager

        val identity = resolveSessionIdentity(isActive, isGuest)
        val (isMerchant, storeName, storeOpen) = resolveMerchantState(isActive, isGuest)
        return SessionState(
            isSessionActive = isActive,
            isGuest = isGuest,
            hasAcceptedTerms = SensusAuthManager.hasAcceptedTerms,
            hasLocationPermission = SensusAuthManager.hasLocationPermission,
            hasCompletedSplash = SensusAuthManager.hasCompletedSplash,
            activeEmail = identity.first,
            activeName = identity.second,
            activePhotoUrl = identity.third,
            activeToken = token,
            isMerchantRegistered = isMerchant,
            merchantStoreName = storeName,
            isStoreOpen = storeOpen
        )
    }

    private fun resolveMerchantState(isActive: Boolean, isGuest: Boolean): Triple<Boolean, String?, Boolean> {
        if (!isActive || isGuest) return Triple(false, null, true)
        val status = securePreferences.storedMerchantStatus
        return if (status.isRegistered) {
            Triple(true, status.storeName?.takeIf { it.isNotBlank() }, status.isStoreOpen)
        } else {
            Triple(false, null, true)
        }
    }

    private fun resolveSessionIdentity(isActive: Boolean, isGuest: Boolean): Triple<String?, String?, String?> {
        if (!isActive) return Triple(null, null, null)
        if (isGuest) return Triple("invitado@sensus.app", "Invitado Sensus", null)
        val email = securePreferences.getUserEmail()?.takeIf { it.isNotBlank() } ?: SensusAuthManager.currentUser?.email
        val name = securePreferences.getUserName()?.takeIf { it.isNotBlank() } ?: SensusAuthManager.currentUser?.name
        val photoUrl = securePreferences.getUserPhoto()
        return Triple(email, name, photoUrl)
    }

    private fun syncState() {
        _sessionState.update { buildCurrentState() }
    }

    fun refreshSync() {
        syncState()
    }

    override suspend fun syncSession() {
        syncState()
    }

    override suspend fun saveGuestSession(token: String, maxRadiusMeters: Int) {
        securePreferences.saveGuestSession(token, maxRadiusMeters)
        SensusAuthManager.loginAsGuest()
        syncState()
    }

    /**
     * FT-08 (T08.6): Purga completa de preferencias cifradas, tablas Room+SQLCipher y estado en memoria.
     */
    override suspend fun clearSession() {
        try {
            authApiService?.logout(null)
        } catch (_: Exception) {
            // Ignorar errores de red para asegurar fail-safe en cliente
        }
        clearLocalSession()
    }

    override suspend fun clearLocalSession() = withContext(NonCancellable) {
        securePreferences.clearAll()
        try {
            withContext(Dispatchers.IO) {
                userSessionDao()?.clearSession()
                pendingSyncDao()?.clearAll()
                offerDao()?.clearOffers()
                favoriteDao()?.clearAll()
            }
        } catch (_: Exception) {
            // Degradación segura si la base local no se puede abrir.
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

    override suspend fun setMerchantStatus(isRegistered: Boolean, storeName: String?) {
        val current = securePreferences.storedMerchantStatus
        securePreferences.storedMerchantStatus = if (isRegistered) {
            current.copy(isRegistered = true, storeName = storeName)
        } else {
            com.example.sensus.data.local.StoredMerchantStatus(isRegistered = false)
        }
        syncState()
    }

    override suspend fun setStoreOpen(isOpen: Boolean) {
        val current = securePreferences.storedMerchantStatus
        if (current.isRegistered) {
            securePreferences.storedMerchantStatus = current.copy(isStoreOpen = isOpen)
        }
        syncState()
    }
}
